package com.rd.remotedexter.mobile.telemetry

import com.rd.remotedexter.mobile.transport.TransportType

/**
 * Transport Health Dashboard - Internal diagnostics panel
 */
data class TransportHealthSnapshot(
    val currentTransport: TransportType?,
    val isConnected: Boolean,
    val lastActivityTimestamp: Long,
    val rttMs: Double,
    val maxRttMs: Long,
    val minRttMs: Long,
    val framesSent: Long,
    val framesReceived: Long,
    val bytesSent: Long,
    val bytesReceived: Long,
    val reconnectAttempts: Long,
    val watchdogTimeouts: Long,
    val streamingFps: Double,
    val framesDropped: Long,
    val avgStreamingSessionDurationMs: Double
) {
    val connectionAgeMs: Long
        get() = System.currentTimeMillis() - lastActivityTimestamp

    val dataThroughputBps: Double
        get() = if (connectionAgeMs > 0) {
            (bytesSent + bytesReceived).toDouble() / (connectionAgeMs / 1000.0)
        } else 0.0

    fun toDisplayString(): String {
        return """
        |=== Transport Health Dashboard ===
        |Transport: ${currentTransport ?: "None"}
        |Connected: $isConnected
        |Connection Age: ${connectionAgeMs / 1000}s
        |RTT: ${"%.1f".format(rttMs)}ms (min: ${minRttMs}ms, max: ${maxRttMs}ms)
        |Frames: $framesSent sent, $framesReceived received
        |Data: ${bytesSent / 1024}KB sent, ${bytesReceived / 1024}KB received
        |Throughput: ${"%.1f".format(dataThroughputBps)} B/s
        |Reconnects: $reconnectAttempts
        |Watchdog Timeouts: $watchdogTimeouts
        |Streaming: ${"%.1f".format(streamingFps)} FPS
        |Frames Dropped: $framesDropped
        |Avg Session: ${"%.1f".format(avgStreamingSessionDurationMs / 1000.0)}s
        |==================================
        """.trimMargin()
    }
}

class TransportHealthDashboard(private val metricsRegistry: MetricsRegistry = MetricsRegistry) {

    private var currentTransport: TransportType? = null
    private var isConnected = false
    private var lastActivityTimestamp = 0L

    fun updateTransportStatus(transport: TransportType?, connected: Boolean) {
        currentTransport = transport
        isConnected = connected
        if (connected) {
            lastActivityTimestamp = System.currentTimeMillis()
        }
    }

    fun recordActivity() {
        lastActivityTimestamp = System.currentTimeMillis()
    }

    fun getHealthSnapshot(): TransportHealthSnapshot {
        val metrics = metricsRegistry.export()
        return TransportHealthSnapshot(
            currentTransport = currentTransport,
            isConnected = isConnected,
            lastActivityTimestamp = lastActivityTimestamp,
            rttMs = metrics["avgRttMs"] as Double,
            maxRttMs = metrics["maxRttMs"] as Long,
            minRttMs = metrics["minRttMs"] as Long,
            framesSent = metrics["framesSent"] as Long,
            framesReceived = metrics["framesReceived"] as Long,
            bytesSent = metrics["bytesSent"] as Long,
            bytesReceived = metrics["bytesReceived"] as Long,
            reconnectAttempts = metrics["reconnectAttempts"] as Long,
            watchdogTimeouts = metrics["watchdogTimeouts"] as Long,
            streamingFps = metrics["streamingFps"] as Double,
            framesDropped = metrics["framesDropped"] as Long,
            avgStreamingSessionDurationMs = metrics["avgStreamingSessionDurationMs"] as Double
        )
    }

    fun getHealthSummary(): String {
        return getHealthSnapshot().toDisplayString()
    }

    // Alerting hooks
    fun checkForAlerts(): List<HealthAlert> {
        val alerts = mutableListOf<HealthAlert>()
        val snapshot = getHealthSnapshot()

        // High RTT alert
        if (snapshot.rttMs > 500.0) {
            alerts.add(HealthAlert(AlertLevel.WARNING, "High RTT", "RTT is ${snapshot.rttMs}ms (>500ms threshold)"))
        }

        // Frequent reconnects alert
        if (snapshot.reconnectAttempts > 5) {
            alerts.add(HealthAlert(AlertLevel.ERROR, "Frequent Reconnects", "${snapshot.reconnectAttempts} reconnect attempts detected"))
        }

        // Excessive frame drops alert
        if (snapshot.framesDropped > 100) {
            alerts.add(HealthAlert(AlertLevel.WARNING, "High Frame Drops", "${snapshot.framesDropped} frames dropped (>100 threshold)"))
        }

        // Watchdog timeouts alert
        if (snapshot.watchdogTimeouts > 3) {
            alerts.add(HealthAlert(AlertLevel.ERROR, "Watchdog Timeouts", "${snapshot.watchdogTimeouts} watchdog timeouts detected"))
        }

        // Connection age alert (no activity for 5 minutes)
        if (snapshot.connectionAgeMs > 300000 && snapshot.isConnected) {
            alerts.add(HealthAlert(AlertLevel.WARNING, "Stale Connection", "No activity for ${snapshot.connectionAgeMs / 1000}s"))
        }

        return alerts
    }
}

enum class AlertLevel {
    INFO, WARNING, ERROR
}

data class HealthAlert(
    val level: AlertLevel,
    val title: String,
    val description: String
)