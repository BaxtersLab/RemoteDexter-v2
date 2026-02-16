package com.rd.remotedexter.mobile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.rd.remotedexter.mobile.telemetry.TransportHealthDashboard
import com.rd.remotedexter.mobile.telemetry.StructuredLogger
import com.rd.remotedexter.mobile.transport.TransportType
import com.rd.remotedexter.mobile.transport.TransportManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Minimal in-app Diagnostics Panel for RemoteDexter
 * Provides real-time telemetry and system health information
 */
class DiagnosticsActivity : AppCompatActivity() {

    private lateinit var transportSpinner: Spinner
    private lateinit var connectionStatusIndicator: ImageView
    private lateinit var statusText: TextView
    private lateinit var rttText: TextView
    private lateinit var framesText: TextView
    private lateinit var bytesText: TextView
    private lateinit var reconnectsText: TextView
    private lateinit var watchdogText: TextView
    private lateinit var streamingText: TextView
    private lateinit var copyTelemetryButton: Button
    private lateinit var copySystemReportButton: Button
    private lateinit var developerModeButton: Button
    private lateinit var refreshButton: Button

    private lateinit var healthDashboard: TransportHealthDashboard
    private lateinit var logger: StructuredLogger
    private lateinit var clipboardManager: ClipboardManager

    private val refreshHandler = android.os.Handler()
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateDiagnostics()
            refreshHandler.postDelayed(this, 2000) // Update every 2 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        initializeViews()
        initializeComponents()
        setupTransportSelector()
        setupButtons()
        startAutoRefresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun initializeViews() {
        transportSpinner = findViewById(R.id.transportSpinner)
        connectionStatusIndicator = findViewById(R.id.connectionStatusIndicator)
        statusText = findViewById(R.id.statusText)
        rttText = findViewById(R.id.rttText)
        framesText = findViewById(R.id.framesText)
        bytesText = findViewById(R.id.bytesText)
        reconnectsText = findViewById(R.id.reconnectsText)
        watchdogText = findViewById(R.id.watchdogText)
        streamingText = findViewById(R.id.streamingText)
        copyTelemetryButton = findViewById(R.id.copyTelemetryButton)
        copySystemReportButton = findViewById(R.id.copySystemReportButton)
        developerModeButton = findViewById(R.id.developerModeButton)
        refreshButton = findViewById(R.id.refreshButton)
    }

