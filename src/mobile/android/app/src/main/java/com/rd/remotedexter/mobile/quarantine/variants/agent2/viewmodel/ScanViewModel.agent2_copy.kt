package com.rd.remotedexter.mobile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.remotedexter.mobile.BuildConfig
import com.remotedexter.mobile.crypto.KeystoreManager
import com.remotedexter.mobile.crypto.KnockSigner
import com.remotedexter.mobile.network.KnockRequest
import com.remotedexter.mobile.network.RelayClient
import com.remotedexter.mobile.util.PairingStore
import com.remotedexter.mobile.util.TimeUtils
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.Executors

class ScanViewModel : ViewModel() {
    private val _status = MutableLiveData<String>()
    val status: LiveData<String> = _status

    private val executor = Executors.newSingleThreadExecutor()
    private val keystoreManager = KeystoreManager()
    private val signer = KnockSigner()
    private val audience = BuildConfig.KNOCK_AUDIENCE

    fun handleTag(tagUidHex: String, pairingStore: PairingStore) {
        val relayUrl = pairingStore.getRelayUrl()
        if (relayUrl.isNullOrBlank()) {
            _status.postValue("Not paired")
            return
        }
        val relayClient = RelayClient(relayUrl)

        _status.postValue("Sending knock...")
        executor.execute {
            try {
                val keyPair = keystoreManager.getOrCreateKeyPair()
                val pubBytes = keystoreManager.getPublicKeyBytes()
                val pubId = sha256Hex(pubBytes)

                val nonce = relayClient.getNonce()
                val timestamp = TimeUtils.nowUtcSeconds()
                val signature = signer.sign(keyPair.private, nonce, timestamp, audience)

                val req = KnockRequest(
                    signature = Base64.getEncoder().encodeToString(signature),
                    tagPublicKeyID = pubId,
                    timestamp = timestamp,
                    audience = audience,
                    metadata = mapOf("tag_uid" to tagUidHex)
                )

                val ok = relayClient.postKnock(req)
                if (ok) {
                    _status.postValue("Success")
                } else {
                    _status.postValue("Failure")
                }
            } catch (_: Exception) {
                _status.postValue("Failure")
            }
        }
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
