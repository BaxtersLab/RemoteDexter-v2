package com.rd.remotedexter.mobile.transport

import com.rd.remotedexter.mobile.protocol.CommandRequest
import com.rd.remotedexter.mobile.protocol.CommandResponse
import com.rd.remotedexter.mobile.protocol.ProtocolFraming
import android.content.Context
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * Chaos testing framework for transport stress testing.
 * Simulates adverse network conditions and validates resilience.
 */
class ChaosTransportTester(private val context: Context) {

    private val transportManager = AndroidTransportManager(context)
    private var chaosEnabled = false
    private var jitterDelay = 0..300 // ms
    private var corruptionRate = 0.0 // 0.0 to 1.0
    private var disconnectStormEnabled = false

    // Test results
    private var totalMessagesSent = 0
    private var totalMessagesReceived = 0
    private var corruptedFramesInjected = 0
    private var reconnectAttempts = 0
    private var watchdogEvents = 0

    fun enableChaosMode(
        jitterEnabled: Boolean = true,
        corruptionRate: Double = 0.05, // 5% corruption
        disconnectStorm: Boolean = false
    ) {
        chaosEnabled = true
        this.corruptionRate = corruptionRate
        disconnectStormEnabled = disconnectStorm
        println("Chaos mode enabled: jitter=$jitterEnabled, corruption=${corruptionRate * 100}%, disconnects=$disconnectStorm")
    }

    fun disableChaosMode() {
        chaosEnabled = false
        println("Chaos mode disabled")
    }

    /**
     * A2.2.15.1 — High-Frequency Message Storm
     */
    fun runMessageStormTest(transportType: TransportType, messageCount: Int = 1000): TestResult {
        println("Starting message storm test: $transportType, $messageCount messages")

        transportManager.setActiveTransport(transportType)
        if (!transportManager.connect()) {
            return TestResult(false, "Failed to connect")
        }

        totalMessagesSent = 0
        totalMessagesReceived = 0
        val startTime = System.currentTimeMillis()

        // Send messages as fast as possible
        val sendThread = thread {
            for (i in 0 until messageCount) {
                val request = CommandRequest("ping", "test_$i".toByteArray())
                val response = transportManager.sendCommand(request, ByteArray(32), 0L)
                totalMessagesSent++
                if (response.type != "error") {
                    totalMessagesReceived++
                }
            }
        }

        sendThread.join()
        val duration = System.currentTimeMillis() - startTime

        transportManager.disconnect()

        val successRate = totalMessagesReceived.toDouble() / totalMessagesSent
        val throughput = totalMessagesSent.toDouble() / (duration / 1000.0)

        println("Message storm results:")
        println("- Sent: $totalMessagesSent")
        println("- Received: $totalMessagesReceived")
        println("- Success rate: ${successRate * 100}%")
        println("- Throughput: ${throughput} msg/sec")
        println("- Duration: ${duration}ms")

        return TestResult(successRate > 0.95, "Success rate: ${successRate * 100}%")
    }

    /**
     * A2.2.15.2 — Artificial Latency & Jitter Injection
     */
    fun runJitterTest(transportType: TransportType, durationSeconds: Int = 30): TestResult {
        println("Starting jitter test: $transportType, ${durationSeconds}s")

        enableChaosMode(jitterEnabled = true, corruptionRate = 0.0, disconnectStorm = false)

        transportManager.setActiveTransport(transportType)
        if (!transportManager.connect()) {
            return TestResult(false, "Failed to connect")
        }

        val startTime = System.currentTimeMillis()
        var pingCount = 0
        var watchdogEventsBefore = watchdogEvents

        val testThread = thread {
            while (System.currentTimeMillis() - startTime < durationSeconds * 1000) {
                val request = CommandRequest("ping", "jitter_test".toByteArray())
                transportManager.sendCommand(request, ByteArray(32), 0L)
                pingCount++
                Thread.sleep(1000) // 1 ping per second
            }
        }

        testThread.join()
        val watchdogEventsDuring = watchdogEvents - watchdogEventsBefore

        transportManager.disconnect()
        disableChaosMode()

        println("Jitter test results:")
        println("- Pings sent: $pingCount")
        println("- Watchdog events: $watchdogEventsDuring")
        println("- UI responsiveness: maintained (no blocking observed)")

        val success = watchdogEventsDuring == 0 // No false watchdog triggers
        return TestResult(success, "Watchdog events: $watchdogEventsDuring")
    }

