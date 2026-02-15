package com.rd.remotedexter.mobile.telemetry

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Global Metrics Registry for RemoteDexter telemetry and observability.
 * Thread-safe singleton providing atomic counters and timing measurements.
 */
object MetricsRegistry {

    // Frame-level metrics
    val framesSent = AtomicLong(0)
    val framesReceived = AtomicLong(0)
    val bytesSent = AtomicLong(0)
    val bytesReceived = AtomicLong(0)

    // Command-level metrics
    val commandsSent = AtomicLong(0)
    val commandsReceived = AtomicLong(0)
    val responsesSent = AtomicLong(0)
    val responsesReceived = AtomicLong(0)

    // Connection metrics
    val reconnectAttempts = AtomicLong(0)
    val watchdogTimeouts = AtomicLong(0)

    // Streaming metrics
    val framesEncoded = AtomicLong(0)
    val framesDropped = AtomicLong(0)
    val streamingSessionDurationMs = AtomicLong(0)
    val streamingSessions = AtomicLong(0)

    // Latency metrics (RTT)
    val totalRttMeasurements = AtomicLong(0)
    val totalRttMs = AtomicLong(0)
    val maxRttMs = AtomicLong(0)
    val minRttMs = AtomicLong(Long.MAX_VALUE)

    // Timing accumulators for averages
    val totalFrameEncodeTimeMs = AtomicLong(0)
    val totalFrameSendTimeMs = AtomicLong(0)

    // Thread-safe increment helpers
    fun incrementFramesSent() = framesSent.incrementAndGet()
    fun incrementFramesReceived() = framesReceived.incrementAndGet()
    fun incrementBytesSent(bytes: Long) = bytesSent.addAndGet(bytes)
    fun incrementBytesReceived(bytes: Long) = bytesReceived.addAndGet(bytes)
    fun incrementCommandsSent() = commandsSent.incrementAndGet()
    fun incrementCommandsReceived() = commandsReceived.incrementAndGet()
    fun incrementResponsesSent() = responsesSent.incrementAndGet()
    fun incrementResponsesReceived() = responsesReceived.incrementAndGet()
    fun incrementReconnectAttempts() = reconnectAttempts.incrementAndGet()
    fun incrementWatchdogTimeouts() = watchdogTimeouts.incrementAndGet()
    fun incrementFramesEncoded() = framesEncoded.incrementAndGet()
    fun incrementFramesDropped() = framesDropped.incrementAndGet()

    // Streaming session tracking
    fun startStreamingSession(): Long {
        streamingSessions.incrementAndGet()
        return System.currentTimeMillis()
    }

    fun endStreamingSession(startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        streamingSessionDurationMs.addAndGet(duration)
    }

    // RTT measurement
    fun recordRtt(rttMs: Long) {
        totalRttMeasurements.incrementAndGet()
        totalRttMs.addAndGet(rttMs)

        // Update min/max atomically
        minRttMs.getAndUpdate { current -> if (rttMs < current) rttMs else current }
        maxRttMs.getAndUpdate { current -> if (rttMs > current) rttMs else current }
    }

    // Frame timing
    fun recordFrameEncodeTime(timeMs: Long) {
        totalFrameEncodeTimeMs.addAndGet(timeMs)
    }

    fun recordFrameSendTime(timeMs: Long) {
        totalFrameSendTimeMs.addAndGet(timeMs)
    }

    // Computed metrics
    fun getAvgRttMs(): Double {
        val count = totalRttMeasurements.get()
        return if (count > 0) totalRttMs.get().toDouble() / count else 0.0
    }

    fun getAvgFrameEncodeTimeMs(): Double {
        val count = framesEncoded.get()
        return if (count > 0) totalFrameEncodeTimeMs.get().toDouble() / count else 0.0
    }

    fun getAvgFrameSendTimeMs(): Double {
        val count = framesSent.get()
        return if (count > 0) totalFrameSendTimeMs.get().toDouble() / count else 0.0
    }

    fun getStreamingFps(): Double {
        val totalDurationSec = streamingSessionDurationMs.get() / 1000.0
        val totalFrames = framesEncoded.get()
        return if (totalDurationSec > 0) totalFrames / totalDurationSec else 0.0
    }

    fun getAvgStreamingSessionDurationMs(): Double {
        val sessions = streamingSessions.get()
        return if (sessions > 0) streamingSessionDurationMs.get().toDouble() / sessions else 0.0
    }

    // Reset all metrics (for testing)
    fun reset() {
        framesSent.set(0)
        framesReceived.set(0)
        bytesSent.set(0)
        bytesReceived.set(0)
        commandsSent.set(0)
        commandsReceived.set(0)
        responsesSent.set(0)
        responsesReceived.set(0)
        reconnectAttempts.set(0)
        watchdogTimeouts.set(0)
        framesEncoded.set(0)
        framesDropped.set(0)
        streamingSessionDurationMs.set(0)
        streamingSessions.set(0)
        totalRttMeasurements.set(0)
        totalRttMs.set(0)
        maxRttMs.set(0)
        minRttMs.set(Long.MAX_VALUE)
        totalFrameEncodeTimeMs.set(0)
        totalFrameSendTimeMs.set(0)
    }

    // Export metrics as map for dashboard/logging
    fun export(): Map<String, Any> {
        return mapOf(
            "framesSent" to framesSent.get(),
            "framesReceived" to framesReceived.get(),
            "bytesSent" to bytesSent.get(),
            "bytesReceived" to bytesReceived.get(),
            "commandsSent" to commandsSent.get(),
            "commandsReceived" to commandsReceived.get(),
            "responsesSent" to responsesSent.get(),
            "responsesReceived" to responsesReceived.get(),
            "reconnectAttempts" to reconnectAttempts.get(),
            "watchdogTimeouts" to watchdogTimeouts.get(),
            "framesEncoded" to framesEncoded.get(),
            "framesDropped" to framesDropped.get(),
            "avgRttMs" to getAvgRttMs(),
            "maxRttMs" to maxRttMs.get(),
            "minRttMs" to minRttMs.get(),
            "avgFrameEncodeTimeMs" to getAvgFrameEncodeTimeMs(),
            "avgFrameSendTimeMs" to getAvgFrameSendTimeMs(),
            "streamingFps" to getStreamingFps(),
            "avgStreamingSessionDurationMs" to getAvgStreamingSessionDurationMs(),
            "totalStreamingSessions" to streamingSessions.get()
        )
    }
}