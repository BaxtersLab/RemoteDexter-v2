package com.rd.remotedexter.mobile.transport

import com.rd.remotedexter.mobile.telemetry.MetricsRegistry

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
class WifiDirectService(private val context: Context, private val chaosTester: ChaosTransportTester? = null) : TransportService {

    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: Channel = wifiP2pManager.initialize(context, context.mainLooper, null)
    private var connected = false
    private var inputStream: BufferedInputStream? = null
    private var outputStream: BufferedOutputStream? = null
    private val writeQueue = LinkedBlockingQueue<ByteArray>()
    private var readThread: Thread? = null
    private var writeThread: Thread? = null
    private var watchdogThread: Thread? = null
    private var lastActivity = System.currentTimeMillis()
    private val watchdogTimeout = 30000L // 30 seconds
    private var messageHandler: ((ByteArray) -> Unit)? = null

    override fun isAvailable(): Boolean {
        // Check if Wi-Fi Direct is supported
        return context.packageManager.hasSystemFeature("android.hardware.wifi.direct")
    }

    override fun connect(messageHandler: ((ByteArray) -> Unit)?): Boolean {
        this.messageHandler = messageHandler
        if (!isAvailable()) {
            return false
        }

        // In a real implementation, this would:
        // 1. Discover peers
        // 2. Connect to peer
        // 3. Establish socket connection
        // 4. Wrap streams with buffered I/O

        connected = true
        println("Wi-Fi Direct transport connected")

        // Start read and write threads
        startReadThread()
        startWriteThread()
        startWatchdog()

        return true
    }

    override fun send(data: ByteArray): Boolean {
        if (!connected) return false

        // Apply chaos testing
        chaosTester?.maybeInjectJitter()
        val finalData = chaosTester?.maybeCorruptFrame(data) ?: data

        // Telemetry: record frame send
        MetricsRegistry.incrementFramesSent()
        MetricsRegistry.incrementBytesSent(finalData.size.toLong())

        return writeQueue.offer(finalData) // Non-blocking add to queue
    }

    override fun receive(): ByteArray? {
        // This method is now handled by read thread
        return null
    }

    override fun disconnect() {
        connected = false
        readThread?.interrupt()
        writeThread?.interrupt()
        watchdogThread?.interrupt()
        // Close streams
        inputStream?.close()
        outputStream?.close()
        println("Wi-Fi Direct transport disconnected")
    }

    private fun startReadThread() {
        readThread = thread(start = true) {
            while (connected && !Thread.interrupted()) {
                try {
                    inputStream?.let { stream ->
                        val message = ProtocolFraming.readFramedMessage(stream)
                        if (message != null) {
                            lastActivity = System.currentTimeMillis()

                            // Telemetry: record frame received
                            MetricsRegistry.incrementFramesReceived()
                            MetricsRegistry.incrementBytesReceived(message.size.toLong())

                            messageHandler?.invoke(message)
                            println("Wi-Fi Direct received ${message.size} bytes")
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    println("Wi-Fi Direct read error: ${e.message}")
                }
            }
        }
    }

    private fun startWriteThread() {
        writeThread = thread(start = true) {
            while (connected && !Thread.interrupted()) {
                try {
                    val data = writeQueue.take() // Blocking take
                    outputStream?.write(data)
                    outputStream?.flush()
                    lastActivity = System.currentTimeMillis()
                    println("Wi-Fi Direct sent ${data.size} bytes")
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    println("Wi-Fi Direct write error: ${e.message}")
                }
            }
        }
    }

    private fun startWatchdog() {
        watchdogThread = thread(start = true) {
            while (connected && !Thread.interrupted()) {
                Thread.sleep(5000) // Check every 5 seconds
                val now = System.currentTimeMillis()
                if (now - lastActivity > watchdogTimeout) {
                    // Telemetry: record watchdog timeout
                    MetricsRegistry.incrementWatchdogTimeouts()

                    println("Wi-Fi Direct watchdog: No activity for ${watchdogTimeout/1000}s, triggering reconnect")
                    // In real implementation, notify SessionController for reconnect
                }
            }
        }
    }
}