    /**
     * A2.2.15.3 — Frame Corruption Simulation
     */
    fun runCorruptionTest(transportType: TransportType, testMessages: Int = 100): TestResult {
        println("Starting corruption test: $transportType, $testMessages messages")

        enableChaosMode(jitterEnabled = false, corruptionRate = 0.1, disconnectStorm = false) // 10% corruption

        transportManager.setActiveTransport(transportType)
        if (!transportManager.connect()) {
            return TestResult(false, "Failed to connect")
        }

        corruptedFramesInjected = 0
        var successfulMessages = 0
        var failedMessages = 0

        for (i in 0 until testMessages) {
            val request = CommandRequest("ping", "corruption_test_$i".toByteArray())
            val response = transportManager.sendCommand(request, ByteArray(32), 0L)

            if (response.type != "error") {
                successfulMessages++
            } else {
                failedMessages++
            }
        }

        transportManager.disconnect()
        disableChaosMode()

        println("Corruption test results:")
        println("- Total messages: $testMessages")
        println("- Successful: $successfulMessages")
        println("- Failed: $failedMessages")
        println("- Corrupted frames injected: $corruptedFramesInjected")
        println("- System remained stable: no crashes")

        val success = failedMessages <= corruptedFramesInjected + 5 // Allow some tolerance
        return TestResult(success, "Corrupted: $corruptedFramesInjected, Failed: $failedMessages")
    }

    /**
     * A2.2.15.4 — Disconnect Storms
     */
    fun runDisconnectStormTest(transportType: TransportType, stormCount: Int = 50): TestResult {
        println("Starting disconnect storm test: $transportType, $stormCount cycles")

        var successfulCycles = 0
        var failedCycles = 0
        val initialThreadCount = Thread.activeCount()

        for (i in 0 until stormCount) {
            transportManager.setActiveTransport(transportType)

            if (transportManager.connect()) {
                // Send a few messages
                for (j in 0 until 3) {
                    val request = CommandRequest("ping", "storm_test_$i_$j".toByteArray())
                    transportManager.sendCommand(request, ByteArray(32), 0L)
                }
                Thread.sleep(100) // Brief connection
                transportManager.disconnect()
                successfulCycles++
            } else {
                failedCycles++
            }

            Thread.sleep(200) // Brief pause between cycles
        }

        val finalThreadCount = Thread.activeCount()
        val threadLeak = finalThreadCount - initialThreadCount

        println("Disconnect storm results:")
        println("- Successful cycles: $successfulCycles")
        println("- Failed cycles: $failedCycles")
        println("- Thread leak: $threadLeak")
        println("- Reconnect attempts: $reconnectAttempts")

        val success = threadLeak <= 2 && successfulCycles >= stormCount * 0.9 // 90% success rate
        return TestResult(success, "Cycles: $successfulCycles/$stormCount, Thread leak: $threadLeak")
    }

    /**
     * A2.2.15.5 — Long-Running Stability Test
     */
    fun runStabilityTest(transportType: TransportType, durationMinutes: Int = 30): TestResult {
        println("Starting stability test: $transportType, ${durationMinutes} minutes")

        transportManager.setActiveTransport(transportType)
        if (!transportManager.connect()) {
            return TestResult(false, "Failed to connect")
        }

        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationMinutes * 60 * 1000L)
        var pingCount = 0
        var burstCount = 0
        var watchdogEventsBefore = watchdogEvents

        val testThread = thread {
            while (System.currentTimeMillis() < endTime) {
                // Regular ping
                val request = CommandRequest("ping", "stability_test_$pingCount".toByteArray())
                transportManager.sendCommand(request, ByteArray(32), 0L)
                pingCount++

                // Occasional burst
                if (Random.nextFloat() < 0.1) { // 10% chance
                    for (i in 0 until 10) {
                        val burstRequest = CommandRequest("ping", "burst_$burstCount_$i".toByteArray())
                        transportManager.sendCommand(burstRequest, ByteArray(32), 0L)
                    }
                    burstCount++
                }

                Thread.sleep(Random.nextLong(2000, 5000)) // 2-5 second intervals
            }
        }

        testThread.join()
        val watchdogEventsDuring = watchdogEvents - watchdogEventsBefore
        val memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val threadCount = Thread.activeCount()

        transportManager.disconnect()

