package com.remotedexter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

class FirstRunActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var explanationText: TextView
    private lateinit var nextButton: Button
    private lateinit var pairingCodeText: TextView
    private lateinit var scanQrButton: Button

    private var currentStep = 0
    private val totalSteps = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create UI programmatically for FRE
        titleText = TextView(this).apply {
            textSize = 24f
            setPadding(32, 32, 32, 16)
            text = "Welcome to RemoteDexter"
        }

        explanationText = TextView(this).apply {
            textSize = 16f
            setPadding(32, 16, 32, 32)
            text = getStepExplanation(0)
        }

        nextButton = Button(this).apply {
            text = "Next"
            setOnClickListener { nextStep() }
            setPadding(32, 16, 32, 16)
        }

        pairingCodeText = TextView(this).apply {
            textSize = 18f
            setPadding(32, 16, 32, 16)
            text = ""
            visibility = android.view.View.GONE
        }

        scanQrButton = Button(this).apply {
            text = "Scan QR Code"
            setOnClickListener { startQrScanner() }
            setPadding(32, 16, 32, 16)
            visibility = android.view.View.GONE
        }

        // Layout
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(titleText)
            addView(explanationText)
            addView(pairingCodeText)
            addView(scanQrButton)
            addView(nextButton)
            setPadding(16, 16, 16, 16)
        }

        setContentView(layout)

        // Request necessary permissions
        requestPermissions()
    }

    private fun getStepExplanation(step: Int): String {
        return when (step) {
            0 -> """
                Welcome to RemoteDexter!

                RemoteDexter gives you complete control over your remote desktop experience with true digital sovereignty.

                Key Principles:
                • No cloud dependency - direct device-to-device
                • End-to-end encryption with Noise protocol
                • User-controlled file transfers
                • Open source and auditable
            """.trimIndent()

            1 -> """
                Permission Setup

                RemoteDexter needs some permissions to work properly:

                • Camera: For QR code scanning during pairing
                • Storage: For receiving files from desktop
                • Location: For Wi-Fi Direct discovery (optional)

                These permissions are only used when needed and never shared.
            """.trimIndent()

            2 -> """
                Device Pairing

                To connect with your desktop, you need to pair this device.

                Your pairing code is:
                RD-4829-1732

                On your desktop, enter this code or scan the QR code.
            """.trimIndent()

            3 -> """
                Setup Complete!

                RemoteDexter is now ready to use.

                • Start sessions from your desktop
                • Approve file transfers
                • Manage trusted devices in settings

                Tap 'Finish' to start using RemoteDexter!
            """.trimIndent()

            else -> ""
        }
    }

    private fun nextStep() {
        currentStep++

        if (currentStep >= totalSteps) {
            // FRE complete, start main activity
            completeFirstRun()
            return
        }

        updateUIForStep()
    }

    private fun updateUIForStep() {
        titleText.text = getStepTitle(currentStep)
        explanationText.text = getStepExplanation(currentStep)

        when (currentStep) {
            2 -> {
                // Show pairing step UI
                pairingCodeText.text = "RD-4829-1732"
                pairingCodeText.visibility = android.view.View.VISIBLE
                scanQrButton.visibility = android.view.View.VISIBLE
                nextButton.text = "I've Paired My Device"
            }
            3 -> {
                // Hide pairing UI
                pairingCodeText.visibility = android.view.View.GONE
                scanQrButton.visibility = android.view.View.GONE
                nextButton.text = "Finish Setup"
            }
            else -> {
                pairingCodeText.visibility = android.view.View.GONE
                scanQrButton.visibility = android.view.View.GONE
                nextButton.text = "Next"
            }
        }
    }

    private fun getStepTitle(step: Int): String {
        return when (step) {
            0 -> "Welcome to RemoteDexter"
            1 -> "Permissions"
            2 -> "Device Pairing"
            3 -> "Setup Complete"
            else -> ""
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, 100)
        }
    }

    private fun startQrScanner() {
        // In a real implementation, start QR scanner activity
        Toast.makeText(this, "QR Scanner would open here", Toast.LENGTH_SHORT).show()
    }

    private fun completeFirstRun() {
        // Save that FRE is complete
        val prefs = getSharedPreferences("remotedexter", MODE_PRIVATE)
        prefs.edit().putBoolean("first_run_complete", true).apply()

        // Start main activity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                Toast.makeText(this, "Some permissions were denied. RemoteDexter may not work properly.", Toast.LENGTH_LONG).show()
            }
        }
    }
}