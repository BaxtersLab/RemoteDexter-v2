package com.rd.remotedexter.mobile.transport

import com.rd.remotedexter.mobile.telemetry.MetricsRegistry
import com.rd.remotedexter.mobile.telemetry.StructuredLogger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

class BluetoothService(private val context: Context, private val chaosTester: ChaosTransportTester? = null) : TransportService {

    private val logger = StructuredLogger(context)

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
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
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled &&
               bluetoothAdapter.bondedDevices.isNotEmpty()
    }

    override fun connect(messageHandler: ((ByteArray) -> Unit)?): Boolean {
        this.messageHandler = messageHandler
        if (!isAvailable()) {
            return false
        }

        // In a real implementation, this would:
        // 1. Get paired devices
        // 2. Create RFCOMM socket
        // 3. Connect to device
        // 4. Wrap streams with buffered I/O

        connected = true
        logger.logTransportConnected("bluetooth")
        println("Bluetooth transport connected")

        // Start read and write threads
        startReadThread()
        startWriteThread()
        startWatchdog()

        return true
    }

    override fun disconnect() {
        connected = false
        readThread?.interrupt()
        writeThread?.interrupt()
        watchdogThread?.interrupt()
        // Close streams
        inputStream?.close()
        outputStream?.close()
        logger.logTransportDisconnected("bluetooth")
        println("Bluetooth transport disconnected")
    }

    override fun send(data: ByteArray): Boolean {
        if (!connected) return false

        // Apply chaos testing
        chaosTester?.maybeInjectJitter()
        val finalData = chaosTester?.maybeCorruptFrame(data) ?: data

        // Telemetry: record frame send
        MetricsRegistry.incrementFramesSent()
        MetricsRegistry.incrementBytesSent(finalData.size.toLong())

        logger.logFrameSent(finalData.size, "bluetooth")

        return writeQueue.offer(finalData) // Non-blocking add to queue
    }

    override fun receive(): ByteArray? {
        // This method is now handled by read thread
        return null
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

                            logger.logFrameReceived(message.size, "bluetooth")

                            messageHandler?.invoke(message)
                            println("Bluetooth received ${message.size} bytes")
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    println("Bluetooth read error: ${e.message}")
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
                    println("Bluetooth sent ${data.size} bytes")
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    println("Bluetooth write error: ${e.message}")
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

                    logger.logWatchdogTimeout("bluetooth", watchdogTimeout)

                    println("Bluetooth watchdog: No activity for ${watchdogTimeout/1000}s, triggering reconnect")
                    // In real implementation, notify SessionController for reconnect
                }
            }
        }
    }
}
}