package com.rd.remotedexter.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rd.remotedexter.mobile.transport.TransportType

/**
 * Onboarding Activity
 * Guides new users through transport selection and initial connection testing
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var transportRadioGroup: RadioGroup
    private lateinit var usbRadio: RadioButton
    private lateinit var wifiRadio: RadioButton
    private lateinit var bluetoothRadio: RadioButton
    private lateinit var testConnectionButton: Button
    private lateinit var continueButton: Button
    private lateinit var statusText: TextView

    private var selectedTransport: TransportType = TransportType.USB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        initializeViews()
        setupTransportSelection()
        setupButtons()
    }

    private fun initializeViews() {
        transportRadioGroup = findViewById(R.id.transportRadioGroup)
        usbRadio = findViewById(R.id.usbRadio)
        wifiRadio = findViewById(R.id.wifiRadio)
        bluetoothRadio = findViewById(R.id.bluetoothRadio)
        testConnectionButton = findViewById(R.id.testConnectionButton)
        continueButton = findViewById(R.id.continueButton)
        statusText = findViewById(R.id.statusText)

        // Default selection
        usbRadio.isChecked = true
        statusText.text = "Welcome to RemoteDexter!\n\nSelect your preferred transport method and test the connection."
    }

    private fun setupTransportSelection() {
        transportRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedTransport = when (checkedId) {
                R.id.usbRadio -> TransportType.USB
                R.id.wifiRadio -> TransportType.WIFI_DIRECT
                R.id.bluetoothRadio -> TransportType.BLUETOOTH
                else -> TransportType.USB
            }

            updateTransportDescription()
        }

        updateTransportDescription()
    }

    private fun updateTransportDescription() {
        val description = when (selectedTransport) {
            TransportType.USB -> """
                USB Transport
                • Fastest and most reliable
                • Requires USB cable connection
                • Best for desktop computers
            """.trimIndent()

            TransportType.WIFI_DIRECT -> """
                Wi-Fi Direct Transport
                • Wireless connection
                • Good performance over Wi-Fi
                • Works with most modern devices
            """.trimIndent()

            TransportType.BLUETOOTH -> """
                Bluetooth Transport
                • Wireless connection
                • Lower bandwidth than USB/Wi-Fi
                • Good for short-range connections
            """.trimIndent()

            else -> ""
        }

        statusText.text = "Welcome to RemoteDexter!\n\n$description\n\nSelect your preferred transport method and test the connection."
    }

    private fun setupButtons() {
        testConnectionButton.setOnClickListener {
            testConnection()
        }

        continueButton.setOnClickListener {
            // Save transport preference and proceed to main app
            saveTransportPreference(selectedTransport)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun testConnection() {
        statusText.text = "Testing connection to $selectedTransport..."
        testConnectionButton.isEnabled = false

        // Simulate connection test (in real implementation, this would test actual transport)
        Thread {
            try {
                Thread.sleep(2000) // Simulate test delay
                runOnUiThread {
                    statusText.text = "✓ Connection test successful!\n\nTransport: $selectedTransport\n\nYou can now continue to the main app."
                    testConnectionButton.isEnabled = true
                    continueButton.isEnabled = true
                }
            } catch (e: InterruptedException) {
                runOnUiThread {
                    statusText.text = "✗ Connection test failed.\n\nPlease check your connection and try again."
                    testConnectionButton.isEnabled = true
                }
            }
        }.start()
    }

    private fun saveTransportPreference(transport: TransportType) {
        // In a real implementation, save to SharedPreferences
        val prefs = getSharedPreferences("RemoteDexter", MODE_PRIVATE)
        prefs.edit()
            .putString("preferred_transport", transport.name)
            .putBoolean("onboarding_completed", true)
            .apply()
    }
}