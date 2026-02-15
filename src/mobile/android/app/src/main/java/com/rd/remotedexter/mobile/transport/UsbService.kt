package com.rd.remotedexter.mobile.transport

import com.rd.remotedexter.mobile.telemetry.MetricsRegistry

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class UsbService(private val context: Context, private val chaosTester: ChaosTransportTester? = null) : TransportService {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
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
        val deviceList = usbManager.deviceList
        return deviceList.values.any { device ->
            // Check if device is an Android device (simplified check)
            device.vendorId == 0x18D1 || device.vendorId == 0x04E8 // Google/ADB or Samsung
        }
    }

    override fun connect(messageHandler: ((ByteArray) -> Unit)?): Boolean {
        this.messageHandler = messageHandler
        if (!isAvailable()) {
            return false
        }

        // In a real implementation, this would:
        // 1. Request USB permission
        // 2. Open USB device
        // 3. Establish communication channel
        // 4. Wrap streams with buffered I/O

        connected = true
        println("USB transport connected")

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
        println("USB transport disconnected")
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
                            println("USB received ${message.size} bytes")
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    println("USB read error: ${e.message}")
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
                    println("USB sent ${data.size} bytes")
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    println("USB write error: ${e.message}")
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

                    println("USB watchdog: No activity for ${watchdogTimeout/1000}s, triggering reconnect")
                    // In real implementation, notify SessionController for reconnect
                }
            }
        }
    }
}