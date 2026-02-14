package com.rd.remotedexter.mobile.network

data class FramedMessage(
    val type: Int,
    val payload: ByteArray
)

interface MessageFramer {
    fun frame(type: Int, payload: ByteArray): ByteArray
    fun parse(frame: ByteArray): FramedMessage
}

class EmptyMessageFramer : MessageFramer {
    override fun frame(type: Int, payload: ByteArray): ByteArray {
        return ByteArray(0)
    }

    override fun parse(frame: ByteArray): FramedMessage {
        return FramedMessage(0, ByteArray(0))
    }
}

