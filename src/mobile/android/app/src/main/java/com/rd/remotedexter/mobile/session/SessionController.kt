package com.rd.remotedexter.mobile.session

import android.content.Context
import android.util.Log
import com.rd.remotedexter.mobile.crypto.NoiseHandshake
import com.rd.remotedexter.mobile.network.TransportManager
import com.rd.remotedexter.mobile.transport.ScreenCaptureService
import com.rd.remotedexter.mobile.transport.VideoEncoder
import com.rd.remotedexter.mobile.transport.InputInjector
import com.rd.remotedexter.mobile.shared.protocol.CommandRequest
import com.rd.remotedexter.mobile.shared.protocol.CommandResponse
import com.rd.remotedexter.mobile.shared.protocol.SessionEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * SessionController manages the lifecycle of remote desktop sessions on Android
 */
class SessionController(private val context: Context) {

    companion object {
        private const val TAG = "SessionController"
    }

    // Session state
    private val _sessionState = MutableStateFlow(SessionState.DISCONNECTED)
    val sessionState: StateFlow<SessionState> = _sessionState

    // Components
    private var transportManager: TransportManager? = null
    private var screenCaptureService: ScreenCaptureService? = null
    private var videoEncoder: VideoEncoder? = null
    private var inputInjector: InputInjector? = null
    private var noiseHandshake: NoiseHandshake? = null

    // Session data
    private var sessionKey: ByteArray? = null
    private var nonce: Long = 1
    private var sessionJob: Job? = null

    // Error tracking
    private var errorCount = 0
    private val maxErrors = 3

