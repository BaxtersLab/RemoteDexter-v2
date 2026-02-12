package com.remotedexter

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class RDService : Service() {

    companion object {
        private const val TAG = "RDService"
        private const val NOTIFICATION_CHANNEL_ID = "remotedexter_service"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var transferManager: com.remotedexter.transfer.TransferManager
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RDService created")

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Initialize transfer manager
        transferManager = com.remotedexter.transfer.TransferManager(
            context = this,
            sendCommand = { command ->
                // In a real implementation, this would send to the desktop via transport layer
                Log.d(TAG, "Sending command: ${command.type}")
                com.remotedexter.protocol.CommandResponse(true, "Command sent")
            },
            onProgress = { fileId, progress, speed ->
                updateNotification("Transferring... ${progress.toInt()}%")
            },
            onStatus = { fileId, status, message ->
                when (status) {
                    "completed" -> showCompletionNotification(message)
                    "failed" -> showErrorNotification(message)
                    else -> updateNotification("Transfer: $status")
                }
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "RDService started")

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification("RemoteDexter running in background"))

        // In a real implementation, you would:
        // 1. Initialize transport layer for incoming connections
        // 2. Set up trust store access
        // 3. Register for incoming session requests
        // 4. Handle file transfer requests

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service doesn't support binding
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "RDService destroyed")

        // Cleanup
        transferManager.shutdown()
        stopForeground(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "RemoteDexter Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service for RemoteDexter remote desktop"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_share) // Replace with actual icon
            .setContentTitle("RemoteDexter")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(message: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle("Transfer Complete")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle("Transfer Error")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // Public methods for activity to interact with service

    fun handleIncomingCommand(command: String, data: ByteArray) {
        transferManager.handleCommand(command, data)
    }

    fun sendFile(uri: android.net.Uri): String? {
        return transferManager.sendFile(uri)
    }

    fun cancelTransfer(fileId: String) {
        transferManager.cancelTransfer(fileId)
    }

    fun getActiveTransfers(): List<Any> {
        return transferManager.getActiveTransfers()
    }
}