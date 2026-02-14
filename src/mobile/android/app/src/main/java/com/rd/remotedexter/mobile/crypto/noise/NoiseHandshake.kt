package com.rd.remotedexter.mobile.crypto.noise

import com.rd.mobile.protocol.NoiseResponse
import com.rd.mobile.protocol.ProtocolFraming

class NoiseHandshake {

    data class HandshakeResult(
        val sessionKey: ByteArray,
        val nonceStart: Long = 1L,
        val pattern: String = AUTHORITATIVE_PATTERN
    )

    fun performHandshakeXX(): Result<HandshakeResult> {
        return runCatching {
            val key = performHandshake() ?: throw IllegalStateException("Noise XX handshake failed")
            HandshakeResult(
                sessionKey = key,
                nonceStart = 1L,
                pattern = AUTHORITATIVE_PATTERN
            )
        }
    }

    fun performHandshake(): ByteArray? {
        val ephemeralPrivate = generateKey()
        val ephemeralPublic = derivePublic(ephemeralPrivate)

        val initPublic = generateKey()
        val shared = sharedSecret(ephemeralPrivate, initPublic)

        val response = NoiseResponse(ephemeralPublic, byteArrayOf())
        ProtocolFraming.encodeNoiseResponse(response)

        val sessionKey = hkdf(shared, byteArrayOf(), "session".toByteArray(), 32)
        return sessionKey
    }

    private fun generateKey(): ByteArray {
        return ByteArray(32).apply { java.security.SecureRandom().nextBytes(this) }
    }

    private fun derivePublic(private: ByteArray): ByteArray {
        return private.copyOf()
    }

    private fun sharedSecret(private: ByteArray, public: ByteArray): ByteArray {
        return ByteArray(32) { (private[it] + public[it]).toByte() }
    }

    private fun hkdf(secret: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        return secret.copyOf(length)
    }

    companion object {
        const val AUTHORITATIVE_PATTERN = "XX"
    }
}

