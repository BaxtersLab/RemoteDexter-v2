package com.rd.remotedexter.mobile.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.rd.mobile.crypto.noise.NoiseHandshake
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AndroidSessionManager(context: Context) {
    enum class LifecycleState {
        IDLE,
        CONNECTED,
        AUTHENTICATED,
        TRANSPORT_ESTABLISHED,
        TERMINATED
    }

    enum class ReconnectPolicy {
        MANUAL
    }

    data class SessionSnapshot(
        val state: LifecycleState,
        val reconnectPolicy: ReconnectPolicy,
        val noisePattern: String
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences("rd_session_security", Context.MODE_PRIVATE)

    private var state: LifecycleState = LifecycleState.IDLE
    private var sessionKey: ByteArray? = null
    private var nonceCounter: Long = 1L
    private val secureRandom = SecureRandom()

    fun initiateSessionNegotiation(): Result<Unit> {
        return runCatching {
            val handshake = NoiseHandshake()
            val negotiated = handshake.performHandshakeXX().getOrThrow()
            sessionKey = negotiated.sessionKey
            nonceCounter = negotiated.nonceStart
            state = LifecycleState.CONNECTED
            persistTruststoreKey("active_session", negotiated.sessionKey)
        }
    }

    fun authenticateSession(): Result<Unit> {
        return runCatching {
            require(state == LifecycleState.CONNECTED) { "Session must be connected before authentication" }
            state = LifecycleState.AUTHENTICATED
        }
    }

    fun establishTransport(): Result<Unit> {
        return runCatching {
            require(state == LifecycleState.AUTHENTICATED) { "Authenticate before transport establishment" }
            state = LifecycleState.TRANSPORT_ESTABLISHED
        }
    }

    fun teardownSession() {
        state = LifecycleState.TERMINATED
        sessionKey = null
        nonceCounter = 1L
    }

    fun manualReconnect(): Result<Unit> {
        return runCatching {
            teardownSession()
            initiateSessionNegotiation().getOrThrow()
            authenticateSession().getOrThrow()
            establishTransport().getOrThrow()
        }
    }

    fun decryptPayload(payload: ByteArray): Result<ByteArray> {
        return runCatching {
            if (!payload.startsWith(ENC_PREFIX)) {
                return@runCatching payload
            }

            val encoded = payload.copyOfRange(ENC_PREFIX.size, payload.size)
            val decoded = Base64.decode(encoded, Base64.DEFAULT)
            require(decoded.size > IV_SIZE) { "Encrypted payload too short" }
            val iv = decoded.copyOfRange(0, IV_SIZE)
            val cipherText = decoded.copyOfRange(IV_SIZE, decoded.size)

            val key = sessionKey ?: loadPersistedTruststoreKey("active_session")
            require(key != null) { "No truststore session key available" }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
            nonceCounter++
            cipher.doFinal(cipherText)
        }
    }

    fun rotateKeyFromBackend(deviceId: String, keyMaterial: ByteArray): Result<Unit> {
        return runCatching {
            persistTruststoreKey(deviceId, keyMaterial)
            if (deviceId == "active_session") {
                sessionKey = keyMaterial
            }
        }
    }

    fun getSnapshot(): SessionSnapshot {
        return SessionSnapshot(
            state = state,
            reconnectPolicy = ReconnectPolicy.MANUAL,
            noisePattern = NoiseHandshake.AUTHORITATIVE_PATTERN
        )
    }

    private fun persistTruststoreKey(deviceId: String, keyMaterial: ByteArray) {
        prefs.edit()
            .putString("truststore_${deviceId}", Base64.encodeToString(keyMaterial, Base64.NO_WRAP))
            .apply()
    }

    private fun loadPersistedTruststoreKey(deviceId: String): ByteArray? {
        val encoded = prefs.getString("truststore_${deviceId}", null) ?: return null
        return Base64.decode(encoded, Base64.DEFAULT)
    }

    fun buildEncryptedPayload(plainText: ByteArray): ByteArray {
        val key = sessionKey ?: return plainText
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(plainText)
        val combined = ByteArray(iv.size + cipherText.size)
        iv.copyInto(combined, 0)
        cipherText.copyInto(combined, iv.size)
        val encoded = Base64.encode(combined, Base64.NO_WRAP)
        return ENC_PREFIX + encoded
    }

    companion object {
        private val ENC_PREFIX = "enc:".toByteArray()
        private const val IV_SIZE = 12
        private const val GCM_TAG_BITS = 128
    }
}