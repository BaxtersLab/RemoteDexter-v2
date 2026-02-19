package com.remotedexter.agent

interface Block {
    val name: String
    val inputs: List<String>
    val outputs: List<String>
    val constraints: List<(AgentState) -> Boolean>
    val allowedTransitions: List<String>

    fun execute(state: AgentState): BlockResult
}
