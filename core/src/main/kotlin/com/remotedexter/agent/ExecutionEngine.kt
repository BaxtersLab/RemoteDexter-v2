package com.remotedexter.agent

class ExecutionEngine(
    private val blocks: Map<String, Block>,
    private val transitionTable: TransitionTable,
    private val constitution: Constitution,
    private val lifeline: LifelineProtocol,
    private val telemetry: Telemetry = NoopTelemetry()
) {
    fun run(state: AgentState) {
        while (true) {
            val pointer = state.executionPointer ?: break
            val block = blocks[pointer]
                if (block == null) {
                    state.lastBlockResult = BlockResult(Status.deadEnd, emptyMap(), null, ErrorCodes.E200, "missingBlock")
                    state.lifelineStatus = LifelineStatus.DEADEND
                // report possible recovery decision
                val decision = lifeline.decideRecovery(state)
                val rec = state.lastBlockResult?.recoveryHint ?: transitionTable.get(state.executionPointer ?: "")?.recovery
                if (rec != null) {
                    val attempts = state.recoveryAttempts.getOrDefault(rec, 0) + 1
                    telemetry.onRecoveryAttempt(rec, decision.name, attempts)
                }
                lifeline.lifelineCheck(state)
                // If lifeline set a recovery pointer, continue the run to perform recovery deterministically in the same invocation.
                if (state.executionPointer != null) continue
                break
            }

            // Validate constraints
            for (c in block.constraints) {
                if (!c(state)) {
                    state.lastBlockResult = BlockResult(Status.fail, emptyMap(), null, ErrorCodes.E100, "constraintFailed")
                        state.lifelineStatus = LifelineStatus.DEADEND
                    lifeline.lifelineCheck(state)
                    return
                }
            }

            // Execute block (pure)
            val result = try {
                block.execute(state)
            } catch (ex: Exception) {
                BlockResult(Status.fail, emptyMap(), null, ErrorCodes.E400, ex.message)
            }

            // Apply constitution
            val consFailure = constitution.enforce(state, block, result)
            if (consFailure != null) {
                state.lastBlockResult = consFailure
                val decision = lifeline.decideRecovery(state)
                val rec = consFailure.recoveryHint ?: transitionTable.get(state.executionPointer ?: "")?.recovery
                if (rec != null) {
                    val attempts = state.recoveryAttempts.getOrDefault(rec, 0) + 1
                    telemetry.onRecoveryAttempt(rec, decision.name, attempts)
                }
                lifeline.lifelineCheck(state)
                    if (state.lifelineStatus.isDeadEnd() || state.lifelineStatus == LifelineStatus.ESCALATED) return
                continue
            }

            // Apply outputs immutably (after block returns)
            for ((k, v) in result.outputData) {
                state.workingMemory[k] = v
            }

            state.lastBlockResult = result

            // Handle fail: consult lifeline for deterministic recovery decision
            if (result.status == Status.fail) {
                val decision = lifeline.decideRecovery(state)
                val rec = result.recoveryHint ?: transitionTable.get(state.executionPointer ?: "")?.recovery
                if (rec != null) {
                    val attempts = state.recoveryAttempts.getOrDefault(rec, 0) + 1
                    telemetry.onRecoveryAttempt(rec, decision.name, attempts)
                }
                lifeline.lifelineCheck(state)
                    if (state.lifelineStatus.isDeadEnd() || state.lifelineStatus == LifelineStatus.ESCALATED) return
                // If lifeline set a recovery pointer, loop to handle it immediately
                if (state.executionPointer != null && state.executionPointer != pointer) continue
            }

            // Handle interrupts
            if (result.status == Status.interrupt) {
                val itype = result.interruptType ?: "generic"
                state.interruptQueue.add(Interrupt(itype, result.outputData, 0))
                    state.lifelineStatus = LifelineStatus.INTERRUPTED
            }

            // Dead end
            if (result.status == Status.deadEnd) {
                state.lifelineStatus = LifelineStatus.DEADEND
                val decision = lifeline.decideRecovery(state)
                val rec = result.recoveryHint ?: transitionTable.get(state.executionPointer ?: "")?.recovery
                if (rec != null) {
                    val attempts = state.recoveryAttempts.getOrDefault(rec, 0) + 1
                    telemetry.onRecoveryAttempt(rec, decision.name, attempts)
                }
                lifeline.lifelineCheck(state)
                    if (state.lifelineStatus.isDeadEnd() || state.lifelineStatus == LifelineStatus.ESCALATED) return
                continue
            }

            // Terminated: explicit END block
            if (result.status == Status.terminated) {
                // emit final state and stop deterministically
                    state.lifelineStatus = LifelineStatus.TERMINATED
                val finalState = result.outputData["finalState"] as? Map<String, Any?> ?: state.workingMemory.toMap()
                telemetry.onEnd(finalState)
                // ensure pointer cleared
                state.executionPointer = null
                return
            }

            // Update loop counter
            val cnt = state.loopCounters.getOrDefault(block.name, 0)
            state.loopCounters[block.name] = cnt + 1

            // Determine next pointer
            val next = result.nextPointer ?: transitionTable.get(block.name)?.fallback ?: transitionTable.get(block.name)?.allowedNextBlocks?.firstOrNull()
            val prevPointer = state.executionPointer
            state.executionPointer = next
            // telemetry transition
            if (state.executionPointer != null) telemetry.onTransition(prevPointer, state.executionPointer!!)

            // Check interrupt queue
            if (state.interruptQueue.isNotEmpty() && state.constitutionalFlags["allowInterrupts"] == true) {
                // pop highest priority
                val highest = state.interruptQueue.maxByOrNull { it.priority }!!
                state.interruptQueue.remove(highest)
                val handler = transitionTable.get(block.name)?.interruptTransitions?.get(highest.type)
                if (handler != null) {
                    state.executionPointer = handler
                        state.lifelineStatus = LifelineStatus.INTERRUPTED
                }
            }

            // stop conditions
            if (state.executionPointer == null) break
                if (state.lifelineStatus.isDeadEnd() || state.lifelineStatus == LifelineStatus.ESCALATED) break
        }
    }
}
