package com.rd.remotedexter.mobile.protocol

data class NoiseInit(val ephemeralPublicKey: ByteArray, val staticPublicKey: ByteArray, val payload: ByteArray)

data class NoiseResponse(val ephemeralPublicKey: ByteArray, val payload: ByteArray)

class ProtocolFraming {

    companion object {
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