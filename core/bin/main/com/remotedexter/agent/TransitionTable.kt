package com.remotedexter.agent

data class TransitionEntry(
    val allowedNextBlocks: List<String>,
    val fallback: String?,
    val recovery: String?,
    val interruptTransitions: Map<String, String> = emptyMap()
)

class TransitionTable(private val table: Map<String, TransitionEntry>) {
    fun get(blockName: String): TransitionEntry? = table[blockName]
}
