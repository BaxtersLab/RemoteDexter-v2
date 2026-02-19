package com.remotedexter.agent

import java.time.Instant

data class Interrupt(val type: String, val payload: Any?, val priority: Int, val timestamp: Long = Instant.now().toEpochMilli())

data class AgentState(
    var executionPointer: String?,
    val workingMemory: MutableMap<String, Any?> = mutableMapOf(),
    val constitutionalFlags: MutableMap<String, Boolean> = mutableMapOf(
        "noSlop" to true,
        "deterministicMode" to true,
        "boundedLoops" to true,
        "allowInterrupts" to true
    ),
    var lifelineStatus: LifelineStatus = LifelineStatus.ALIVE,
    var lastBlockResult: BlockResult? = null,
    val interruptQueue: MutableList<Interrupt> = mutableListOf(),
    // loop counters per block
    val loopCounters: MutableMap<String, Int> = mutableMapOf()
    ,
    // recovery attempts per recovery target to prevent infinite recovery loops
    val recoveryAttempts: MutableMap<String, Int> = mutableMapOf()
)
