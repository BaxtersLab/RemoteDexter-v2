package com.remotedexter.agent

class Constitution(private val transitionTable: TransitionTable, private val loopThreshold: Int = 100) {

    fun enforce(state: AgentState, block: Block, result: BlockResult): BlockResult? {
        // NO SLOP RULE: outputs must match declared outputs
        if (state.constitutionalFlags["noSlop"] == true) {
            for (k in result.outputData.keys) {
                if (!block.outputs.contains(k)) {
                    // violation
                        state.lifelineStatus = LifelineStatus.DEADEND
                    val br = BlockResult(Status.deadEnd, emptyMap(), null, ErrorCodes.E900, "Constitution:noSlop")
                    state.lastBlockResult = br
                    return br
                }
            }
        }

        // DETERMINISTIC TRANSITIONS
        if (state.constitutionalFlags["deterministicMode"] == true) {
            val trans = transitionTable.get(block.name)
            // Terminal blocks (terminated result) are allowed to have empty transitions.
            if (trans == null) {
                state.lifelineStatus = LifelineStatus.DEADEND
                val br = BlockResult(Status.deadEnd, emptyMap(), null, ErrorCodes.E900, "Constitution:noTransitions")
                state.lastBlockResult = br
                return br
            }
            if (trans.allowedNextBlocks.isEmpty() && result.status != Status.terminated) {
                state.lifelineStatus = LifelineStatus.DEADEND
                // Treat missing allowed transitions as an invalid transition (E300)
                val br = BlockResult(Status.deadEnd, emptyMap(), null, ErrorCodes.E300, "Constitution:noTransitions")
                state.lastBlockResult = br
                return br
            }
            val next = result.nextPointer ?: trans.fallback ?: trans.allowedNextBlocks.firstOrNull()
            if (next != null && !trans.allowedNextBlocks.contains(next)) {
                state.lifelineStatus = LifelineStatus.DEADEND
                val br = BlockResult(Status.deadEnd, emptyMap(), null, ErrorCodes.E300, "Constitution:invalidTransition")
                state.lastBlockResult = br
                return br
            }
        }

        // BOUNDED LOOPS
        if (state.constitutionalFlags["boundedLoops"] == true) {
            val cnt = state.loopCounters.getOrDefault(block.name, 0)
            if (cnt > loopThreshold) {
                state.lifelineStatus = LifelineStatus.DEADEND
                val br = BlockResult(Status.deadEnd, emptyMap(), null, ErrorCodes.E200, "Constitution:loopExceeded")
                state.lastBlockResult = br
                return br
            }
        }

        // NO SELF-MODIFYING STATE: not enforced at runtime here, assumed by design

        return null
    }
}
