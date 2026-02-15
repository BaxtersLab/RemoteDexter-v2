package com.rd.remotedexter.mobile.telemetry

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Structured JSON logging for RemoteDexter telemetry
 */
data class LogEntry(
    val timestamp: String,
    val subsystem: String,
    val event: String,
    val metadata: Map<String, Any> = emptyMap()
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("timestamp", timestamp)
        json.put("subsystem", subsystem)
        json.put("event", event)

        // Add metadata
        val metadataJson = JSONObject()
        metadata.forEach { (key, value) ->
            metadataJson.put(key, value)
        }
        json.put("metadata", metadataJson)

        return json.toString()
    }
}

class StructuredLogger(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val logFile: File by lazy {
        File(context.getExternalFilesDir(null), "remotedexter_telemetry.log")
    }

    private var logToFile = true
    private var logToConsole = true

    fun setLogToFile(enabled: Boolean) {
        logToFile = enabled
    }

    fun setLogToConsole(enabled: Boolean) {
        logToConsole = enabled
    }

    // Transport events
    fun logTransportConnected(transportType: String, metadata: Map<String, Any> = emptyMap()) {
        log("transport", "connected", metadata + mapOf("transport_type" to transportType))
    }

    fun logTransportDisconnected(transportType: String, reason: String? = null) {
        val metadata = mutableMapOf<String, Any>("transport_type" to transportType)
        reason?.let { metadata["reason"] = it }
        log("transport", "disconnected", metadata)
    }

    fun logFrameSent(size: Int, transportType: String) {
        log("transport", "frame_sent", mapOf(
            "size_bytes" to size,
            "transport_type" to transportType
        ))
    }

    fun logFrameReceived(size: Int, transportType: String) {
        log("transport", "frame_received", mapOf(
            "size_bytes" to size,
            "transport_type" to transportType
        ))
    }

    fun logReconnectAttempt(transportType: String, attemptNumber: Int) {
        log("transport", "reconnect_attempt", mapOf(
            "transport_type" to transportType,
            "attempt_number" to attemptNumber
        ))
    }

    fun logWatchdogTimeout(transportType: String, inactiveMs: Long) {
        log("transport", "watchdog_timeout", mapOf(
            "transport_type" to transportType,
            "inactive_ms" to inactiveMs
        ))
    }

    // Protocol events
    fun logCommandSent(commandType: String, payloadSize: Int) {
        log("protocol", "command_sent", mapOf(
            "command_type" to commandType,
            "payload_size" to payloadSize
        ))
    }

    fun logCommandReceived(commandType: String, payloadSize: Int) {
        log("protocol", "command_received", mapOf(
            "command_type" to commandType,
            "payload_size" to payloadSize
        ))
    }

    fun logResponseSent(status: String, payloadSize: Int) {
        log("protocol", "response_sent", mapOf(
            "status" to status,
            "payload_size" to payloadSize
        ))
    }

    fun logResponseReceived(status: String, payloadSize: Int) {
        log("protocol", "response_received", mapOf(
            "status" to status,
            "payload_size" to payloadSize
        ))
    }

    fun logProtocolError(error: String, context: String) {
        log("protocol", "error", mapOf(
            "error" to error,
            "context" to context
        ))
    }

    // Streaming events
    fun logStreamingStarted(sessionId: String) {
        log("streaming", "started", mapOf("session_id" to sessionId))
    }

    fun logStreamingEnded(sessionId: String, durationMs: Long, framesEncoded: Int) {
        log("streaming", "ended", mapOf(
            "session_id" to sessionId,
            "duration_ms" to durationMs,
            "frames_encoded" to framesEncoded
        ))
    }

    fun logFrameEncoded(encodeTimeMs: Long, sizeBytes: Int) {
        log("streaming", "frame_encoded", mapOf(
            "encode_time_ms" to encodeTimeMs,
            "size_bytes" to sizeBytes
        ))
    }

    fun logFrameDropped(reason: String) {
        log("streaming", "frame_dropped", mapOf("reason" to reason))
    }

    // Performance events
    fun logRttMeasurement(rttMs: Long) {
        log("performance", "rtt_measured", mapOf("rtt_ms" to rttMs))
    }

    fun logThroughputMeasurement(bytesPerSecond: Double, transportType: String) {
        log("performance", "throughput_measured", mapOf(
            "bytes_per_second" to bytesPerSecond,
            "transport_type" to transportType
        ))
    }

    // Error events
    fun logError(subsystem: String, error: String, stackTrace: String? = null) {
        val metadata = mutableMapOf<String, Any>("error" to error)
        stackTrace?.let { metadata["stack_trace"] = it }
        log(subsystem, "error", metadata)
    }

    fun logWarning(subsystem: String, warning: String) {
        log(subsystem, "warning", mapOf("warning" to warning))
    }

    // Generic logging method
    fun log(subsystem: String, event: String, metadata: Map<String, Any> = emptyMap()) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            subsystem = subsystem,
            event = event,
            metadata = metadata
        )

        if (logToConsole) {
            println("[${entry.timestamp}] ${entry.subsystem}.${entry.event}: ${entry.metadata}")
        }

        if (logToFile) {
            try {
                FileWriter(logFile, true).use { writer ->
                    writer.appendLine(entry.toJsonString())
                }
            } catch (e: Exception) {
                // Fallback to console if file logging fails
                println("Failed to write to log file: ${e.message}")
            }
        }
    }

    // Export logs for debugging
    fun exportLogs(): String {
        return try {
            logFile.readText()
        } catch (e: Exception) {
            "Failed to read log file: ${e.message}"
        }
    }

    // Clear logs
    fun clearLogs() {
        try {
            logFile.writeText("")
        } catch (e: Exception) {
            println("Failed to clear log file: ${e.message}")
        }
    }

    // Get log file size
    fun getLogFileSize(): Long {
        return try {
            logFile.length()
        } catch (e: Exception) {
            0L
        }
    }
}