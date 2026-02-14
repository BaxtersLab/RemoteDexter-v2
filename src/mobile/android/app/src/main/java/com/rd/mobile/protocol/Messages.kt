package com.rd.mobile.protocol

data class CommandRequest(
    val type: String,
    val payload: ByteArray = byteArrayOf()
)

data class CommandResponse(
    val status: String,
    val payload: ByteArray = byteArrayOf()
)

object CommandValidatorShim {
    private val legacyAliases = mapOf(
        "begin_session" to "connect",
        "auth" to "authenticate",
        "open_transport" to "establish_transport",
        "close_session" to "teardown"
    )

    fun canonicalize(type: String): String {
        val normalized = type.trim().lowercase()
        return legacyAliases[normalized] ?: normalized
    }

    fun isKnown(type: String): Boolean {
        return canonicalize(type) in knownCommands
    }

    private val knownCommands = setOf(
        "ping",
        "device_info",
        "connect",
        "authenticate",
        "establish_transport",
        "teardown",
        "manual_reconnect",
        "start_streaming",
        "stop_streaming",
        "set_input_mode",
        "mouse_move",
        "mouse_click",
        "key_event",
        "scroll_event",
        "clipboard_sync",
        "truststore_update",
        "key_rotation"
    )
}