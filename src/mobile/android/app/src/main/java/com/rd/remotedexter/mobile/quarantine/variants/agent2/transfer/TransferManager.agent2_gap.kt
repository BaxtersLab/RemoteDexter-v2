package com.rd.remotedexter.mobile.transfer
import android.content.Context
import android.net.Uri
import android.util.Log
import com.rd.remotedexter.mobile.protocol.CommandRequest
import com.rd.remotedexter.mobile.protocol.CommandResponse
import com.rd.remotedexter.mobile.protocol.FileOffer
import com.rd.remotedexter.mobile.protocol.FileAccept
import com.rd.remotedexter.mobile.protocol.FileReject
import com.rd.remotedexter.mobile.protocol.FileChunk

class TransferManager(
    private val context: Context,
    private val sendCommand: (CommandRequest) -> CommandResponse,
    private val onProgress: (fileId: String, progress: Float, speed: Float) -> Unit = { _, _, _ -> },
    private val onStatus: (fileId: String, status: String, message: String) -> Unit = { _, _, _ -> }
) {
    private val fileSender = FileSender(context, sendCommand, onProgress, onStatus)
    private val fileReceiver = FileReceiver(context, sendCommand, onProgress, onStatus)

    companion object {
        private const val TAG = "TransferManager"
    }

    // Public API for sending files
    fun sendFile(uri: Uri): String? {
        Log.d(TAG, "Initiating file send")
        return fileSender.sendFile(uri)
    }

    // Public API for cancelling transfers
    fun cancelTransfer(fileId: String) {
        Log.d(TAG, "Cancelling transfer: $fileId")
        fileSender.cancelTransfer(fileId)
        fileReceiver.cancelTransfer(fileId)
    }

    // Public API for getting active transfers
    fun getActiveTransfers(): List<Any> {
        val senderTransfers = fileSender.getActiveTransfers()
        val receiverTransfers = fileReceiver.getActiveTransfers()
        return senderTransfers + receiverTransfers
    }

    // Internal method to handle incoming commands from desktop
    fun handleCommand(command: String, data: ByteArray) {
        try {
            when (command) {
                "file_offer" -> {
                    val offer = com.rd.remotedexter.mobile.protocol.deserialize<FileOffer>(data)
                    fileReceiver.handleFileOffer(offer)
                }
                "file_accept" -> {
                    val accept = com.rd.remotedexter.mobile.protocol.deserialize<FileAccept>(data)
                    fileSender.handleFileAccept(accept)
                }
                "file_reject" -> {
                    val reject = com.rd.remotedexter.mobile.protocol.deserialize<FileReject>(data)
                    fileSender.handleFileReject(reject)
                }
                "file_chunk" -> {
                    val chunk = com.rd.remotedexter.mobile.protocol.deserialize<FileChunk>(data)
                    fileReceiver.handleFileChunk(chunk)
                }
                "file_eof" -> {
                    // EOF is handled in the chunk with eof=true
                    val chunk = com.rd.remotedexter.mobile.protocol.deserialize<FileChunk>(data)
                    fileReceiver.handleFileChunk(chunk)
                }
                else -> {
                    Log.w(TAG, "Unknown file transfer command: $command")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle file transfer command: $command", e)
            onStatus("", "error", "Failed to handle command: ${e.message}")
        }
    }

    // Cleanup method
    fun shutdown() {
        // Cancel all active transfers
        getActiveTransfers().forEach { transfer ->
            when (transfer) {
                is FileSender.FileTransfer -> fileSender.cancelTransfer(transfer.fileId)
                is FileReceiver.FileTransfer -> fileReceiver.cancelTransfer(transfer.fileId)
            }
        }
    }
}
