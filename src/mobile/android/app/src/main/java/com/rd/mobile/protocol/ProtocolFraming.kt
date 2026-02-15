package com.rd.remotedexter.mobile.protocol

import com.rd.remotedexter.mobile.telemetry.MetricsRegistry

data class NoiseInit(val ephemeralPublicKey: ByteArray, val staticPublicKey: ByteArray, val payload: ByteArray)

data class NoiseResponse(val ephemeralPublicKey: ByteArray, val payload: ByteArray)

enum class MessageType {
    NOISE_INIT,
    NOISE_RESPONSE,
    COMMAND_REQUEST,
    COMMAND_RESPONSE,
    UNKNOWN
}

class ProtocolFraming {

    companion object {
        fun routeMessage(bytes: ByteArray): MessageType {
            return when {
                decodeNoiseInit(bytes) != null -> MessageType.NOISE_INIT
                decodeNoiseResponse(bytes) != null -> MessageType.NOISE_RESPONSE
                parseCommandRequest(bytes) != null -> MessageType.COMMAND_REQUEST
                parseCommandResponse(bytes) != null -> MessageType.COMMAND_RESPONSE
                else -> MessageType.UNKNOWN
            }
        }
        fun encodeNoiseInit(init: NoiseInit): ByteArray {
            val buf = mutableListOf<Byte>()
            buf.add(init.ephemeralPublicKey.size.toByte())
            buf.addAll(init.ephemeralPublicKey.asList())
            buf.add(init.staticPublicKey.size.toByte())
            buf.addAll(init.staticPublicKey.asList())
            buf.add(init.payload.size.toByte())
            buf.addAll(init.payload.asList())
            return frameMessage(buf.toByteArray())
        }

        fun decodeNoiseInit(data: ByteArray): NoiseInit? {
            if (data.size < 3) return null
            val ephemeralLen = data[0].toInt()
            if (data.size < 1 + ephemeralLen + 1) return null
            val ephemeral = data.sliceArray(1..ephemeralLen)
            val staticLen = data[1 + ephemeralLen].toInt()
            if (data.size < 1 + ephemeralLen + 1 + staticLen + 1) return null
            val static = data.sliceArray(1 + ephemeralLen + 1..1 + ephemeralLen + 1 + staticLen)
            val payloadLen = data[1 + ephemeralLen + 1 + staticLen].toInt()
            val payload = data.sliceArray(1 + ephemeralLen + 1 + staticLen + 1..data.size - 1)
            if (payload.size != payloadLen) return null
            return NoiseInit(ephemeral, static, payload)
        }

        fun encodeNoiseResponse(resp: NoiseResponse): ByteArray {
            val buf = mutableListOf<Byte>()
            buf.add(resp.ephemeralPublicKey.size.toByte())
            buf.addAll(resp.ephemeralPublicKey.asList())
            buf.add(resp.payload.size.toByte())
            buf.addAll(resp.payload.asList())
            return frameMessage(buf.toByteArray())
        }

        fun decodeNoiseResponse(data: ByteArray): NoiseResponse? {
            if (data.size < 2) return null
            val ephemeralLen = data[0].toInt()
            if (data.size < 1 + ephemeralLen + 1) return null
            val ephemeral = data.sliceArray(1..ephemeralLen)
            val payloadLen = data[1 + ephemeralLen].toInt()
            val payload = data.sliceArray(1 + ephemeralLen + 1..data.size - 1)
            if (payload.size != payloadLen) return null
            return NoiseResponse(ephemeral, payload)
        }

        fun frameCommandRequest(req: CommandRequest): ByteArray {
            // Telemetry: record command sent
            MetricsRegistry.incrementCommandsSent()
            return frameMessage(req.encode())
        }

        fun frameCommandResponse(resp: CommandResponse): ByteArray {
            // Telemetry: record response sent
            MetricsRegistry.incrementResponsesSent()
            return frameMessage(resp.encode())
        }

        fun parseCommandRequest(bytes: ByteArray): CommandRequest? {
            val payload = parseFrame(bytes) ?: run {
                println("ProtocolFraming: Failed to parse frame for command request")
                return null
            }
            return try {
                val request = CommandRequest.decode(payload)
                // Telemetry: record command received
                MetricsRegistry.incrementCommandsReceived()
                request
            } catch (e: IllegalArgumentException) {
                println("ProtocolFraming: Failed to decode command request: ${e.message}")
                null
            }
        }

        fun parseCommandResponse(bytes: ByteArray): CommandResponse? {
            val payload = parseFrame(bytes) ?: run {
                println("ProtocolFraming: Failed to parse frame for command response")
                return null
            }
            return try {
                val response = CommandResponse.decode(payload)
                // Telemetry: record response received
                MetricsRegistry.incrementResponsesReceived()
                response
            } catch (e: IllegalArgumentException) {
                println("ProtocolFraming: Failed to decode command response: ${e.message}")
                null
            }
        }

        private fun parseFrame(data: ByteArray): ByteArray? {
            if (data.size < 4) return null
            val length = ((data[0].toInt() and 0xFF) shl 24) or
                        ((data[1].toInt() and 0xFF) shl 16) or
                        ((data[2].toInt() and 0xFF) shl 8) or
                        (data[3].toInt() and 0xFF)
            if (data.size != 4 + length) return null
            return data.copyOfRange(4, data.size)
        }

        private fun frameMessage(data: ByteArray): ByteArray {
            val length = data.size
            val buf = ByteArray(4 + data.size)
            buf[0] = (length shr 24).toByte()
            buf[1] = (length shr 16).toByte()
            buf[2] = (length shr 8).toByte()
            buf[3] = length.toByte()
            data.copyInto(buf, 4)
            return buf
        }
    }
}