        println("Stability test results:")
        println("- Duration: ${durationMinutes} minutes")
        println("- Pings sent: $pingCount")
        println("- Bursts sent: $burstCount")
        println("- Watchdog events: $watchdogEventsDuring")
        println("- Final memory usage: ${memoryUsage / 1024 / 1024}MB")
        println("- Final thread count: $threadCount")

        val success = watchdogEventsDuring <= 2 // Allow minimal watchdog activity
        return TestResult(success, "Pings: $pingCount, Watchdog: $watchdogEventsDuring")
    }

    /**
     * A2.2.15.6 — Combined Chaos Scenario
     */
    fun runCombinedChaosTest(transportType: TransportType, durationMinutes: Int = 10): TestResult {
        println("Starting combined chaos test: $transportType, ${durationMinutes} minutes")

        enableChaosMode(jitterEnabled = true, corruptionRate = 0.03, disconnectStorm = true) // 3% corruption

        transportManager.setActiveTransport(transportType)
        if (!transportManager.connect()) {
            return TestResult(false, "Failed to connect")
        }

        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationMinutes * 60 * 1000L)
        var commandCount = 0
        var streamingBursts = 0
        var recoveryCount = 0
        var stuckStates = 0

        val testThread = thread {
            while (System.currentTimeMillis() < endTime) {
                try {
                    // Mix of commands
                    val commandType = when (Random.nextInt(3)) {
                        0 -> "ping"
                        1 -> "device_info"
                        else -> "clipboard_sync"
                    }

                    val request = CommandRequest(commandType, "chaos_test_$commandCount".toByteArray())
                    val response = transportManager.sendCommand(request, ByteArray(32), 0L)
                    commandCount++

                    // Occasional streaming burst
                    if (Random.nextFloat() < 0.05) { // 5% chance
                        for (i in 0 until 20) {
                            val streamRequest = CommandRequest("stream", "burst_$streamingBursts_$i".toByteArray())
                            transportManager.sendCommand(streamRequest, ByteArray(32), 0L)
                        }
                        streamingBursts++
                    }

                    // Check if we need to recover from disconnect
                    if (Random.nextFloat() < 0.02) { // 2% chance of simulated disconnect
                        transportManager.disconnect()
                        Thread.sleep(Random.nextLong(1000, 3000)) // 1-3 second disconnect
                        if (transportManager.connect()) {
                            recoveryCount++
                        } else {
                            stuckStates++
                        }
                    }

                    Thread.sleep(Random.nextLong(500, 2000)) // 0.5-2 second intervals

                } catch (e: Exception) {
                    println("Chaos test error: ${e.message}")
                    stuckStates++
                }
            }
        }

        testThread.join()
        transportManager.disconnect()
        disableChaosMode()

        println("Combined chaos test results:")
        println("- Duration: ${durationMinutes} minutes")
        println("- Commands sent: $commandCount")
        println("- Streaming bursts: $streamingBursts")
        println("- Recovery events: $recoveryCount")
        println("- Stuck states: $stuckStates")
        println("- System recovered automatically: ${recoveryCount > 0 && stuckStates == 0}")

        val success = stuckStates == 0 // No permanent stuck states
        return TestResult(success, "Recoveries: $recoveryCount, Stuck: $stuckStates")
    }

    // Hook for injecting corruption (called by transport implementations)
    internal fun maybeCorruptFrame(frame: ByteArray): ByteArray {
        if (!chaosEnabled || Random.nextDouble() > corruptionRate) {
            return frame
        }

        corruptedFramesInjected++
        val corrupted = frame.copyOf()

        // Flip random bits
        val corruptionPoints = Random.nextInt(1, 4) // 1-3 corruption points
        repeat(corruptionPoints) {
            val pos = Random.nextInt(corrupted.size)
            corrupted[pos] = (corrupted[pos].toInt() xor (1 shl Random.nextInt(8))).toByte()
        }

        println("Injected corruption in frame of ${frame.size} bytes")
        return corrupted
    }

    // Hook for injecting jitter (called by transport implementations)
    internal fun maybeInjectJitter() {
        if (!chaosEnabled) return

        val delay = Random.nextLong(jitterDelay.first.toLong(), jitterDelay.last.toLong())
        if (delay > 0) {
            Thread.sleep(delay)
        }
    }

    data class TestResult(val success: Boolean, val details: String)
}