package com.rd.remotedexter.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rd.remotedexter.mobile.chaos.ChaosTransportTester
import com.rd.remotedexter.mobile.telemetry.StructuredLogger

/**
 * Developer Mode Activity
 * Provides access to advanced debugging and testing features
 */
class DeveloperModeActivity : AppCompatActivity() {

    private lateinit var chaosTester: ChaosTransportTester
    private lateinit var logger: StructuredLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer_mode)

        chaosTester = ChaosTransportTester()
        logger = StructuredLogger()

        setupViews()
    }

    private fun setupViews() {
        val chaosTestSwitch = findViewById<Switch>(R.id.chaosTestSwitch)
        val chaosTestButton = findViewById<Button>(R.id.chaosTestButton)
        val exportLogsButton = findViewById<Button>(R.id.exportLogsButton)
        val clearLogsButton = findViewById<Button>(R.id.clearLogsButton)
        val backButton = findViewById<Button>(R.id.backButton)
        val developerInfoText = findViewById<TextView>(R.id.developerInfoText)

        // Display developer info
        developerInfoText.text = """
            Developer Mode Active
            Build: ${com.remotedexter.mobile.BuildConfig.VERSION_NAME} (${com.remotedexter.mobile.BuildConfig.VERSION_CODE})
            Protocol: v1.0.0
            Transport: Multi-transport (USB/Bluetooth/Wi-Fi Direct)
            Telemetry: Enabled
        """.trimIndent()

        // Chaos testing controls
        chaosTestSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                chaosTester.startChaosTesting()
            } else {
                chaosTester.stopChaosTesting()
            }
        }

        chaosTestButton.setOnClickListener {
            chaosTester.runSingleChaosTest()
        }

        // Log management
        exportLogsButton.setOnClickListener {
            val logs = logger.exportLogs()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, logs)
            }
            startActivity(Intent.createChooser(intent, "Export Logs"))
        }

        clearLogsButton.setOnClickListener {
            logger.clearLogs()
        }

        backButton.setOnClickListener {
            finish()
        }
    }
}