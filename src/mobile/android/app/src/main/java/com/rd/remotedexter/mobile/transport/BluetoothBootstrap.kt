package com.rd.remotedexter.mobile.transport

class BluetoothBootstrap : TransportBootstrap {
    private var buffer: ByteArray = ByteArray(0)

    override fun initiate() {
        buffer = ByteArray(0)
    }

    override fun send(payload: ByteArray) {
        buffer = payload
    }

    override fun receive(): ByteArray {
        return buffer
    }
}

