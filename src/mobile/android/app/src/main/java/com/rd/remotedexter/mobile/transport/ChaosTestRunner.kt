package com.rd.remotedexter.mobile.transport

import android.content.Context

/**
 * Test runner for A2.2.15 Transport Stress & Chaos Testing
 */
class ChaosTestRunner(private val context: Context) {

    private val chaosTester = ChaosTransportTester(context)
    private val transportManager = AndroidTransportManager(context, chaosTester)

    fun runAllTests(): TestSuiteResult {
        println("=== A2.2.15 Transport Stress & Chaos Testing Suite ===")
        println("Starting comprehensive transport resilience validation...")

        val results = mutableListOf<TestResult>()

        // A2.2.15.1 — High-Frequency Message Storm
        println("\n--- A2.2.15.1 High-Frequency Message Storm ---")
        TransportType.values().forEach { transportType ->
            val result = chaosTester.runMessageStormTest(transportType, 1000)
            results.add(TestResult("MessageStorm_$transportType", result))
            println("$transportType: ${result.details}")
        }

        // A2.2.15.2 — Artificial Latency & Jitter Injection
        println("\n--- A2.2.15.2 Artificial Latency & Jitter Injection ---")
        val jitterResult = chaosTester.runJitterTest(TransportType.WIFI_DIRECT, 30)
        results.add(TestResult("JitterTest", jitterResult))
        println("Jitter test: ${jitterResult.details}")

        // A2.2.15.3 — Frame Corruption Simulation
        println("\n--- A2.2.15.3 Frame Corruption Simulation ---")
        val corruptionResult = chaosTester.runCorruptionTest(TransportType.USB, 100)
        results.add(TestResult("CorruptionTest", corruptionResult))
        println("Corruption test: ${corruptionResult.details}")

        // A2.2.15.4 — Disconnect Storms
        println("\n--- A2.2.15.4 Disconnect Storms ---")
        TransportType.values().forEach { transportType ->
            val result = chaosTester.runDisconnectStormTest(transportType, 50)
            results.add(TestResult("DisconnectStorm_$transportType", result))
            println("$transportType: ${result.details}")
        }

        // A2.2.15.5 — Long-Running Stability Test
        println("\n--- A2.2.15.5 Long-Running Stability Test ---")
        val stabilityResult = chaosTester.runStabilityTest(TransportType.USB, 5) // Reduced for demo
        results.add(TestResult("StabilityTest", stabilityResult))
        println("Stability test: ${stabilityResult.details}")

        // A2.2.15.6 — Combined Chaos Scenario
        println("\n--- A2.2.15.6 Combined Chaos Scenario ---")
        val chaosResult = chaosTester.runCombinedChaosTest(TransportType.WIFI_DIRECT, 2) // Reduced for demo
        results.add(TestResult("CombinedChaos", chaosResult))
        println("Combined chaos test: ${chaosResult.details}")

        // Summarize results
        val passed = results.count { it.result.success }
        val total = results.size
        val successRate = (passed.toDouble() / total) * 100

        println("\n=== Test Suite Summary ===")
        println("Total tests: $total")
        println("Passed: $passed")
        println("Failed: ${total - passed}")
        println("Success rate: ${successRate}%")

        results.forEach { result ->
            val status = if (result.result.success) "✅ PASS" else "❌ FAIL"
            println("$status ${result.name}: ${result.result.details}")
        }

    fun runIndividualTest(testName: String): ChaosTransportTester.TestResult? {
        return when (testName) {
            "MessageStorm_BLUETOOTH" -> chaosTester.runMessageStormTest(TransportType.BLUETOOTH, 100)
            "MessageStorm_USB" -> chaosTester.runMessageStormTest(TransportType.USB, 100)
            "MessageStorm_WIFI_DIRECT" -> chaosTester.runMessageStormTest(TransportType.WIFI_DIRECT, 100)
            "JitterTest" -> chaosTester.runJitterTest(TransportType.WIFI_DIRECT, 10)
            "CorruptionTest" -> chaosTester.runCorruptionTest(TransportType.USB, 50)
            "DisconnectStorm_BLUETOOTH" -> chaosTester.runDisconnectStormTest(TransportType.BLUETOOTH, 20)
            "DisconnectStorm_USB" -> chaosTester.runDisconnectStormTest(TransportType.USB, 20)
            "DisconnectStorm_WIFI_DIRECT" -> chaosTester.runDisconnectStormTest(TransportType.WIFI_DIRECT, 20)
            "StabilityTest" -> chaosTester.runStabilityTest(TransportType.USB, 2)
            "CombinedChaos" -> chaosTester.runCombinedChaosTest(TransportType.WIFI_DIRECT, 1)
            else -> null
        }
    }

    data class TestResult(val name: String, val result: ChaosTransportTester.TestResult)
    data class TestSuiteResult(val overallSuccess: Boolean, val individualResults: List<TestResult>)
}