    private fun initializeComponents() {
        healthDashboard = TransportHealthDashboard()
        logger = StructuredLogger(this)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    private fun setupTransportSelector() {
        val transportOptions = arrayOf("USB", "Wi-Fi Direct", "Bluetooth")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, transportOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        transportSpinner.adapter = adapter

        // Set current transport if available
        val currentTransport = getCurrentTransport()
        val position = when (currentTransport) {
            TransportType.USB -> 0
            TransportType.WIFI_DIRECT -> 1
            TransportType.BLUETOOTH -> 2
            else -> 0
        }
        transportSpinner.setSelection(position)

        transportSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTransport = when (position) {
                    0 -> TransportType.USB
                    1 -> TransportType.WIFI_DIRECT
                    else -> TransportType.BLUETOOTH
                }
                switchTransport(selectedTransport)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        copyTelemetryButton.setOnClickListener {
            copyTelemetrySnapshot()
        }

        copySystemReportButton.setOnClickListener {
            copySystemReport()
        }

        developerModeButton.setOnClickListener {
            val intent = Intent(this, DeveloperModeActivity::class.java)
            startActivity(intent)
        }

        refreshButton.setOnClickListener {
            updateDiagnostics()
        }
    }

    private fun startAutoRefresh() {
        refreshHandler.post(refreshRunnable)
    }

    private fun updateDiagnostics() {
        val snapshot = healthDashboard.getHealthSnapshot()
        val alerts = healthDashboard.checkForAlerts()

        // Update connection status indicator
        updateConnectionStatus(snapshot, alerts)

        // Update text views
        statusText.text = buildStatusText(snapshot)
        rttText.text = "RTT: ${"%.1f".format(snapshot.rttMs)}ms (min: ${snapshot.minRttMs}ms, max: ${snapshot.maxRttMs}ms)"
        framesText.text = "Frames: ${snapshot.framesSent} sent, ${snapshot.framesReceived} received"
        bytesText.text = "Data: ${formatBytes(snapshot.bytesSent)} sent, ${formatBytes(snapshot.bytesReceived)} received"
        reconnectsText.text = "Reconnects: ${snapshot.reconnectAttempts}"
        watchdogText.text = "Watchdog: ${snapshot.watchdogTimeouts} timeouts"
        streamingText.text = "Streaming: ${"%.1f".format(snapshot.streamingFps)} FPS, ${snapshot.framesDropped} dropped"
    }

    private fun updateConnectionStatus(snapshot: com.rd.remotedexter.mobile.telemetry.TransportHealthSnapshot, alerts: List<com.rd.remotedexter.mobile.telemetry.HealthAlert>) {
        val statusResId = when {
            !snapshot.isConnected -> R.drawable.status_red
            alerts.any { it.level == com.rd.remotedexter.mobile.telemetry.AlertLevel.ERROR } -> R.drawable.status_red
            alerts.any { it.level == com.rd.remotedexter.mobile.telemetry.AlertLevel.WARNING } -> R.drawable.status_yellow
            else -> R.drawable.status_green
        }
        connectionStatusIndicator.setImageResource(statusResId)
    }

    private fun buildStatusText(snapshot: com.rd.remotedexter.mobile.telemetry.TransportHealthSnapshot): String {
        val transportName = when (snapshot.currentTransport) {
            TransportType.USB -> "USB"
            TransportType.WIFI_DIRECT -> "Wi-Fi Direct"
            TransportType.BLUETOOTH -> "Bluetooth"
            else -> "None"
        }

        val connectionStatus = if (snapshot.isConnected) "Connected" else "Disconnected"
        val age = if (snapshot.isConnected) " (${snapshot.connectionAgeMs / 1000}s)" else ""

        return "$transportName - $connectionStatus$age"
    }

    private fun copyTelemetrySnapshot() {
        val snapshot = healthDashboard.getHealthSnapshot()
        val json = JSONObject()

        json.put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        json.put("transport", snapshot.currentTransport?.name ?: "NONE")
        json.put("connected", snapshot.isConnected)
        json.put("connectionAgeMs", snapshot.connectionAgeMs)
        json.put("rttMs", snapshot.rttMs)
        json.put("maxRttMs", snapshot.maxRttMs)
        json.put("minRttMs", snapshot.minRttMs)
        json.put("framesSent", snapshot.framesSent)
        json.put("framesReceived", snapshot.framesReceived)
        json.put("bytesSent", snapshot.bytesSent)
        json.put("bytesReceived", snapshot.bytesReceived)
        json.put("reconnectAttempts", snapshot.reconnectAttempts)
        json.put("watchdogTimeouts", snapshot.watchdogTimeouts)
        json.put("streamingFps", snapshot.streamingFps)
        json.put("framesDropped", snapshot.framesDropped)
        json.put("throughputBps", snapshot.dataThroughputBps)

        val clip = ClipData.newPlainText("RemoteDexter Telemetry", json.toString(2))
        clipboardManager.setPrimaryClip(clip)

        Toast.makeText(this, "Telemetry snapshot copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun copySystemReport() {
        val snapshot = healthDashboard.getHealthSnapshot()
        val recentLogs = logger.exportLogs()

        val report = JSONObject()
        report.put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        report.put("appVersion", "1.0.0") // Would be dynamic in real app
        report.put("androidVersion", android.os.Build.VERSION.RELEASE)
        report.put("deviceModel", android.os.Build.MODEL)

        // Health snapshot
        val healthJson = JSONObject()
        healthJson.put("transport", snapshot.currentTransport?.name ?: "NONE")
        healthJson.put("connected", snapshot.isConnected)
        healthJson.put("rttMs", snapshot.rttMs)
        healthJson.put("framesSent", snapshot.framesSent)
        healthJson.put("framesReceived", snapshot.framesReceived)
        healthJson.put("reconnectAttempts", snapshot.reconnectAttempts)
        healthJson.put("watchdogTimeouts", snapshot.watchdogTimeouts)
        report.put("health", healthJson)

        // Recent logs (last 100 lines)
        val logLines = recentLogs.lines().takeLast(100)
        report.put("recentLogs", logLines.joinToString("\n"))

        val clip = ClipData.newPlainText("RemoteDexter System Report", report.toString(2))
        clipboardManager.setPrimaryClip(clip)

        Toast.makeText(this, "System report copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentTransport(): TransportType? {
        // This would need to be implemented to get current transport from TransportManager
        // For now, return null
        return null
    }

    private fun switchTransport(transportType: TransportType) {
        // This would need to call TransportManager.setActiveTransport()
        // For now, just show a toast
        Toast.makeText(this, "Switched to ${transportType.name}", Toast.LENGTH_SHORT).show()
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            bytes >= 1024 -> "${bytes / 1024}KB"
            else -> "${bytes}B"
        }
    }
}
