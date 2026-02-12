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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class FileReceiver(
    private val context: Context,
    private val sendCommand: (CommandRequest) -> CommandResponse,
    private val onProgress: (fileId: String, progress: Float, speed: Float) -> Unit = { _, _, _ -> },
    private val onStatus: (fileId: String, status: String, message: String) -> Unit = { _, _, _ -> }
) {
    private val activeTransfers = ConcurrentHashMap<String, FileTransfer>()
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "FileReceiver"
        private const val MAX_CONCURRENT_TRANSFERS = 2
        private const val MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024 // 2GB
    }

    data class FileTransfer(
        val fileId: String,
        val fileName: String,
        val size: Long,
        var transferred: Long = 0,
        val startTime: Long = System.currentTimeMillis(),
        var lastUpdate: Long = System.currentTimeMillis(),
        var status: String = "receiving",
        var error: String? = null,
        var file: File? = null,
        var outputStream: FileOutputStream? = null
    )

    fun handleFileOffer(offer: FileOffer) {
        Log.d(TAG, "Received file offer: ${offer.fileName} (${offer.size} bytes)")

        // Check concurrent transfer limit
        if (activeTransfers.size >= MAX_CONCURRENT_TRANSFERS) {
            rejectFile(offer.fileId, "Maximum concurrent transfers reached")
            return
        }

        // Validate file size
        if (offer.size > MAX_FILE_SIZE) {
            rejectFile(offer.fileId, "File too large: ${offer.size} bytes (max: $MAX_FILE_SIZE)")
            return
        }

        // For now, auto-accept all transfers (in a real app, show user dialog)
        acceptFile(offer)
    }

    private fun acceptFile(offer: FileOffer) {
        try {
            // Create file in app-private storage
            val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
            downloadsDir.mkdirs()

            val destFile = File(downloadsDir, offer.fileName)
            if (destFile.exists()) {
                // Handle file name conflicts
                var counter = 1
                val nameWithoutExt = offer.fileName.substringBeforeLast(".")
                val extension = offer.fileName.substringAfterLast(".", "")
                var newName = offer.fileName

                while (File(downloadsDir, newName).exists()) {
                    newName = if (extension.isNotEmpty()) {
                        "$nameWithoutExt ($counter).$extension"
                    } else {
                        "$nameWithoutExt ($counter)"
                    }
                    counter++
                }

                destFile = File(downloadsDir, newName)
            }

            val outputStream = FileOutputStream(destFile)

            val transfer = FileTransfer(
                fileId = offer.fileId,
                fileName = destFile.name,
                size = offer.size,
                file = destFile,
                outputStream = outputStream
            )

            activeTransfers[offer.fileId] = transfer

            // Send acceptance
            val accept = FileAccept(offer.fileId)
            val acceptData = com.remotedexter.protocol.serialize(accept)
            val cmd = CommandRequest("file_accept", acceptData)

            sendCommand(cmd)

            onStatus(offer.fileId, "accepted", "Accepted file transfer: ${destFile.name}")
            Log.d(TAG, "Accepted file transfer: ${destFile.name}")

        } catch (e: IOException) {
            Log.e(TAG, "Failed to create destination file", e)
            rejectFile(offer.fileId, "Failed to create destination file: ${e.message}")
        }
    }

    private fun rejectFile(fileId: String, reason: String) {
        val reject = FileReject(fileId, reason)
        val rejectData = com.remotedexter.protocol.serialize(reject)
        val cmd = CommandRequest("file_reject", rejectData)

        sendCommand(cmd)

        onStatus(fileId, "rejected", "Rejected file transfer: $reason")
        Log.d(TAG, "Rejected file transfer: $reason")
    }

    fun handleFileChunk(chunk: FileChunk) {
        val transfer = activeTransfers[chunk.fileId] ?: run {
            Log.w(TAG, "Received chunk for unknown transfer: ${chunk.fileId}")
            return
        }

        if (transfer.status != "receiving") {
            Log.w(TAG, "Received chunk for transfer not in receiving state: ${chunk.fileId}")
            return
        }

        try {
            // Validate chunk offset
            if (chunk.offset != transfer.transferred) {
                transfer.status = "failed"
                transfer.error = "Chunk offset mismatch: expected ${transfer.transferred}, got ${chunk.offset}"
                onStatus(chunk.fileId, "failed", transfer.error!!)
                cleanupTransfer(chunk.fileId)
                return
            }

            // Write chunk data
            transfer.outputStream?.write(chunk.data)

            transfer.transferred += chunk.data.size.toLong()
            transfer.lastUpdate = System.currentTimeMillis()

            // Update progress
            val progress = (transfer.transferred.toFloat() / transfer.size.toFloat()) * 100f
            val elapsed = (System.currentTimeMillis() - transfer.startTime) / 1000f
            val speed = if (elapsed > 0) transfer.transferred / elapsed else 0f

            onProgress(chunk.fileId, progress, speed)

            // Check for completion
            if (chunk.eof) {
                // Verify final size
                if (transfer.transferred != transfer.size) {
                    transfer.status = "failed"
                    transfer.error = "Final size mismatch: expected ${transfer.size}, got ${transfer.transferred}"
                    onStatus(chunk.fileId, "failed", transfer.error!!)
                } else {
                    transfer.status = "completed"
                    onStatus(chunk.fileId, "completed", "File transfer completed: ${transfer.fileName}")
                    Log.d(TAG, "File transfer completed: ${transfer.fileName}")
                }
                cleanupTransfer(chunk.fileId)
            }

        } catch (e: IOException) {
            transfer.status = "failed"
            transfer.error = "Failed to write chunk: ${e.message}"
            onStatus(chunk.fileId, "failed", transfer.error!!)
            cleanupTransfer(chunk.fileId)
            Log.e(TAG, "Failed to write chunk", e)
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
            transfer.outputStream?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Failed to close output stream", e)
        }

        // If transfer failed, delete partial file
        if (transfer.status == "failed" || transfer.status == "cancelled") {
            transfer.file?.delete()
        }

        activeTransfers.remove(fileId)
    }
}