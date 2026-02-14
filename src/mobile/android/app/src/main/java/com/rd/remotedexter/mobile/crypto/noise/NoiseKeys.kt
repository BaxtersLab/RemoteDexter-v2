package com.rd.remotedexter.mobile.crypto.noise

data class NoiseKeys(
    val controlSendKey: ByteArray,
    val controlRecvKey: ByteArray,
    val mediaSendKey: ByteArray,
    val mediaRecvKey: ByteArray
)

