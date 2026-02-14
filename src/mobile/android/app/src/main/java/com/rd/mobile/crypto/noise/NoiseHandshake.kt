package com.rd.mobile.crypto.noise

import com.rd.mobile.protocol.NoiseInit
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
        // Generate ephemeral keypair (simulate)
        val ephemeralPrivate = generateKey()
        val ephemeralPublic = derivePublic(ephemeralPrivate)

        // Receive NoiseInit (simulate)
        val initPublic = generateKey() // simulate received
        val shared = sharedSecret(ephemeralPrivate, initPublic)

        // Construct NoiseResponse
        val response = NoiseResponse(ephemeralPublic, byteArrayOf())
        ProtocolFraming.encodeNoiseResponse(response)

        println("NoiseInit received")
        println("NoiseResponse sent")

        // Derive session key
        val sessionKey = hkdf(shared, byteArrayOf(), "session".toByteArray(), 32)
        println("Session keys derived: ${sessionKey.sliceArray(0..3).joinToString("") { "%02x".format(it) }}")

        return sessionKey
    }

    private fun generateKey(): ByteArray {
        return ByteArray(32).apply { java.security.SecureRandom().nextBytes(this) }
    }

    private fun derivePublic(private: ByteArray): ByteArray {
        // Placeholder X25519
        return private.copyOf() // simulate
    }

    private fun sharedSecret(private: ByteArray, public: ByteArray): ByteArray {
        // Placeholder
        return ByteArray(32) { (private[it] + public[it]).toByte() }
    }

    private fun hkdf(secret: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        // Placeholder HKDF
        return secret.copyOf(length)
    }

    companion object {
        const val AUTHORITATIVE_PATTERN = "XX"
    }
}