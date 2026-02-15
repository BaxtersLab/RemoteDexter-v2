package com.rd.remotedexter.mobile.protocol

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

private val UTF8 = StandardCharsets.UTF_8

data class CommandRequest(
    val type: String,
    val payload: ByteArray = byteArrayOf()
) {
    fun encode(): ByteArray {
        val typeBytes = type.toByteArray(UTF8)
        val typeLen = typeBytes.size.toByte()
        val payloadLen = payload.size.toByte()
        val baos = ByteArrayOutputStream()
        baos.write(typeLen.toInt())
        baos.write(typeBytes)
        baos.write(payloadLen.toInt())
        baos.write(payload)
        return baos.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): CommandRequest {
            if (bytes.size < 2) throw IllegalArgumentException("Data too short")
            val typeLen = bytes[0].toInt()
            if (bytes.size < 1 + typeLen + 1) throw IllegalArgumentException("Invalid data")
            val type = String(bytes, 1, typeLen, UTF8)
            val payloadLen = bytes[1 + typeLen].toInt()
            val payloadStart = 1 + typeLen + 1
            if (bytes.size < payloadStart + payloadLen) throw IllegalArgumentException("Payload length mismatch")
            val payload = bytes.copyOfRange(payloadStart, payloadStart + payloadLen)
            return CommandRequest(type, payload)
        }
    }
}

data class CommandResponse(
    val status: String,
    val payload: ByteArray = byteArrayOf()
) {
    fun encode(): ByteArray {
        val statusBytes = status.toByteArray(UTF8)
        val statusLen = statusBytes.size.toByte()
        val payloadLen = payload.size.toByte()
        val baos = ByteArrayOutputStream()
        baos.write(statusLen.toInt())
        baos.write(statusBytes)
        baos.write(payloadLen.toInt())
        baos.write(payload)
        return baos.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): CommandResponse {
            if (bytes.size < 2) throw IllegalArgumentException("Data too short")
            val statusLen = bytes[0].toInt()
            if (bytes.size < 1 + statusLen + 1) throw IllegalArgumentException("Invalid data")
            val status = String(bytes, 1, statusLen, UTF8)
            val payloadLen = bytes[1 + statusLen].toInt()
            val payloadStart = 1 + statusLen + 1
            if (bytes.size < payloadStart + payloadLen) throw IllegalArgumentException("Payload length mismatch")
            val payload = bytes.copyOfRange(payloadStart, payloadStart + payloadLen)
            return CommandResponse(status, payload)
        }
    }
}

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