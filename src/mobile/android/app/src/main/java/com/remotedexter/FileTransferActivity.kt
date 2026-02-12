package com.remotedexter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.remotedexter.protocol.CommandRequest
import com.remotedexter.protocol.CommandResponse
import com.remotedexter.transfer.TransferManager

class FileTransferActivity : AppCompatActivity() {

    private lateinit var transferManager: TransferManager
    private lateinit var statusText: TextView
    private lateinit var sendButton: Button
    private lateinit var cancelButton: Button

    // Mock command sender (replace with actual implementation)
    private val mockSendCommand = { cmd: CommandRequest ->
        // This would normally send the command to the desktop via your transport layer
        // For demo purposes, we'll just simulate success
        CommandResponse(true, "Command sent successfully")
    }

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileId = transferManager.sendFile(it)
            if (fileId != null) {
                Toast.makeText(this, "File transfer initiated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to initiate transfer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create simple UI programmatically (in real app, use XML layout)
        statusText = TextView(this).apply {
            text = "File Transfer Status"
            textSize = 16f
            setPadding(16, 16, 16, 16)
        }

        sendButton = Button(this).apply {
            text = "Send File"
            setOnClickListener {
                filePickerLauncher.launch("*/*")
            }
        }

        cancelButton = Button(this).apply {
            text = "Cancel All Transfers"
            setOnClickListener {
                // Cancel all active transfers (in real app, show list to select which one)
                transferManager.getActiveTransfers().forEach { transfer ->
                    when (transfer) {
                        is com.remotedexter.transfer.FileSender.FileTransfer -> {
                            transferManager.cancelTransfer(transfer.fileId)
                        }
                        is com.remotedexter.transfer.FileReceiver.FileTransfer -> {
                            transferManager.cancelTransfer(transfer.fileId)
                        }
                    }
                }
                Toast.makeText(this@FileTransferActivity, "All transfers cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        // Simple vertical layout
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(statusText)
            addView(sendButton)
            addView(cancelButton)
            setPadding(32, 32, 32, 32)
        }

        setContentView(layout)

        // Initialize transfer manager
        transferManager = TransferManager(
            context = this,
            sendCommand = mockSendCommand,
            onProgress = { fileId, progress, speed ->
                runOnUiThread {
                    statusText.text = "Transfer $fileId: ${progress.toInt()}% (${speed.toInt()} B/s)"
                }
            },
            onStatus = { fileId, status, message ->
                runOnUiThread {
                    statusText.text = "Transfer $fileId: $status - $message"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        transferManager.shutdown()
    }

    // Method to handle incoming commands from desktop (call this from your transport layer)
    fun handleIncomingCommand(command: String, data: ByteArray) {
        transferManager.handleCommand(command, data)
    }
}