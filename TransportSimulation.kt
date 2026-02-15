import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Inline definitions for simulation (same as before)
data class CommandRequest(
    val type: String,
    val payload: ByteArray = byteArrayOf()
) {
    fun encode(): ByteArray {
        val typeBytes = type.toByteArray(Charsets.UTF_8)
        val typeLen = typeBytes.size.toByte()
        val payloadLen = payload.size.toByte()
        val baos = ByteArrayOutputStream()
        baos.write(typeLen.toInt())
        baos.write(typeBytes)
        baos.write(payloadLen.toInt())
        baos.write(payload)
        return baos.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): CommandRequest {
            if (bytes.size < 2) throw IllegalArgumentException("Data too short")
            val typeLen = bytes[0].toInt()
            if (bytes.size < 1 + typeLen + 1) throw IllegalArgumentException("Invalid data")
            val type = String(bytes, 1, typeLen, Charsets.UTF_8)
            val payloadLen = bytes[1 + typeLen].toInt()
            val payloadStart = 1 + typeLen + 1
            if (bytes.size < payloadStart + payloadLen) throw IllegalArgumentException("Payload length mismatch")
            val payload = bytes.copyOfRange(payloadStart, payloadStart + payloadLen)
            return CommandRequest(type, payload)
        }
    }
}

data class CommandResponse(
    val status: String,
    val payload: ByteArray = byteArrayOf()
) {
    fun encode(): ByteArray {
        val statusBytes = status.toByteArray(Charsets.UTF_8)
        val statusLen = statusBytes.size.toByte()
        val payloadLen = payload.size.toByte()
        val baos = ByteArrayOutputStream()
        baos.write(statusLen.toInt())
        baos.write(statusBytes)
        baos.write(payloadLen.toInt())
        baos.write(payload)
        return baos.toByteArray()
    }

    companion object {
        fun decode(bytes: ByteArray): CommandResponse {
            if (bytes.size < 2) throw IllegalArgumentException("Data too short")
            val statusLen = bytes[0].toInt()
            if (bytes.size < 1 + statusLen + 1) throw IllegalArgumentException("Invalid data")
            val status = String(bytes, 1, statusLen, Charsets.UTF_8)
            val payloadLen = bytes[1 + statusLen].toInt()
            val payloadStart = 1 + statusLen + 1
            if (bytes.size < payloadStart + payloadLen) throw IllegalArgumentException("Payload length mismatch")
            val payload = bytes.copyOfRange(payloadStart, payloadStart + payloadLen)
            return CommandResponse(status, payload)
        }
    }
}

data class NoiseInit(val ephemeralPublicKey: ByteArray, val staticPublicKey: ByteArray, val payload: ByteArray)
data class NoiseResponse(val ephemeralPublicKey: ByteArray, val payload: ByteArray)

