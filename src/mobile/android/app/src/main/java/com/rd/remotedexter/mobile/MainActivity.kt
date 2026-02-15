package com.rd.remotedexter.mobile

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.os.Handler
import android.os.Looper
import com.rd.remotedexter.mobile.ConnectionStatusView
import com.rd.remotedexter.mobile.protocol.CommandValidatorShim
import com.rd.remotedexter.mobile.protocol.CommandRequest
import com.rd.remotedexter.mobile.protocol.CommandResponse
import com.rd.mobile.render.RustDeskFrameBridge
import com.rd.mobile.render.SurfaceFramePipeline
import com.rd.mobile.session.AndroidSessionManager

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var connectionStatusView: ConnectionStatusView
    private lateinit var controllerToolbar: LinearLayout
    private lateinit var remoteSurface: SurfaceView
    private lateinit var copyButton: Button
    private lateinit var pasteButton: Button
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var framePipeline: SurfaceFramePipeline
    private lateinit var frameBridge: RustDeskFrameBridge
    private var screenCaptureService: ScreenCaptureService? = null
    private var inputInjector: InputInjector? = null
    private var inputMode = "mouse"
    private var sessionActive = false
    private var applyingRemoteClipboard = false
    private lateinit var sessionManager: AndroidSessionManager
    private lateinit var statusUpdateHandler: Handler
    private lateinit var statusUpdateRunnable: Runnable

    private val clipboardChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (!sessionActive || applyingRemoteClipboard) {
            return@OnPrimaryClipChangedListener
        }

        val clipText = clipboardManager.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(this)
            ?.toString()
            .orEmpty()
        if (clipText.isNotEmpty()) {
            sendClipboardSyncToRemote(clipText)
        }
    }

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if onboarding is needed
        val prefs = getSharedPreferences("RemoteDexter", MODE_PRIVATE)
        val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)

        if (!onboardingCompleted) {
            // Show onboarding first
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        statusText = findViewById(R.id.status_text)
        statusText.text = "RD Mobile Running"

        connectionStatusView = findViewById(R.id.connectionStatusView)
        connectionStatusView.setOnClickListener {
            val intent = Intent(this, DiagnosticsActivity::class.java)
            startActivity(intent)
        }

        controllerToolbar = findViewById(R.id.controller_toolbar)
        remoteSurface = findViewById(R.id.remote_surface)
        copyButton = findViewById(R.id.btn_copy)
        pasteButton = findViewById(R.id.btn_paste)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipboardChangedListener)
        setupGestureHandlers()
        framePipeline = SurfaceFramePipeline(remoteSurface.holder, fpsCap = 60)
        framePipeline.start()
        frameBridge = RustDeskFrameBridge { frame ->
            framePipeline.submitFrame(frame)
        }

        setSessionActive(false)
        sessionManager = AndroidSessionManager(this)

        statusUpdateHandler = Handler(Looper.getMainLooper())
        statusUpdateRunnable = Runnable {
            connectionStatusView.updateStatus()
            statusUpdateHandler.postDelayed(statusUpdateRunnable, 2000) // Update every 2 seconds
        }
        statusUpdateHandler.post(statusUpdateRunnable)

        copyButton.setOnClickListener {
            sendKeyCombo("Ctrl+C")
        }

        pasteButton.setOnClickListener {
            sendKeyCombo("Ctrl+V")
        }

        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        requestScreenCapture()
    }

    private fun requestScreenCapture() {
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            REQUEST_MEDIA_PROJECTION
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            screenCaptureService = ScreenCaptureService(this, mediaProjectionManager, data!!)
            runOnUiThread {
                statusText.text = "Screen Capture Ready"
            }
        } else {
            runOnUiThread {
                statusText.text = "Screen Capture Denied"
            }
        }
    }

    fun onBootstrapComplete() {
        runOnUiThread {
            statusText.text = "Bootstrap Complete"
        }
        println("UI: Bootstrap event received")
    }

    fun onHandshakeComplete() {
        runOnUiThread {
            statusText.text = "Handshake Complete"
            setSessionActive(true)
        }
        sessionManager.authenticateSession()
        sessionManager.establishTransport()
        println("UI: Handshake event")
    }

    fun handleCommand(cmd: CommandRequest): CommandResponse {
        val commandType = CommandValidatorShim.canonicalize(cmd.type)

        if (!CommandValidatorShim.isKnown(commandType)) {
            return CommandResponse("ok", "ignored_unknown_command".toByteArray())
        }

        runOnUiThread {
            statusText.text = "Command received: $commandType"
        }
        println("UI: Command event")
        return when (commandType) {
            "ping" -> CommandResponse("ok", "pong".toByteArray())
            "device_info" -> CommandResponse("ok", "Android Device".toByteArray())
            "connect" -> {
                val result = sessionManager.initiateSessionNegotiation()
                if (result.isSuccess) {
                    setSessionActive(true)
                    CommandResponse("ok", "connected".toByteArray())
                } else {
                    forceDisconnect("Negotiation failed")
                    CommandResponse("error", "negotiation_failed".toByteArray())
                }
            }
            "authenticate" -> {
                val result = sessionManager.authenticateSession()
                if (result.isSuccess) {
                    CommandResponse("ok", "authenticated".toByteArray())
                } else {
                    forceDisconnect("Authentication failed")
                    CommandResponse("error", "auth_failed".toByteArray())
                }
            }
            "establish_transport" -> {
                val result = sessionManager.establishTransport()
                if (result.isSuccess) {
                    CommandResponse("ok", "transport_established".toByteArray())
                } else {
                    forceDisconnect("Transport establishment failed")
                    CommandResponse("error", "transport_failed".toByteArray())
                }
            }
            "teardown" -> {
                sessionManager.teardownSession()
                setSessionActive(false)
                CommandResponse("ok", "session_terminated".toByteArray())
            }
            "manual_reconnect" -> {
                val result = sessionManager.manualReconnect()
                if (result.isSuccess) {
                    setSessionActive(true)
                    CommandResponse("ok", "manually_reconnected".toByteArray())
                } else {
                    setSessionActive(false)
                    CommandResponse("error", "manual_reconnect_failed".toByteArray())
                }
            }
            "start_streaming" -> {
                screenCaptureService?.startStreaming()
                framePipeline.start()
                runOnUiThread {
                    setSessionActive(true)
                }
                CommandResponse("ok", "streaming_started".toByteArray())
            }
            "stop_streaming" -> {
                screenCaptureService?.stopStreaming()
                framePipeline.stop()
                runOnUiThread {
                    setSessionActive(false)
                }
                CommandResponse("ok", "streaming_stopped".toByteArray())
            }
            "frame_decoded_rgba8888" -> {
                processEncryptedPayload(cmd.payload) { decrypted ->
                    ingestDecodedFrame(decrypted)
                    CommandResponse("ok", "frame_received".toByteArray())
                }
            }
            "set_input_mode" -> {
                inputMode = String(cmd.payload)
                runOnUiThread {
                    statusText.text = "Input mode: $inputMode"
                }
                CommandResponse("ok", "mode_set".toByteArray())
            }
            "mouse_move" -> {
                processEncryptedPayload(cmd.payload) { decrypted ->
                    handleMouseMove(decrypted)
                    CommandResponse("ok", byteArrayOf())
                }
            }
            "mouse_click" -> {
                processEncryptedPayload(cmd.payload) { decrypted ->
                    handleMouseClick(decrypted)
                    CommandResponse("ok", byteArrayOf())
                }
            }
            "key_event" -> {
                processEncryptedPayload(cmd.payload) { decrypted ->
                    handleKeyEvent(decrypted)
                    CommandResponse("ok", byteArrayOf())
                }
            }
            "clipboard_sync" -> {
                handleRemoteClipboardUpdate(String(cmd.payload))
                CommandResponse("ok", "clipboard_applied".toByteArray())
            }
            "scroll_event" -> {
                processEncryptedPayload(cmd.payload) { decrypted ->
                    handleScrollEvent(decrypted)
                    CommandResponse("ok", byteArrayOf())
                }
            }
            "truststore_update" -> {
                val parts = String(cmd.payload).split(":", limit = 2)
                if (parts.size != 2) {
                    return CommandResponse("error", "invalid_truststore_payload".toByteArray())
                }
                val result = sessionManager.rotateKeyFromBackend(parts[0], parts[1].toByteArray())
                if (result.isSuccess) CommandResponse("ok", "truststore_updated".toByteArray())
                else CommandResponse("error", "truststore_update_failed".toByteArray())
            }
            "key_rotation" -> {
                val result = sessionManager.rotateKeyFromBackend("active_session", cmd.payload)
                if (result.isSuccess) CommandResponse("ok", "key_rotated".toByteArray())
                else CommandResponse("error", "key_rotation_failed".toByteArray())
            }
            else -> CommandResponse("error", "unknown command".toByteArray())
        }.also {
            runOnUiThread {
                statusText.text = "Response sent: ${it.status}"
            }
        }
    }

    private fun handleMouseMove(encryptedPayload: ByteArray) {
        inputInjector?.injectMouseMove(10, 10)
    }

    private fun handleMouseClick(encryptedPayload: ByteArray) {
        inputInjector?.injectMouseClick(0, 0)
    }

    private fun handleKeyEvent(encryptedPayload: ByteArray) {
        inputInjector?.injectKeyEvent(0, 0)
    }

    private fun handleScrollEvent(encryptedPayload: ByteArray) {
        inputInjector?.injectScroll(1)
    }

    private fun sendKeyCombo(combo: String) {
        if (!sessionActive) {
            runOnUiThread {
                statusText.text = "Session inactive"
            }
            return
        }

        when (combo) {
            "Ctrl+C" -> {
                val scanCode = mapAndroidKeyCodeToScanCode(KeyEvent.KEYCODE_C)
                if (scanCode != null) {
                    inputInjector?.injectKeyEvent(scanCode, InputInjector.ACTION_DOWN)
                    inputInjector?.injectKeyEvent(scanCode, InputInjector.ACTION_UP)
                }
            }
            "Ctrl+V" -> {
                val scanCode = mapAndroidKeyCodeToScanCode(KeyEvent.KEYCODE_V)
                if (scanCode != null) {
                    inputInjector?.injectKeyEvent(scanCode, InputInjector.ACTION_DOWN)
                    inputInjector?.injectKeyEvent(scanCode, InputInjector.ACTION_UP)
                }
            }
        }

        runOnUiThread {
            statusText.text = "Sent key combo: $combo"
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!sessionActive) {
            return super.dispatchKeyEvent(event)
        }

        if (event.isCtrlPressed && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_C -> {
                    sendKeyCombo("Ctrl+C")
                    return true
                }
                KeyEvent.KEYCODE_V -> {
                    sendKeyCombo("Ctrl+V")
                    return true
                }
            }
        }

        val mappedAction = when (event.action) {
            KeyEvent.ACTION_DOWN -> InputInjector.ACTION_DOWN
            KeyEvent.ACTION_UP -> InputInjector.ACTION_UP
            else -> null
        }
        val scanCode = mapAndroidKeyCodeToScanCode(event.keyCode)

        if (mappedAction != null && scanCode != null) {
            inputInjector?.injectKeyEvent(scanCode, mappedAction)
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        setSessionActive(false)
        framePipeline.stop()
        clipboardManager.removePrimaryClipChangedListener(clipboardChangedListener)
        super.onDestroy()
    }

    private fun setSessionActive(active: Boolean) {
        sessionActive = active
        controllerToolbar.visibility = if (active) LinearLayout.VISIBLE else LinearLayout.GONE
    }

    private fun setupGestureHandlers() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!sessionActive) return false
                inputInjector?.injectMouseClick(InputInjector.BUTTON_LEFT, InputInjector.ACTION_DOWN)
                inputInjector?.injectMouseClick(InputInjector.BUTTON_LEFT, InputInjector.ACTION_UP)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (!sessionActive) return
                inputInjector?.injectMouseClick(InputInjector.BUTTON_RIGHT, InputInjector.ACTION_DOWN)
                inputInjector?.injectMouseClick(InputInjector.BUTTON_RIGHT, InputInjector.ACTION_UP)
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!sessionActive) return false
                inputInjector?.injectMouseMove((-distanceX).toInt(), (-distanceY).toInt())
                return true
            }
        })

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (!sessionActive) return false
                val amount = if (detector.scaleFactor >= 1f) 1 else -1
                inputInjector?.injectScroll(amount)
                return true
            }
        })

        remoteSurface.setOnTouchListener { _: View, event: MotionEvent ->
            if (!sessionActive) return@setOnTouchListener false

            scaleGestureDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    inputInjector?.injectTouch(event.x.toInt(), event.y.toInt(), InputInjector.ACTION_DOWN)
                }
                MotionEvent.ACTION_MOVE -> {
                    inputInjector?.injectTouch(event.x.toInt(), event.y.toInt(), InputInjector.ACTION_MOVE)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    inputInjector?.injectTouch(event.x.toInt(), event.y.toInt(), InputInjector.ACTION_UP)
                }
            }
            true
        }
    }

    private fun handleRemoteClipboardUpdate(text: String) {
        if (text.isBlank()) {
            return
        }

        applyingRemoteClipboard = true
        try {
            val clip = ClipData.newPlainText("remote", text)
            clipboardManager.setPrimaryClip(clip)
            runOnUiThread {
                statusText.text = "Clipboard synced from remote"
            }
        } finally {
            applyingRemoteClipboard = false
        }
    }

    private fun sendClipboardSyncToRemote(text: String) {
        val encrypted = sessionManager.buildEncryptedPayload(text.toByteArray())
        println("Clipboard sync outbound: ${encrypted.size} bytes")
    }

    private fun mapAndroidKeyCodeToScanCode(keyCode: Int): Int? {
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> 4
            KeyEvent.KEYCODE_B -> 5
            KeyEvent.KEYCODE_C -> 6
            KeyEvent.KEYCODE_D -> 7
            KeyEvent.KEYCODE_E -> 8
            KeyEvent.KEYCODE_F -> 9
            KeyEvent.KEYCODE_G -> 10
            KeyEvent.KEYCODE_H -> 11
            KeyEvent.KEYCODE_I -> 12
            KeyEvent.KEYCODE_J -> 13
            KeyEvent.KEYCODE_K -> 14
            KeyEvent.KEYCODE_L -> 15
            KeyEvent.KEYCODE_M -> 16
            KeyEvent.KEYCODE_N -> 17
            KeyEvent.KEYCODE_O -> 18
            KeyEvent.KEYCODE_P -> 19
            KeyEvent.KEYCODE_Q -> 20
            KeyEvent.KEYCODE_R -> 21
            KeyEvent.KEYCODE_S -> 22
            KeyEvent.KEYCODE_T -> 23
            KeyEvent.KEYCODE_U -> 24
            KeyEvent.KEYCODE_V -> 25
            KeyEvent.KEYCODE_W -> 26
            KeyEvent.KEYCODE_X -> 27
            KeyEvent.KEYCODE_Y -> 28
            KeyEvent.KEYCODE_Z -> 29
            KeyEvent.KEYCODE_1 -> 30
            KeyEvent.KEYCODE_2 -> 31
            KeyEvent.KEYCODE_3 -> 32
            KeyEvent.KEYCODE_4 -> 33
            KeyEvent.KEYCODE_5 -> 34
            KeyEvent.KEYCODE_6 -> 35
            KeyEvent.KEYCODE_7 -> 36
            KeyEvent.KEYCODE_8 -> 37
            KeyEvent.KEYCODE_9 -> 38
            KeyEvent.KEYCODE_0 -> 39
            KeyEvent.KEYCODE_ENTER -> 40
            KeyEvent.KEYCODE_ESCAPE -> 41
            KeyEvent.KEYCODE_DEL -> 42
            KeyEvent.KEYCODE_TAB -> 43
            KeyEvent.KEYCODE_SPACE -> 44
            KeyEvent.KEYCODE_DPAD_RIGHT -> 79
            KeyEvent.KEYCODE_DPAD_LEFT -> 80
            KeyEvent.KEYCODE_DPAD_DOWN -> 81
            KeyEvent.KEYCODE_DPAD_UP -> 82
            else -> null
        }
    }

    private fun processEncryptedPayload(
        payload: ByteArray,
        onSuccess: (ByteArray) -> CommandResponse
    ): CommandResponse {
        val decrypted = sessionManager.decryptPayload(payload)
        if (decrypted.isFailure) {
            forceDisconnect("AEAD decrypt failed")
            return CommandResponse("error", "aead_decrypt_failed".toByteArray())
        }
        return onSuccess(decrypted.getOrThrow())
    }

    private fun forceDisconnect(reason: String) {
        sessionManager.teardownSession()
        screenCaptureService?.stopStreaming()
        framePipeline.stop()
        setSessionActive(false)
        runOnUiThread {
            statusText.text = reason
        }
    }

    private fun ingestDecodedFrame(payload: ByteArray) {
        if (payload.size < 8) {
            return
        }

        val width =
            ((payload[0].toInt() and 0xFF) shl 24) or
                ((payload[1].toInt() and 0xFF) shl 16) or
                ((payload[2].toInt() and 0xFF) shl 8) or
                (payload[3].toInt() and 0xFF)
        val height =
            ((payload[4].toInt() and 0xFF) shl 24) or
                ((payload[5].toInt() and 0xFF) shl 16) or
                ((payload[6].toInt() and 0xFF) shl 8) or
                (payload[7].toInt() and 0xFF)

        if (width <= 0 || height <= 0) {
            return
        }

        val frameBytes = payload.copyOfRange(8, payload.size)
        val expected = width * height * 4
        if (frameBytes.size < expected) {
            return
        }

        frameBridge.submitDecodedFrame(width, height, frameBytes)
    }

    override fun onDestroy() {
        super.onDestroy()
        statusUpdateHandler.removeCallbacks(statusUpdateRunnable)
    }
}

