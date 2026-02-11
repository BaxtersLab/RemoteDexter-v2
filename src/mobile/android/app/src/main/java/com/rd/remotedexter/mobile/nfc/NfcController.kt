package com.rd.mobile.nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

class NfcController(private val activity: AppCompatActivity) {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun enableForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        val intent = Intent(activity, activity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, null)
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }
}