    enum class SessionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        STREAMING,
        ERROR
    }

    /**
     * Starts a remote desktop session
     */
    suspend fun startSession(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting remote session...")

            _sessionState.value = SessionState.CONNECTING

            // Initialize components
            transportManager = TransportManager(context)
            screenCaptureService = ScreenCaptureService(context)
            videoEncoder = VideoEncoder()
            inputInjector = InputInjector(context)
            noiseHandshake = NoiseHandshake()

            // Step 1: Establish transport connection
            val transportResult = transportManager?.establishConnection()
            if (transportResult?.isFailure == true) {
                throw Exception("Transport connection failed: ${transportResult.exceptionOrNull()?.message}")
            }

            // Step 2: Perform Noise handshake
            val handshakeResult = noiseHandshake?.performHandshake()
            if (handshakeResult?.isFailure == true) {
                throw Exception("Noise handshake failed: ${handshakeResult.exceptionOrNull()?.message}")
            }

            sessionKey = handshakeResult?.getOrNull()?.sessionKey
            nonce = handshakeResult.getOrNull()?.nonce ?: 1

            _sessionState.value = SessionState.CONNECTED
            Log.i(TAG, "Transport and crypto established")

            // Step 3: Start screen capture and streaming
            val streamingResult = startStreaming()
            if (streamingResult.isFailure) {
                throw Exception("Streaming failed: ${streamingResult.exceptionOrNull()?.message}")
            }

            _sessionState.value = SessionState.STREAMING

            // Step 4: Enable input injection
            val inputResult = enableInputControl()
            if (inputResult.isFailure) {
                Log.w(TAG, "Input control failed, but continuing with streaming: ${inputResult.exceptionOrNull()?.message}")
            }

            // Start session monitoring
            startSessionMonitoring()

            Log.i(TAG, "Remote session started successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start session", e)
            _sessionState.value = SessionState.ERROR
            errorCount++
            cleanup()
            Result.failure(e)
        }
    }

    /**
     * Stops the remote desktop session
     */
    suspend fun stopSession(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Stopping remote session...")

            sessionJob?.cancel()
            sessionJob = null

            cleanup()

            _sessionState.value = SessionState.DISCONNECTED
            errorCount = 0

            Log.i(TAG, "Remote session stopped")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping session", e)
            Result.failure(e)
        }
    }

    /**
     * Starts screen capture and video streaming
     */
    private suspend fun startStreaming(): Result<Unit> {
        return try {
            screenCaptureService?.let { capture ->
                videoEncoder?.let { encoder ->
                    // Start screen capture
                    val captureResult = capture.startCapture()
                    if (captureResult.isFailure) {
                        return Result.failure(Exception("Screen capture failed: ${captureResult.exceptionOrNull()?.message}"))
                    }

                    // Configure encoder with session keys
                    sessionKey?.let { key ->
                        encoder.setSessionKeys(key, nonce)
                    }

                    // Start encoding
                    encoder.startEncoding()

                    Log.i(TAG, "Screen streaming started")
                    Result.success(Unit)
                } ?: Result.failure(Exception("Video encoder not initialized"))
            } ?: Result.failure(Exception("Screen capture service not initialized"))

        } catch (e: Exception) {
            Log.e(TAG, "Error starting streaming", e)
            Result.failure(e)
        }
    }

    /**
     * Enables input control (touch/mouse injection)
     */
    private suspend fun enableInputControl(): Result<Unit> {
        return try {
            inputInjector?.let { injector ->
                sessionKey?.let { key ->
                    injector.setSessionKeys(key, nonce)
                    val result = injector.startInjection()
                    if (result.isSuccess) {
                        Log.i(TAG, "Input control enabled")
                    }
                    result
                } ?: Result.failure(Exception("Session key not available"))
            } ?: Result.failure(Exception("Input injector not initialized"))

        } catch (e: Exception) {
            Log.e(TAG, "Error enabling input control", e)
            Result.failure(e)
        }
    }

    /**
     * Starts session health monitoring
     */
    private fun startSessionMonitoring() {
        sessionJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(5000) // Check every 5 seconds

                try {
                    // Check transport health
                    transportManager?.let { transport ->
                        if (!transport.isHealthy()) {
                            Log.w(TAG, "Transport unhealthy, attempting recovery")
                            handleTransportFailure("Transport connection lost")
                        }
                    }

                    // Check streaming health
                    screenCaptureService?.let { capture ->
                        if (!capture.isCapturing()) {
                            Log.w(TAG, "Screen capture stopped, restarting")
                            handleStreamingFailure("Screen capture stopped")
                        }
                    }

                    // Reset error count on successful checks
                    if (errorCount > 0) {
                        errorCount--
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error during session monitoring", e)
                    errorCount++
                }

                // If too many errors, stop session
                if (errorCount >= maxErrors) {
                    Log.e(TAG, "Too many session errors, stopping session")
                    stopSession()
                    break
                }
            }
        }
    }

    /**
     * Handles transport failures with recovery attempts
     */
    private suspend fun handleTransportFailure(reason: String) {
        Log.w(TAG, "Transport failure: $reason")

        // Try to reconnect transport
        transportManager?.let { transport ->
            val reconnectResult = transport.reconnect()
            if (reconnectResult.isFailure) {
                Log.e(TAG, "Transport reconnection failed")
                errorCount++
            } else {
                Log.i(TAG, "Transport reconnected successfully")
                errorCount = 0
            }
        }
    }

    /**
     * Handles streaming failures with recovery attempts
     */
    private suspend fun handleStreamingFailure(reason: String) {
        Log.w(TAG, "Streaming failure: $reason")

        // Try to restart streaming
        val restartResult = startStreaming()
        if (restartResult.isFailure) {
            Log.e(TAG, "Streaming restart failed")
            errorCount++
        } else {
            Log.i(TAG, "Streaming restarted successfully")
            errorCount = 0
        }
    }

    /**
     * Sends a command through the session
     */
    suspend fun sendCommand(command: CommandRequest): Result<CommandResponse> {
        return withContext(Dispatchers.IO) {
            try {
                transportManager?.let { transport ->
                    sessionKey?.let { key ->
                        val response = transport.sendCommand(command, key, nonce)
                        nonce++ // Increment nonce after each command
                        Result.success(response)
                    } ?: Result.failure(Exception("Session key not available"))
                } ?: Result.failure(Exception("Transport manager not initialized"))

            } catch (e: Exception) {
                Log.e(TAG, "Error sending command", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Cleans up session resources
     */
    private fun cleanup() {
        try {
            inputInjector?.stopInjection()
            videoEncoder?.stopEncoding()
            screenCaptureService?.stopCapture()
            transportManager?.disconnect()
            noiseHandshake?.cleanup()

            inputInjector = null
            videoEncoder = null
            screenCaptureService = null
            transportManager = null
            noiseHandshake = null
            sessionKey = null
            nonce = 1

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    /**
     * Checks if the session is healthy
     */
    fun isHealthy(): Boolean {
        return _sessionState.value != SessionState.ERROR && errorCount < maxErrors
    }

    /**
     * Gets current session state
     */
    fun getCurrentState(): SessionState {
        return _sessionState.value
    }
}