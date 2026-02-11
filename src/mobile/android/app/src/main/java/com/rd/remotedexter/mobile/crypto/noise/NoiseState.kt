package com.rd.mobile.crypto.noise

data class NoiseState(
    val sessionId: ByteArray,
    val transcriptHash: ByteArray,
    val keys: NoiseKeys
)

