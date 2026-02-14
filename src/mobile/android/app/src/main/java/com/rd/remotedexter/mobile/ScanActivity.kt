package com.rd.remotedexter.mobile

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.remotedexter.mobile.nfc.NfcController
import com.remotedexter.mobile.nfc.NfcUtils
import com.remotedexter.mobile.transport.TransportSelector
import com.remotedexter.mobile.transport.TransportType
import com.remotedexter.mobile.util.PairingStore
import com.remotedexter.mobile.viewmodel.ScanViewModel

class ScanActivity : AppCompatActivity() {
    private val viewModel: ScanViewModel by viewModels()
    private lateinit var nfcController: NfcController
    private lateinit var statusText: TextView
    private var handledTag = false
    private val bypassNfc = BuildConfig.BYPASS_NFC
    private var selectedTransport: TransportType = TransportType.NFC
    private val pairingStore by lazy { PairingStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(R.layout.activity_scan)
        statusText = findViewById(R.id.status_text)
        val group = findViewById<RadioGroup>(R.id.transportGroup)
        group.setOnCheckedChangeListener { _, checkedId ->
            selectedTransport = when (checkedId) {
                R.id.optionNfc -> TransportType.NFC
                R.id.optionBt -> TransportType.BT
                R.id.optionWifi -> TransportType.WIFI
                else -> TransportType.NFC
            }
        }

        if (bypassNfc) {
            val relayUrl = pairingStore.getRelayUrl()
            if (relayUrl.isNullOrBlank()) {
                statusText.text = getString(R.string.status_not_paired)
                return
            }
            val transport = TransportSelector.select(selectedTransport)
            transport.initiate()
            return
        }

        nfcController = NfcController(this)

        viewModel.status.observe(this) { status ->
            statusText.text = status
            if (status == getString(R.string.status_success) || status == getString(R.string.status_failure)) {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (bypassNfc) {
            return
        }
        handledTag = false
        nfcController.enableForegroundDispatch()
    }

    override fun onPause() {
        nfcController.disableForegroundDispatch()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (handledTag) {
            return
        }
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            if (tag != null) {
                handledTag = true
                nfcController.disableForegroundDispatch()
                val uidHex = NfcUtils.getTagUidHex(tag)
                viewModel.handleTag(uidHex, pairingStore)
            }
        }
    }
}

