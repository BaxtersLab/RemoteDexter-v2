package com.remotedexter.agent

/**
 * Deterministic recovery decision choices.
 */
enum class RecoveryDecision {
    ALLOW, FORBID, ESCALATE
}

class LifelineProtocol(private val transitionTable: TransitionTable, private val defaultRecovery: String, private val maxRecoveryAttempts: Int = 3) {

    /**
     * Top-level lifeline check invoked when state.lifelineStatus indicates trouble.
     * This will consult decideRecovery and act accordingly.
     */
    fun lifelineCheck(state: AgentState) {
        // Run lifeline logic for dead-end statuses or explicit failures that request recovery.
        if (!state.lifelineStatus.isDeadEnd() && state.lastBlockResult?.status != Status.fail) return

        val decision = decideRecovery(state)
        when (decision) {
            RecoveryDecision.ALLOW -> invokeRecovery(state)
            RecoveryDecision.FORBID -> {
                // mark final dead-end; no recovery allowed
                state.lifelineStatus = LifelineStatus.DEADEND_FORBIDDEN
                // ensure the execution pointer is cleared to make termination deterministic
                state.executionPointer = null
            }
            RecoveryDecision.ESCALATE -> {
                state.lifelineStatus = LifelineStatus.ESCALATED
                // escalate is terminal: clear pointer to ensure run stops
                state.executionPointer = null
            }
        }
    }

    /**
     * Decide deterministically whether recovery should run, be forbidden, or escalate.
     * Rules (deterministic):
     * - If last result is null -> FORBID
     * - If last result is deadEnd -> FORBID
     * - If last result is fail -> ALLOW unless recovery attempts exceeded
     * - If last result is interrupt -> ESCALATE
     */
    fun decideRecovery(state: AgentState): RecoveryDecision {
        val last = state.lastBlockResult ?: return RecoveryDecision.FORBID
        when (last.status) {
            Status.deadEnd -> return RecoveryDecision.FORBID
            Status.interrupt -> return RecoveryDecision.ESCALATE
            Status.fail -> {
                val rec = last.recoveryHint ?: transitionTable.get(state.executionPointer ?: "")?.recovery ?: defaultRecovery
                if (rec == null) return RecoveryDecision.FORBID
                val attempts = state.recoveryAttempts.getOrDefault(rec, 0)
                return if (attempts >= maxRecoveryAttempts) RecoveryDecision.ESCALATE else RecoveryDecision.ALLOW
            }
            Status.success, Status.terminated -> return RecoveryDecision.FORBID
        }
    }

    /**
     * Perform recovery: choose the recovery target, glove-tap sync, and set execution pointer.
     * Increments recoveryAttempts counter to avoid infinite recovery loops.
     */
    fun invokeRecovery(state: AgentState) {
        val last = state.lastBlockResult ?: run { state.lifelineStatus = LifelineStatus.DEADEND; return }
        val rec = last.recoveryHint ?: transitionTable.get(state.executionPointer ?: "")?.recovery ?: defaultRecovery
        if (rec == null) {
            state.lifelineStatus = LifelineStatus.DEADEND
            return
        }

        // enforce deterministic reset
        gloveTapSync(state)

        // increment recovery attempt counter
        val att = state.recoveryAttempts.getOrDefault(rec, 0) + 1
        state.recoveryAttempts[rec] = att

        state.executionPointer = rec
        state.lifelineStatus = LifelineStatus.RECOVERING
        state.interruptQueue.clear()
    }

    fun gloveTapSync(state: AgentState) {
        state.loopCounters.clear()
        state.constitutionalFlags["deterministicMode"] = true
    }
}

