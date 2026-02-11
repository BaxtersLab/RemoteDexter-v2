package com.rd.remotedexter.mobile.transport

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

enum class TransportType {
    USB,
    WIFI_DIRECT,
    BLUETOOTH
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
object TransportSelector {

    fun selectTransport(context: Context): TransportService {
        // Prefer USB if available
        val usbService = UsbService(context)
        if (usbService.isAvailable()) {
            println("Transport selected: usb")
            return usbService
        }

        // Then Wi-Fi Direct
        val wifiService = WifiDirectService(context)
        if (wifiService.isAvailable()) {
            println("Transport selected: wifidirect")
            return wifiService
        }

        // Fall back to Bluetooth
        val bluetoothService = BluetoothService(context)
        println("Transport selected: bluetooth")
        return bluetoothService
    }
}