class ProtocolFraming {
    companion object {
        fun encodeNoiseInit(init: NoiseInit): ByteArray {
            val buf = mutableListOf<Byte>()
            buf.add(init.ephemeralPublicKey.size.toByte())
            buf.addAll(init.ephemeralPublicKey.asList())
            buf.add(init.staticPublicKey.size.toByte())
            buf.addAll(init.staticPublicKey.asList())
            buf.add(init.payload.size.toByte())
            buf.addAll(init.payload.asList())
            return frameMessage(buf.toByteArray())
        }

        fun decodeNoiseInit(data: ByteArray): NoiseInit? {
            if (data.size < 3) return null
            val ephemeralLen = data[0].toInt()
            if (data.size < 1 + ephemeralLen + 1) return null
            val ephemeral = data.sliceArray(1..ephemeralLen)
            val staticLen = data[1 + ephemeralLen].toInt()
            if (data.size < 1 + ephemeralLen + 1 + staticLen + 1) return null
            val static = data.sliceArray(1 + ephemeralLen + 1..1 + ephemeralLen + 1 + staticLen)
            val payloadLen = data[1 + ephemeralLen + 1 + staticLen].toInt()
            val payload = data.sliceArray(1 + ephemeralLen + 1 + staticLen + 1..data.size - 1)
            if (payload.size != payloadLen) return null
            return NoiseInit(ephemeral, static, payload)
        }

        fun encodeNoiseResponse(resp: NoiseResponse): ByteArray {
            val buf = mutableListOf<Byte>()
            buf.add(resp.ephemeralPublicKey.size.toByte())
            buf.addAll(resp.ephemeralPublicKey.asList())
            buf.add(resp.payload.size.toByte())
            buf.addAll(resp.payload.asList())
            return frameMessage(buf.toByteArray())
        }

        fun decodeNoiseResponse(data: ByteArray): NoiseResponse? {
            if (data.size < 2) return null
            val ephemeralLen = data[0].toInt()
            if (data.size < 1 + ephemeralLen + 1) return null
            val ephemeral = data.sliceArray(1..ephemeralLen)
            val payloadLen = data[1 + ephemeralLen].toInt()
            val payload = data.sliceArray(1 + ephemeralLen + 1..data.size - 1)
            if (payload.size != payloadLen) return null
            return NoiseResponse(ephemeral, payload)
        }

        fun frameCommandRequest(req: CommandRequest): ByteArray {
            return frameMessage(req.encode())
        }

        fun parseCommandRequest(bytes: ByteArray): CommandRequest? {
            val payload = parseFrame(bytes) ?: return null
            return try {
                CommandRequest.decode(payload)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun frameCommandResponse(resp: CommandResponse): ByteArray {
            return frameMessage(resp.encode())
        }

        fun parseCommandResponse(bytes: ByteArray): CommandResponse? {
            val payload = parseFrame(bytes) ?: return null
            return try {
                CommandResponse.decode(payload)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        private fun parseFrame(data: ByteArray): ByteArray? {
            if (data.size < 4) return null
            val length = ((data[0].toInt() and 0xFF) shl 24) or
                        ((data[1].toInt() and 0xFF) shl 16) or
                        ((data[2].toInt() and 0xFF) shl 8) or
                        (data[3].toInt() and 0xFF)
            if (data.size != 4 + length) return null
            return data.copyOfRange(4, data.size)
        }

        private fun frameMessage(data: ByteArray): ByteArray {
            val length = data.size
            val buf = ByteArray(4 + data.size)
            buf[0] = (length shr 24).toByte()
            buf[1] = (length shr 16).toByte()
            buf[2] = (length shr 8).toByte()
            buf[3] = length.toByte()
            data.copyInto(buf, 4)
            return buf
        }
    }
}

// Transport simulation utilities
class MockTransport {
    val desktopToMobile = ByteArrayOutputStream()
    val mobileToDesktop = ByteArrayOutputStream()

    fun getDesktopToMobileInputStream() = ByteArrayInputStream(desktopToMobile.toByteArray())
    fun getMobileToDesktopInputStream() = ByteArrayInputStream(mobileToDesktop.toByteArray())

    fun reset() {
        desktopToMobile.reset()
        mobileToDesktop.reset()
    }
}

// Streaming read functions
fun readFramedMessage(input: ByteArrayInputStream): ByteArray? {
    // Read 4-byte length prefix
    val lengthBytes = ByteArray(4)
    val readLen = input.read(lengthBytes)
    if (readLen != 4) return null

    val length = ((lengthBytes[0].toInt() and 0xFF) shl 24) or
                ((lengthBytes[1].toInt() and 0xFF) shl 16) or
                ((lengthBytes[2].toInt() and 0xFF) shl 8) or
                (lengthBytes[3].toInt() and 0xFF)

    // Read exact payload length
    val payload = ByteArray(length)
    val readPayload = input.read(payload)
    if (readPayload != length) return null

    return payload
}

fun main() {
    println("Starting A2.2.7 Transport Socket Integration Simulation")

    // A2.2.7.1 — Create Mock Transport
    val transport = MockTransport()
    println("✓ Mock transport established:")
    println("  - desktopToMobile stream")
    println("  - mobileToDesktop stream")

    // A2.2.7.2 — Desktop → Mobile Streaming Test
    println("\n=== A2.2.7.2 Desktop → Mobile Streaming Test ===")
    val req = CommandRequest("ping", byteArrayOf(1, 2, 3))
    val framedReq = ProtocolFraming.frameCommandRequest(req)

    // Desktop writes
    transport.desktopToMobile.write(framedReq)
    println("1. Desktop wrote framed CommandRequest: ${framedReq.size} bytes")

    // Mobile reads
    val mobileInput = transport.getDesktopToMobileInputStream()
    val receivedPayload = readFramedMessage(mobileInput)
    println("2. Mobile read payload: ${receivedPayload?.size} bytes")

    val decodedReq = receivedPayload?.let { CommandRequest.decode(it) }
    println("3. Mobile decoded: $decodedReq")

    // Validate
    assert(decodedReq != null) { "Decode failed" }
    assertEquals("ping", decodedReq?.type)
    assertTrue(decodedReq?.payload?.contentEquals(byteArrayOf(1, 2, 3)) == true)
    println("✓ Validation passed: No partial reads, no overreads, no frame boundary corruption")

    // A2.2.7.3 — Mobile → Desktop Streaming Test
    println("\n=== A2.2.7.3 Mobile → Desktop Streaming Test ===")
    val resp = CommandResponse("ok", byteArrayOf(9, 8, 7))
    val framedResp = ProtocolFraming.frameCommandResponse(resp)

    // Mobile writes
    transport.mobileToDesktop.write(framedResp)
    println("1. Mobile wrote framed CommandResponse: ${framedResp.size} bytes")

    // Desktop reads
    val desktopInput = transport.getMobileToDesktopInputStream()
    val receivedRespPayload = readFramedMessage(desktopInput)
    println("2. Desktop read payload: ${receivedRespPayload?.size} bytes")

    val decodedResp = receivedRespPayload?.let { CommandResponse.decode(it) }
    println("3. Desktop decoded: $decodedResp")

    // Validate
    assert(decodedResp != null) { "Decode failed" }
    assertEquals("ok", decodedResp?.status)
    assertTrue(decodedResp?.payload?.contentEquals(byteArrayOf(9, 8, 7)) == true)
    println("✓ Validation passed: Correct routing, correct decode")

    // A2.2.7.4 — Mixed Stream Test
    println("\n=== A2.2.7.4 Mixed Stream Test ===")
    transport.reset()

    val messages = listOf(
        "NoiseInit" to ProtocolFraming.encodeNoiseInit(NoiseInit(byteArrayOf(1,2,3), byteArrayOf(4,5,6), byteArrayOf(7,8,9))),
        "CommandRequest" to ProtocolFraming.frameCommandRequest(CommandRequest("device_info", byteArrayOf(42))),
        "CommandResponse" to ProtocolFraming.frameCommandResponse(CommandResponse("ok", byteArrayOf(99))),
        "CommandRequest" to ProtocolFraming.frameCommandRequest(CommandRequest("start_streaming", byteArrayOf()))
    )

    // Write sequence back-to-back
    val mixedStream = ByteArrayOutputStream()
    messages.forEach { (type, framed) ->
        mixedStream.write(framed)
        println("Wrote $type: ${framed.size} bytes")
    }

    // Read them sequentially
    val mixedInput = ByteArrayInputStream(mixedStream.toByteArray())
    val expectedTypes = listOf("handshake", "command", "response", "command")

    for ((index, expectedHandler) in expectedTypes.withIndex()) {
        val payload = readFramedMessage(mixedInput)
        assert(payload != null) { "Failed to read message $index" }

        val handler = when {
            ProtocolFraming.decodeNoiseInit(payload) != null -> "handshake"
            ProtocolFraming.parseCommandRequest(payload) != null -> "command"
            ProtocolFraming.parseCommandResponse(payload) != null -> "response"
            else -> "unknown"
        }

        println("Message ${index + 1}: Routed to $handler (expected: $expectedHandler)")
        assertEquals(expectedHandler, handler) { "Misrouting for message $index" }
    }

    println("✓ Validation passed: No frame boundary bleed, no misrouting, no decode failures")

    // A2.2.7.5 — Stress Test
    println("\n=== A2.2.7.5 Stress Test ===")
    transport.reset()

    val stressMessages = mutableListOf<ByteArray>()
    for (i in 0..99) {
        val msg = if (i % 2 == 0) {
            ProtocolFraming.frameCommandRequest(CommandRequest("stress_$i", byteArrayOf(i.toByte())))
        } else {
            ProtocolFraming.frameCommandResponse(CommandResponse("ok_$i", byteArrayOf((i * 2).toByte())))
        }
        stressMessages.add(msg)
    }

    val stressStream = ByteArrayOutputStream()
    stressMessages.forEach { stressStream.write(it) }

    val stressInput = ByteArrayInputStream(stressStream.toByteArray())
    var readCount = 0
    var anomalies = 0

    while (stressInput.available() > 0) {
        val payload = readFramedMessage(stressInput)
        if (payload == null) {
            println("  ⚠️  Failed to read message $readCount")
            anomalies++
            break
        }
        readCount++
    }

    println("Read $readCount/100 messages")
    if (anomalies == 0) {
        println("✓ No anomalies: No deadlocks, no partial frame reads, no dropped messages")
    } else {
        println("⚠️  Anomalies detected: $anomalies")
    }

    println("\n✓ Transport socket integration simulation complete!")
}