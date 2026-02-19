package com.remotedexter.agent

enum class LifelineStatus {
    ALIVE,
    RECOVERING,
    DEADEND,
    DEADEND_FORBIDDEN,
    ESCALATED,
    TERMINATED,
    INTERRUPTED;

    fun isDeadEnd(): Boolean = this == DEADEND || this == DEADEND_FORBIDDEN
}
