package com.rd.remotedexter.mobile.crypto.noise

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object NoiseHKDF {
    private const val INFO = "RemoteDexter-Noise-Keys"

    fun deriveKeys(sharedSecret: ByteArray, transcriptHash: ByteArray): NoiseKeys {
        val prk = hkdfExtract(transcriptHash, sharedSecret)
        val okm = hkdfExpand(prk, INFO.toByteArray(Charsets.UTF_8), 32 * 4)
        val k1 = okm.copyOfRange(0, 32)
        val k2 = okm.copyOfRange(32, 64)
        val k3 = okm.copyOfRange(64, 96)
        val k4 = okm.copyOfRange(96, 128)
        return NoiseKeys(k1, k2, k3, k4)
    }

    fun sessionIdFromTranscript(transcriptHash: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256").digest(transcriptHash)
        return digest.copyOfRange(0, 16)
    }

    private fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        val result = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < length) {
            mac.reset()
            mac.update(t)
            mac.update(info)
            mac.update(counter.toByte())
            t = mac.doFinal()
            val toCopy = minOf(t.size, length - offset)
            System.arraycopy(t, 0, result, offset, toCopy)
            offset += toCopy
            counter += 1
        }
        return result
    }
}

