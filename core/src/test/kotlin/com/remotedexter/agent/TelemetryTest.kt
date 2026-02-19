package com.remotedexter.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertContains
import kotlin.test.assertFails
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TelemetryTest {

    private class TelemetryCollector : Telemetry {
        val transitions = mutableListOf<Pair<String?, String>>()
        val recoveryAttempts = mutableListOf<Triple<String, String, Int>>()
        val ends = mutableListOf<Map<String, Any?>>()

        override fun onTransition(from: String?, to: String) {
            transitions.add(Pair(from, to))
        }

        override fun onRecoveryAttempt(target: String, decision: String, attempt: Int) {
            recoveryAttempts.add(Triple(target, decision, attempt))
        }

        override fun onEnd(finalState: Map<String, Any?>) {
            ends.add(finalState)
        }
    }

    // Thread-safe collector for concurrency tests
    private class ConcurrentTelemetryCollector : Telemetry {
        val transitions = Collections.synchronizedList(mutableListOf<Pair<String?, String>>())
        val recoveryAttempts = Collections.synchronizedList(mutableListOf<Triple<String, String, Int>>())
        val ends = Collections.synchronizedList(mutableListOf<Map<String, Any?>>())

        override fun onTransition(from: String?, to: String) { transitions.add(Pair(from, to)) }
        override fun onRecoveryAttempt(target: String, decision: String, attempt: Int) { recoveryAttempts.add(Triple(target, decision, attempt)) }
        override fun onEnd(finalState: Map<String, Any?>) { ends.add(finalState) }
    }

    // Collector that records event order for strict ordering assertions
    private class OrderedTelemetryCollector : Telemetry {
        val events = Collections.synchronizedList(mutableListOf<Pair<String, Any?>>())
        override fun onTransition(from: String?, to: String) { events.add(Pair("transition", Pair(from, to))) }
        override fun onRecoveryAttempt(target: String, decision: String, attempt: Int) { events.add(Pair("recovery", Triple(target, decision, attempt))) }
        override fun onEnd(finalState: Map<String, Any?>) { events.add(Pair("end", finalState)) }
    }

    // Slow telemetry to exercise non-blocking behavior (engine should still finish)
    private class SlowTelemetry(val sleepMs: Long = 100) : Telemetry {
        val transitions = mutableListOf<Pair<String?, String>>()
        val recoveryAttempts = mutableListOf<Triple<String, String, Int>>()
        val ends = mutableListOf<Map<String, Any?>>()

        override fun onTransition(from: String?, to: String) {
            Thread.sleep(sleepMs)
            transitions.add(Pair(from, to))
        }

        override fun onRecoveryAttempt(target: String, decision: String, attempt: Int) {
            Thread.sleep(sleepMs)
            recoveryAttempts.add(Triple(target, decision, attempt))
        }

        override fun onEnd(finalState: Map<String, Any?>) {
            Thread.sleep(sleepMs)
            ends.add(finalState)
        }
    }

    private class SimpleBlock(
        override val name: String,
        override val inputs: List<String> = emptyList(),
        override val outputs: List<String> = emptyList(),
        override val constraints: List<(AgentState) -> Boolean> = emptyList(),
        override val allowedTransitions: List<String> = emptyList(),
        val body: (AgentState) -> BlockResult
    ) : Block {
        override fun execute(state: AgentState): BlockResult = body(state)
    }

    @Test
    fun telemetryRecordsTransitionAndEnd() {
        val a = SimpleBlock("A", outputs = listOf("x"), allowedTransitions = listOf("END")) { _ ->
            BlockResult(Status.success, mapOf("x" to 42), "END")
        }
        val end = EndBlock("END")

        val table = TransitionTable(mapOf(
            "A" to TransitionEntry(listOf("END"), fallback = null, recovery = null),
            "END" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        val lifeline = LifelineProtocol(table, "REC")
        val collector = TelemetryCollector()
        val engine = ExecutionEngine(mapOf("A" to a, "END" to end), table, constitution, lifeline, collector)

        val state = AgentState("A")
        engine.run(state)

        // onEnd should be called with final state containing our working memory
        assertTrue(collector.ends.isNotEmpty())
        val final = collector.ends.first()
        assertEquals(42, final["x"])
    }

    @Test
    fun telemetryRecordsRecoveryAttempts() {
        val a = SimpleBlock("A", allowedTransitions = listOf()) { _ ->
            BlockResult(Status.fail, emptyMap(), null, ErrorCodes.E400, recoveryHint = "REC")
        }
        val rec = SimpleBlock("REC", outputs = listOf("ok"), allowedTransitions = listOf("END")) { _ ->
            BlockResult(Status.success, mapOf("ok" to true), "END")
        }
        val end = EndBlock("END")

        val table = TransitionTable(mapOf(
            "A" to TransitionEntry(listOf("REC"), fallback = null, recovery = "REC"),
            "REC" to TransitionEntry(listOf("END"), fallback = null, recovery = null),
            "END" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        val lifeline = LifelineProtocol(table, "REC", maxRecoveryAttempts = 2)
        val collector = TelemetryCollector()
        val engine = ExecutionEngine(mapOf("A" to a, "REC" to rec, "END" to end), table, constitution, lifeline, collector)

        val state = AgentState("A")
        engine.run(state)

        assertTrue(collector.recoveryAttempts.isNotEmpty())
        val attempt = collector.recoveryAttempts.first()
        assertEquals("REC", attempt.first)
        // decision should be ALLOW (since attempts < maxRecoveryAttempts)
        assertEquals("ALLOW", attempt.second)
        assertEquals(1, attempt.third)
    }

    @Test
    fun telemetryIdempotence_singleEventEmittedOnce() {
        val a = SimpleBlock("A", outputs = listOf("x"), allowedTransitions = listOf("END")) { _ ->
            BlockResult(Status.success, mapOf("x" to 1), "END")
        }
        val end = EndBlock("END")

        val table = TransitionTable(mapOf(
            "A" to TransitionEntry(listOf("END"), fallback = null, recovery = null),
            "END" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        val lifeline = LifelineProtocol(table, "REC")
        val collector = TelemetryCollector()
        val engine = ExecutionEngine(mapOf("A" to a, "END" to end), table, constitution, lifeline, collector)

        val state = AgentState("A")
        engine.run(state)

        // single transition A -> END
        assertEquals(1, collector.transitions.size)
        // running again on same final state should not add more events
        engine.run(state)
        assertEquals(1, collector.transitions.size)
    }

    @Test
    fun telemetryConcurrency_separatesLifelines() {
        val a = SimpleBlock("A", outputs = listOf("x"), allowedTransitions = listOf("END")) { _ ->
            BlockResult(Status.success, mapOf("x" to 1), "END")
        }
        val end = EndBlock("END")

        val table = TransitionTable(mapOf(
            "A" to TransitionEntry(listOf("END"), fallback = null, recovery = null),
            "END" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        val lifeline = LifelineProtocol(table, "REC")
        val telemetry = ConcurrentTelemetryCollector()

        val engineBlocks = mapOf("A" to a, "END" to end)

        val latch = CountDownLatch(2)
        val t1 = Thread {
            val engine = ExecutionEngine(engineBlocks, table, constitution, lifeline, telemetry)
            engine.run(AgentState("A"))
            latch.countDown()
        }
        val t2 = Thread {
            val engine = ExecutionEngine(engineBlocks, table, constitution, lifeline, telemetry)
            engine.run(AgentState("A"))
            latch.countDown()
        }
        t1.start(); t2.start()
        val completed = latch.await(2, TimeUnit.SECONDS)
        assertTrue(completed)
        // should have two transition records (one per lifeline run)
        assertEquals(2, telemetry.transitions.size)
    }

    @Test
    fun telemetryNonBlocking_behavior() {
        val a = SimpleBlock("A", outputs = listOf("x"), allowedTransitions = listOf("END")) { _ ->
            BlockResult(Status.success, mapOf("x" to 7), "END")
        }
        val end = EndBlock("END")

        val table = TransitionTable(mapOf(
            "A" to TransitionEntry(listOf("END"), fallback = null, recovery = null),
            "END" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        val lifeline = LifelineProtocol(table, "REC")
        val slow = SlowTelemetry(50)
        val engine = ExecutionEngine(mapOf("A" to a, "END" to end), table, constitution, lifeline, slow)

        val start = System.nanoTime()
        engine.run(AgentState("A"))
        val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
        // even with slow telemetry, the run should complete quickly (less than 2000ms)
        assertTrue(durationMs < 2000, "Engine run took too long: ${durationMs}ms")
        assertEquals(1, slow.ends.size)
    }

    @Test
    fun telemetryEscalation_andErrorPath() {
        val a = SimpleBlock("A", allowedTransitions = listOf()) { _ ->
            BlockResult(Status.fail, emptyMap(), null, ErrorCodes.E400, recoveryHint = "REC")
        }
        val rec = SimpleBlock("REC", outputs = listOf("ok"), allowedTransitions = listOf("END")) { _ ->
            BlockResult(Status.success, mapOf("ok" to true), "END")
        }
        val end = EndBlock("END")

        val table = TransitionTable(mapOf(
            "A" to TransitionEntry(listOf("REC"), fallback = null, recovery = "REC"),
            "REC" to TransitionEntry(listOf("END"), fallback = null, recovery = null),
            "END" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        // set maxRecoveryAttempts = 0 to force ESCALATE decision on first failure
        val lifeline = LifelineProtocol(table, "REC", maxRecoveryAttempts = 0)
        val collector = TelemetryCollector()
        val engine = ExecutionEngine(mapOf("A" to a, "REC" to rec, "END" to end), table, constitution, lifeline, collector)

        val state = AgentState("A")
        engine.run(state)

        assertTrue(collector.recoveryAttempts.isNotEmpty())
        val attempt = collector.recoveryAttempts.first()
        assertEquals("REC", attempt.first)
        assertEquals(RecoveryDecision.ESCALATE.name, attempt.second)
        assertEquals(1, attempt.third)
        // engine should have set lifeline status to ESCALATED and not called onEnd
        assertEquals(LifelineStatus.ESCALATED, state.lifelineStatus)
        assertTrue(collector.ends.isEmpty())
    }

    @Test
    fun telemetryStrictOrdering_recoveryBeforeEnd() {
        val a = SimpleBlock("A", allowedTransitions = listOf()) { _ ->
            // fail and request recovery
            BlockResult(Status.fail, emptyMap(), null, ErrorCodes.E400, recoveryHint = "REC")
        }
        val rec = SimpleBlock("REC", outputs = listOf("ok"), allowedTransitions = listOf("END")) { _ ->
            BlockResult(Status.success, mapOf("ok" to true), "END")
        }
        val end = EndBlock("END")

        val table = TransitionTable(mapOf(
            "A" to TransitionEntry(listOf("REC"), fallback = null, recovery = "REC"),
            "REC" to TransitionEntry(listOf("END"), fallback = null, recovery = null),
            "END" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        val lifeline = LifelineProtocol(table, "REC", maxRecoveryAttempts = 2)
        val collector = OrderedTelemetryCollector()
        val engine = ExecutionEngine(mapOf("A" to a, "REC" to rec, "END" to end), table, constitution, lifeline, collector)

        val state = AgentState("A")
        engine.run(state)

        // ensure that a recovery event appears before the final 'end' event
        val events = collector.events.map { it.first }
        val recoveryIndex = events.indexOf("recovery")
        val endIndex = events.indexOf("end")
        assertTrue(recoveryIndex >= 0 && endIndex >= 0)
        assertTrue(recoveryIndex < endIndex, "recovery should occur before end")
    }
}
