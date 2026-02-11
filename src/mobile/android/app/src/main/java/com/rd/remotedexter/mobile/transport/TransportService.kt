package com.rd.remotedexter.mobile.transport

interface TransportService {
    fun isAvailable(): Boolean
    fun connect(): Boolean
    fun send(data: ByteArray): Boolean
    fun receive(): ByteArray?
    fun disconnect()
}