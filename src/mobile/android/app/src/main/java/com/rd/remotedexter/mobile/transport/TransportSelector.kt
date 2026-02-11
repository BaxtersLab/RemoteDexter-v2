package com.rd.mobile.transport

enum class TransportType {
    NFC,
    BT,
    WIFI
}

object TransportSelector {
    fun select(type: TransportType): TransportBootstrap {
        return when (type) {
            TransportType.NFC -> NfcBootstrap()
            TransportType.BT -> BluetoothBootstrap()
            TransportType.WIFI -> WifiDirectBootstrap()
        }
    }
}

