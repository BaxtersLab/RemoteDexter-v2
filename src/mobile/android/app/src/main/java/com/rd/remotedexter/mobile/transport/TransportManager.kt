package com.rd.remotedexter.mobile.transport

import com.rd.remotedexter.mobile.telemetry.MetricsRegistry

enum class TransportType {
    BLUETOOTH, USB, WIFI_DIRECT
}

interface TransportManager {
    fun setActiveTransport(type: TransportType)
    fun connect(): Boolean
    fun disconnect()
    fun sendCommand(req: CommandRequest, key: ByteArray, nonce: Long): CommandResponse
    fun sendResponse(resp: CommandResponse): Boolean
    fun receiveLoop(onMessage: (ByteArray) -> Unit)
    fun getAvailableTransports(): List<TransportType>
}

class AndroidTransportManager(private val context: Context, private val chaosTester: ChaosTransportTester? = null) : TransportManager {

    private var activeTransport: TransportService? = null
    private var activeType: TransportType? = null
    private var messageHandler: ((ByteArray) -> Unit)? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3
    private val reconnectDelayBase = 1000L // 1 second base delay

    private val bluetoothService = BluetoothService(context, chaosTester)
    private val usbService = UsbService(context, chaosTester)
    private val wifiDirectService = WifiDirectService(context, chaosTester)

    // Backpressure handling
    private val commandQueue = LinkedBlockingQueue<Pair<CommandRequest, ByteArray>>(10) // Max 10 pending commands
    private val responseQueue = LinkedBlockingQueue<CommandResponse>(50) // Max 50 pending responses
    private var commandProcessorThread: Thread? = null

    override fun setActiveTransport(type: TransportType) {
        activeTransport = when (type) {
            TransportType.BLUETOOTH -> bluetoothService
            TransportType.USB -> usbService
            TransportType.WIFI_DIRECT -> wifiDirectService
        }
        activeType = type
        println("TransportManager: Set active transport to $type")
    }

    override fun connect(): Boolean {
        val success = activeTransport?.connect { message -> handleIncomingMessage(message) } ?: false
        if (success) {
            reconnectAttempts = 0
            startCommandProcessor()
        }
        return success
    }

    override fun disconnect() {
        commandProcessorThread?.interrupt()
        activeTransport?.disconnect()
        activeTransport = null
        activeType = null
        reconnectAttempts = 0
    }

    override fun sendCommand(req: CommandRequest, key: ByteArray, nonce: Long): CommandResponse {
        // Add to command queue with backpressure
        if (!commandQueue.offer(req to key)) {
            // Queue full - drop oldest command if it's not critical
            commandQueue.poll() // Drop oldest
            if (!commandQueue.offer(req to key)) {
                return CommandResponse("error", "queue full".toByteArray())
            }
        }
        // Commands are processed asynchronously by commandProcessorThread
        // For now, return a placeholder - real implementation would wait for response
        return CommandResponse("pending", "processing".toByteArray())
    }

    override fun sendResponse(resp: CommandResponse): Boolean {
        // Add to response queue with backpressure
        if (!responseQueue.offer(resp)) {
            // Queue full - for responses, we prioritize newer ones
            responseQueue.poll() // Drop oldest response
            return responseQueue.offer(resp)
        }
        return true
    }

    override fun receiveLoop(onMessage: (ByteArray) -> Unit) {
        messageHandler = onMessage
        // The actual receiving is now handled by transport threads
        // This method is kept for interface compatibility
    }

    override fun getAvailableTransports(): List<TransportType> {
        val available = mutableListOf<TransportType>()
        if (bluetoothService.isAvailable()) available.add(TransportType.BLUETOOTH)
        if (usbService.isAvailable()) available.add(TransportType.USB)
        if (wifiDirectService.isAvailable()) available.add(TransportType.WIFI_DIRECT)

        // Sort by priority: USB > Wi-Fi Direct > Bluetooth
        return available.sortedBy { type ->
            when (type) {
                TransportType.USB -> 0
                TransportType.WIFI_DIRECT -> 1
                TransportType.BLUETOOTH -> 2
            }
        }
    }

    private fun startCommandProcessor() {
        commandProcessorThread = thread(start = true) {
            while (activeTransport != null && !Thread.interrupted()) {
                try {
                    // Process commands
                    val (req, key) = commandQueue.take()
                    val framedRequest = ProtocolFraming.frameCommandRequest(req, key)
                    activeTransport?.send(framedRequest)

                    // Process responses
                    val resp = responseQueue.poll() // Non-blocking
                    resp?.let {
                        val framedResponse = ProtocolFraming.frameCommandResponse(it)
                        activeTransport?.send(framedResponse)
                    }

                    Thread.sleep(10) // Small delay to prevent busy loop
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    println("Command processor error: ${e.message}")
                    handleTransportError()
                }
            }
        }
    }

    private fun handleTransportError() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            val delay = reconnectDelayBase * (1L shl (reconnectAttempts - 1)) // Exponential backoff
            println("Transport error, attempting reconnect $reconnectAttempts/$maxReconnectAttempts in ${delay}ms")
            Thread.sleep(delay)
            connect() // Retry connection
        } else {
            println("Max reconnect attempts reached, giving up")
            disconnect()
        }
    }

    // Streaming session tracking
    private var currentStreamingSessionStart: Long? = null

    fun startStreamingSession(): Long {
        currentStreamingSessionStart = MetricsRegistry.startStreamingSession()
        return currentStreamingSessionStart!!
    }

    fun endStreamingSession() {
        currentStreamingSessionStart?.let { startTime ->
            MetricsRegistry.endStreamingSession(startTime)
            currentStreamingSessionStart = null
        }
    }

    fun recordFrameEncoded(encodeTimeMs: Long) {
        MetricsRegistry.incrementFramesEncoded()
        MetricsRegistry.recordFrameEncodeTime(encodeTimeMs)
    }

    fun recordFrameDropped() {
        MetricsRegistry.incrementFramesDropped()
    }

    // RTT measurement
    fun sendPingWithTimestamp(): Boolean {
        val timestamp = System.currentTimeMillis()
        val pingPayload = timestamp.toString().toByteArray()
        val request = CommandRequest("ping", pingPayload)
        return sendCommand(request, ByteArray(32), 0L).status != "error"
    }

    fun recordPingResponse(response: CommandResponse) {
        if (response.status == "pong" && response.payload.isNotEmpty()) {
            try {
                val sentTimestamp = String(response.payload).toLong()
                val rtt = System.currentTimeMillis() - sentTimestamp
                MetricsRegistry.recordRtt(rtt)
            } catch (e: Exception) {
                // Invalid timestamp in response
            }
        }
    }
}