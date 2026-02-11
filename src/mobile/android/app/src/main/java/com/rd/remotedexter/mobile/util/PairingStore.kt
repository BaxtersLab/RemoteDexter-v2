package com.rd.mobile.util

import android.content.Context

class PairingStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("rd_pairing", Context.MODE_PRIVATE)

    fun getRelayUrl(): String? {
        return prefs.getString("relay_url", null)
    }

    fun setRelayUrl(url: String) {
        prefs.edit().putString("relay_url", url).apply()
    }
}

