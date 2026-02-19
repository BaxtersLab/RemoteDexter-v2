package com.remotedexter.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExecutionEngineContractTest {

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
    fun endBlockTerminatesDeterministically() {
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
        val engine = ExecutionEngine(mapOf("A" to a, "END" to end), table, constitution, lifeline, NoopTelemetry())

        val state = AgentState("A")
        // allow test to inject keys not strictly declared in outputs for interrupt routing
        state.constitutionalFlags["noSlop"] = false
        engine.run(state)

        assertEquals(null, state.executionPointer)
        assertEquals(LifelineStatus.TERMINATED, state.lifelineStatus)
        val final = state.workingMemory["x"]
        assertEquals(42, final)
    }

    @Test
    fun illegalTransitionIsRejectedByConstitution() {
        val a = SimpleBlock("A", allowedTransitions = listOf()) { _ ->
            BlockResult(Status.success, emptyMap(), "MISSING")
        }

        val table = TransitionTable(mapOf(
            "A" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        val lifeline = LifelineProtocol(table, "REC")
        val engine = ExecutionEngine(mapOf("A" to a), table, constitution, lifeline)

        val state = AgentState("A")
        engine.run(state)

        assertNotNull(state.lastBlockResult)
        assertEquals(ErrorCodes.E300, state.lastBlockResult?.errorCode)
        assertTrue(state.lifelineStatus.isDeadEnd())
    }

    @Test
    fun recoveryPathIsTakenWhenAllowed() {
        val a = SimpleBlock("A", allowedTransitions = listOf()) { _ ->
            BlockResult(Status.fail, emptyMap(), null, ErrorCodes.E400, recoveryHint = "REC")
        }
        val rec = SimpleBlock("REC", outputs = listOf("ok"), allowedTransitions = listOf("END")) { s ->
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
        val engine = ExecutionEngine(mapOf("A" to a, "REC" to rec, "END" to end), table, constitution, lifeline)

        val state = AgentState("A")
        engine.run(state)

        // recoveryAttempts should have recorded at least one attempt
        assertTrue(state.recoveryAttempts.getOrDefault("REC", 0) >= 1)
        assertEquals(true, state.workingMemory["ok"])
        assertEquals(null, state.executionPointer)
    }

    @Test
    fun deadEndRecoveryIsForbiddenAndEscalates() {
        val a = SimpleBlock("A") { _ ->
            BlockResult(Status.deadEnd, emptyMap(), null, ErrorCodes.E200)
        }

        val table = TransitionTable(mapOf(
            "A" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        val lifeline = LifelineProtocol(table, "REC")
        val engine = ExecutionEngine(mapOf("A" to a), table, constitution, lifeline)

        val state = AgentState("A")
        engine.run(state)

        assertTrue(state.lifelineStatus.isDeadEnd())
        // When deadEnd -> recovery forbidden
        assertEquals(LifelineStatus.DEADEND_FORBIDDEN, state.lifelineStatus)
    }

    @Test
    fun interruptRoutesToHandler() {
        val a = SimpleBlock("A", outputs = listOf("alert"), allowedTransitions = listOf("END")) { _ ->
            // explicitly route to HANDLER when interrupting to ensure handler executes
            BlockResult(Status.interrupt, mapOf("alert" to "ok"), "HANDLER", null, null, "alert")
        }

        val handler = SimpleBlock("HANDLER", outputs = listOf("handled"), allowedTransitions = listOf("END")) { _ ->
            BlockResult(Status.success, mapOf("handled" to true), "END")
        }

        val end = EndBlock("END")

        val table = TransitionTable(mapOf(
            // allow HANDLER as a legal next block so the interrupt routing is valid under the constitution
            "A" to TransitionEntry(listOf("HANDLER", "END"), fallback = null, recovery = null, interruptTransitions = mapOf("alert" to "HANDLER")),
            "HANDLER" to TransitionEntry(listOf("END"), fallback = null, recovery = null),
            "END" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        val lifeline = LifelineProtocol(table, "REC")
        val engine = ExecutionEngine(mapOf("A" to a, "HANDLER" to handler, "END" to end), table, constitution, lifeline)

        val state = AgentState("A")
        engine.run(state)
        // debug info
        println("LAST_RESULT: ${state.lastBlockResult}")
        println("RECOVERY_ATTEMPTS: ${state.recoveryAttempts}")
        println("LIFELINE_STATUS: ${state.lifelineStatus}")
        println("WORKING_MEMORY: ${state.workingMemory}")

        assertEquals(true, state.workingMemory["handled"])
        assertEquals(LifelineStatus.TERMINATED, state.lifelineStatus)
        assertEquals(null, state.executionPointer)
    }

    @Test
    fun failRecoveryExceedingAttemptsEscalates() {
        val a = SimpleBlock("A", allowedTransitions = listOf()) { _ ->
            BlockResult(Status.fail, emptyMap(), null, ErrorCodes.E400, recoveryHint = "REC")
        }

        val rec = SimpleBlock("REC", outputs = listOf("ok"), allowedTransitions = listOf("END")) { s ->
            BlockResult(Status.success, mapOf("ok" to true), "END")
        }
        val end = EndBlock("END")

        val table = TransitionTable(mapOf(
            "A" to TransitionEntry(listOf("REC"), fallback = null, recovery = "REC"),
            "REC" to TransitionEntry(listOf("END"), fallback = null, recovery = null),
            "END" to TransitionEntry(listOf(), fallback = null, recovery = null)
        ))

        val constitution = Constitution(table, loopThreshold = 10)
        // set maxRecoveryAttempts to 0 so any recovery attempt is considered exceeded
        val lifeline = LifelineProtocol(table, "REC", maxRecoveryAttempts = 0)
        val engine = ExecutionEngine(mapOf("A" to a, "REC" to rec, "END" to end), table, constitution, lifeline)

        val state = AgentState("A")
        engine.run(state)

        assertEquals(LifelineStatus.ESCALATED, state.lifelineStatus)
        assertEquals(null, state.executionPointer)
    }
}
