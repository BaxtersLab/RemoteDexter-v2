package com.remotedexter.agent

import java.util.logging.Logger

interface Telemetry {
    fun onTransition(from: String?, to: String)
    fun onRecoveryAttempt(target: String, decision: String, attempt: Int)
    fun onEnd(finalState: Map<String, Any?>)
}

class NoopTelemetry : Telemetry {
    override fun onTransition(from: String?, to: String) {}
    override fun onRecoveryAttempt(target: String, decision: String, attempt: Int) {}
    override fun onEnd(finalState: Map<String, Any?>) {}
}

class LoggingTelemetry : Telemetry {
    private val log = Logger.getLogger("RemoteDexter.Telemetry")
    override fun onTransition(from: String?, to: String) {
        log.info("transition: $from -> $to")
    }

    override fun onRecoveryAttempt(target: String, decision: String, attempt: Int) {
        log.info("recovery: target=$target decision=$decision attempt=$attempt")
    }

    override fun onEnd(finalState: Map<String, Any?>) {
        log.info("END emitted: finalState=$finalState")
    }
}
