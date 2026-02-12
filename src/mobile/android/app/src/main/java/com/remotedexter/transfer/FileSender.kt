package com.remotedexter.transfer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.remotedexter.protocol.FileOffer
import com.remotedexter.protocol.FileAccept
import com.remotedexter.protocol.FileReject
import com.remotedexter.protocol.FileChunk
import com.remotedexter.protocol.CommandRequest
import com.remotedexter.protocol.CommandResponse
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FileSender(
    private val context: Context,
    private val sendCommand: (CommandRequest) -> CommandResponse,
    private val onProgress: (fileId: String, progress: Float, speed: Float) -> Unit = { _, _, _ -> },
    private val onStatus: (fileId: String, status: String, message: String) -> Unit = { _, _, _ -> }
) {
    private val activeTransfers = ConcurrentHashMap<String, FileTransfer>()
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "FileSender"
        private const val MAX_CONCURRENT_TRANSFERS = 2
        private const val CHUNK_SIZE = 64 * 1024 // 64KB chunks
    }

    data class FileTransfer(
        val fileId: String,
        val fileName: String,
        val size: Long,
        var transferred: Long = 0,
        val startTime: Long = System.currentTimeMillis(),
        var lastUpdate: Long = System.currentTimeMillis(),
        var status: String = "sending",
        var error: String? = null,
        var uri: Uri? = null,
        var inputStream: InputStream? = null
    )

    fun sendFile(uri: Uri): String? {
        // Check concurrent transfer limit
        if (activeTransfers.size >= MAX_CONCURRENT_TRANSFERS) {
            onStatus("", "error", "Maximum concurrent transfers reached")
            return null
        }

        try {
            // Get file info
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex("_display_name")
                    val sizeIndex = it.getColumnIndex("_size")

                    val fileName = if (nameIndex >= 0) it.getString(nameIndex) else "unknown_file"
                    val fileSize = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L

                    if (fileSize <= 0) {
                        onStatus("", "error", "Unable to determine file size")
                        return null
                    }

                    // Open input stream
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        onStatus("", "error", "Unable to open file for reading")
                        return null
                    }

                    val fileId = UUID.randomUUID().toString()

                    val transfer = FileTransfer(
                        fileId = fileId,
                        fileName = fileName,
                        size = fileSize,
                        uri = uri,
                        inputStream = inputStream
                    )

                    activeTransfers[fileId] = transfer

                    // Send file offer
                    val offer = FileOffer(fileId, fileName, fileSize)
                    val offerData = com.remotedexter.protocol.serialize(offer)
                    val cmd = CommandRequest("file_offer", offerData)

                    val response = sendCommand(cmd)

                    if (response.success) {
                        onStatus(fileId, "offered", "File offer sent: $fileName")
                        Log.d(TAG, "File offer sent: $fileName")
                        return fileId
                    } else {
                        onStatus(fileId, "error", "Failed to send file offer: ${response.message}")
                        cleanupTransfer(fileId)
                        return null
                    }
                } else {
                    onStatus("", "error", "Unable to get file information")
                    return null
                }
            } ?: run {
                onStatus("", "error", "Unable to query file information")
                return null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate file transfer", e)
            onStatus("", "error", "Failed to initiate file transfer: ${e.message}")
            return null
        }
    }

    fun handleFileAccept(accept: FileAccept) {
        val transfer = activeTransfers[accept.fileId] ?: run {
            Log.w(TAG, "Received accept for unknown transfer: ${accept.fileId}")
            return
        }

        if (transfer.status != "offered") {
            Log.w(TAG, "Received accept for transfer not in offered state: ${accept.fileId}")
            return
        }

        transfer.status = "sending"
        onStatus(accept.fileId, "accepted", "File transfer accepted, starting send")

        // Start sending chunks in background
        mainHandler.post {
            sendFileChunks(transfer)
        }
    }

    fun handleFileReject(reject: FileReject) {
        val transfer = activeTransfers[reject.fileId] ?: run {
            Log.w(TAG, "Received reject for unknown transfer: ${reject.fileId}")
            return
        }

        transfer.status = "rejected"
        transfer.error = reject.reason
        onStatus(reject.fileId, "rejected", "File transfer rejected: ${reject.reason}")
        cleanupTransfer(reject.fileId)
    }

    private fun sendFileChunks(transfer: FileTransfer) {
        try {
            val buffer = ByteArray(CHUNK_SIZE)
            var bytesRead: Int

            while (transfer.inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1 && transfer.status == "sending") {
                val chunkData = if (bytesRead == CHUNK_SIZE) buffer else buffer.copyOf(bytesRead)

                val chunk = FileChunk(
                    fileId = transfer.fileId,
                    offset = transfer.transferred,
                    data = chunkData,
                    eof = false
                )

                val chunkDataSerialized = com.remotedexter.protocol.serialize(chunk)
                val cmd = CommandRequest("file_chunk", chunkDataSerialized)

                val response = sendCommand(cmd)

                if (!response.success) {
                    transfer.status = "failed"
                    transfer.error = "Failed to send chunk: ${response.message}"
                    onStatus(transfer.fileId, "failed", transfer.error!!)
                    cleanupTransfer(transfer.fileId)
                    return
                }

                transfer.transferred += bytesRead.toLong()
                transfer.lastUpdate = System.currentTimeMillis()

                // Update progress
                val progress = (transfer.transferred.toFloat() / transfer.size.toFloat()) * 100f
                val elapsed = (System.currentTimeMillis() - transfer.startTime) / 1000f
                val speed = if (elapsed > 0) transfer.transferred / elapsed else 0f

                onProgress(transfer.fileId, progress, speed)
            }

            // Send final chunk with EOF
            if (transfer.status == "sending") {
                val finalChunk = FileChunk(
                    fileId = transfer.fileId,
                    offset = transfer.transferred,
                    data = byteArrayOf(),
                    eof = true
                )

                val finalChunkData = com.remotedexter.protocol.serialize(finalChunk)
                val cmd = CommandRequest("file_eof", finalChunkData)

                val response = sendCommand(cmd)

                if (response.success) {
                    transfer.status = "completed"
                    onStatus(transfer.fileId, "completed", "File transfer completed: ${transfer.fileName}")
                    Log.d(TAG, "File transfer completed: ${transfer.fileName}")
                } else {
                    transfer.status = "failed"
                    transfer.error = "Failed to send EOF: ${response.message}"
                    onStatus(transfer.fileId, "failed", transfer.error!!)
                }
            }

        } catch (e: IOException) {
            transfer.status = "failed"
            transfer.error = "Failed to read file: ${e.message}"
            onStatus(transfer.fileId, "failed", transfer.error!!)
            Log.e(TAG, "Failed to send file chunks", e)
        } finally {
            cleanupTransfer(transfer.fileId)
        }
    }

    fun cancelTransfer(fileId: String) {
        val transfer = activeTransfers[fileId] ?: return

        if (transfer.status == "completed" || transfer.status == "failed") {
            return
        }

        transfer.status = "cancelled"
        onStatus(fileId, "cancelled", "File transfer cancelled")
        cleanupTransfer(fileId)
    }

    fun getActiveTransfers(): List<FileTransfer> {
        return activeTransfers.values.toList()
    }

    private fun cleanupTransfer(fileId: String) {
        val transfer = activeTransfers[fileId] ?: return

        try {
            transfer.inputStream?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Failed to close input stream", e)
        }

        activeTransfers.remove(fileId)
    }
}