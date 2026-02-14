package com.rd.remotedexter.mobile.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore

class KeystoreManager {
    private val alias = "remotedexter_ed25519"

    fun getOrCreateKeyPair(): KeyPair {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val existing = ks.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        if (existing != null) {
            return KeyPair(existing.certificate.publicKey, existing.privateKey)
        }

        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_ED25519,
            "AndroidKeyStore"
        )
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    fun getPublicKeyBytes(): ByteArray {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val entry = ks.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: getOrCreateKeyPair().let { return it.public.encoded }
        return entry.certificate.publicKey.encoded
    }
}

