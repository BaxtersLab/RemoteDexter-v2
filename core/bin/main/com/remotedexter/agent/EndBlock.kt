package com.remotedexter.agent

/**
 * Explicit END block: terminates lifeline and emits a final state.
 * It is pure and returns a terminated BlockResult.
 */
class EndBlock(override val name: String = "END") : Block {
    override val inputs: List<String> = emptyList()
    override val outputs: List<String> = listOf("finalState")
    override val constraints: List<(AgentState) -> Boolean> = emptyList()
    override val allowedTransitions: List<String> = emptyList()

    override fun execute(state: AgentState): BlockResult {
        val final = state.workingMemory.toMap()
        // Attach final state as output; engine will interpret terminated status
        return BlockResult(Status.terminated, mapOf("finalState" to final), null)
    }
}
