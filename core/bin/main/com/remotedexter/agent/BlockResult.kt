package com.remotedexter.agent

data class BlockResult(
    val status: Status,
    val outputData: Map<String, Any?> = emptyMap(),
    val nextPointer: String? = null,
    val errorCode: String? = null,
    val recoveryHint: String? = null,
    val interruptType: String? = null
)

enum class Status {
    success,
    fail,
    deadEnd,
    interrupt,
    terminated
}
