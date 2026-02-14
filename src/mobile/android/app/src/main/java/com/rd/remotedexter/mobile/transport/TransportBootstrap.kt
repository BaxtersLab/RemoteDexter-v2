package com.rd.remotedexter.mobile.transport

interface TransportBootstrap {
    fun initiate()
    fun send(payload: ByteArray)
    fun receive(): ByteArray
}

