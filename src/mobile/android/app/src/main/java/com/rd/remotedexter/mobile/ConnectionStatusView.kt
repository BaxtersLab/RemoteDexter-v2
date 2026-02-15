package com.rd.remotedexter.mobile

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.rd.remotedexter.mobile.telemetry.HealthAlert
import com.rd.remotedexter.mobile.telemetry.TransportHealthDashboard

/**
 * Lightweight Connection Status Indicator
 * Shows Green/Yellow/Red status with transport info
 */
class ConnectionStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var statusIndicator: ImageView
    private lateinit var statusText: TextView
    private lateinit var transportText: TextView

    private lateinit var healthDashboard: TransportHealthDashboard

    init {
        initializeViews()
        healthDashboard = TransportHealthDashboard()
        updateStatus()
    }

    private fun initializeViews() {
        LayoutInflater.from(context).inflate(R.layout.view_connection_status, this, true)
        statusIndicator = findViewById(R.id.statusIndicator)
        statusText = findViewById(R.id.statusText)
        transportText = findViewById(R.id.transportText)
    }

    fun updateStatus() {
        val snapshot = healthDashboard.getHealthSnapshot()
        val alerts = healthDashboard.checkForAlerts()

        // Update status indicator
        val statusResId = when {
            !snapshot.isConnected -> R.drawable.status_red
            alerts.any { it.level == HealthAlert.AlertLevel.ERROR } -> R.drawable.status_red
            alerts.any { it.level == HealthAlert.AlertLevel.WARNING } -> R.drawable.status_yellow
            else -> R.drawable.status_green
        }
        statusIndicator.setImageResource(statusResId)

        // Update status text
        val connectionStatus = if (snapshot.isConnected) "Connected" else "Disconnected"
        statusText.text = connectionStatus

        // Update transport text
        val transportName = when (snapshot.currentTransport) {
            com.rd.remotedexter.mobile.transport.TransportType.USB -> "USB"
            com.rd.remotedexter.mobile.transport.TransportType.WIFI_DIRECT -> "Wi-Fi Direct"
            com.rd.remotedexter.mobile.transport.TransportType.BLUETOOTH -> "Bluetooth"
            else -> "None"
        }
        transportText.text = transportName

        // Update text color based on status
        val textColor = when {
            !snapshot.isConnected -> android.graphics.Color.RED
            alerts.any { it.level == HealthAlert.AlertLevel.ERROR } -> android.graphics.Color.RED
            alerts.any { it.level == HealthAlert.AlertLevel.WARNING } -> android.graphics.Color.rgb(255, 152, 0) // Orange
            else -> android.graphics.Color.GREEN
        }
        statusText.setTextColor(textColor)
        transportText.setTextColor(textColor)
    }

    fun setOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener(listener)
    }
}