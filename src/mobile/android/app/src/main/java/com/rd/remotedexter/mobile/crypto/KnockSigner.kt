package com.rd.remotedexter.mobile.crypto

import java.nio.ByteBuffer
import java.security.PrivateKey
import java.security.Signature

class KnockSigner {
    fun sign(privateKey: PrivateKey, relayNonce: ByteArray, timestampSec: Long, audience: String): ByteArray {
        val msg = buildMessage(relayNonce, timestampSec, audience)
        val sig = Signature.getInstance("Ed25519")
        sig.initSign(privateKey)
        sig.update(msg)
        return sig.sign()
    }

    private fun buildMessage(relayNonce: ByteArray, timestampSec: Long, audience: String): ByteArray {
        val audienceBytes = audience.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(relayNonce.size + 8 + audienceBytes.size)
        buffer.put(relayNonce)
        buffer.putLong(timestampSec)
        buffer.put(audienceBytes)
        return buffer.array()
    }
}

