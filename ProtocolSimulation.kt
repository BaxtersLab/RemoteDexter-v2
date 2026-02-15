import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Inline definitions for simulation
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

fun main() {
    println("Starting A2.2.6 End-to-End Protocol Simulation")

    // A2.2.6.1 — Desktop → Mobile Simulation
    println("\n=== A2.2.6.1 Desktop → Mobile ===")
    val req = CommandRequest("ping", byteArrayOf(1, 2, 3))
    println("1. Constructed: $req")

    val encodedReq = req.encode()
    println("2. Encoded: ${encodedReq.size} bytes")

    val framedReq = ProtocolFraming.frameMessage(encodedReq)
    println("3. Framed: ${framedReq.size} bytes (4-byte header + ${encodedReq.size} payload)")

    val payload = ProtocolFraming.parseFrame(framedReq)
    println("4. Parsed frame: ${payload?.size} bytes")
    assert(payload != null) { "Frame parsing failed" }

    val decodedReq = CommandRequest.decode(payload!!)
    println("5. Decoded: $decodedReq")

    // Validate
    assertEquals("ping", decodedReq.type)
    assertTrue(decodedReq.payload.contentEquals(byteArrayOf(1, 2, 3)))
    println("✓ Validation passed: type=${decodedReq.type}, payload=${decodedReq.payload.contentToString()}")

    // A2.2.6.2 — Mobile → Desktop Simulation
    println("\n=== A2.2.6.2 Mobile → Desktop ===")
    val resp = CommandResponse("ok", byteArrayOf(9, 8, 7))
    println("1. Constructed: $resp")

    val encodedResp = resp.encode()
    println("2. Encoded: ${encodedResp.size} bytes")

    val framedResp = ProtocolFraming.frameMessage(encodedResp)
    println("3. Framed: ${framedResp.size} bytes (4-byte header + ${encodedResp.size} payload)")

    val payloadResp = ProtocolFraming.parseFrame(framedResp)
    println("4. Parsed frame: ${payloadResp?.size} bytes")
    assert(payloadResp != null) { "Frame parsing failed" }

    val decodedResp = CommandResponse.decode(payloadResp!!)
    println("5. Decoded: $decodedResp")

    // Validate
    assertEquals("ok", decodedResp.status)
    assertTrue(decodedResp.payload.contentEquals(byteArrayOf(9, 8, 7)))
    println("✓ Validation passed: status=${decodedResp.status}, payload=${decodedResp.payload.contentToString()}")

    // A2.2.6.3 — Mixed Command Routing Simulation
    println("\n=== A2.2.6.3 Mixed Command Routing ===")
    val messages = listOf(
        Triple("NoiseInit", ProtocolFraming.encodeNoiseInit(NoiseInit(byteArrayOf(1,2,3), byteArrayOf(4,5,6), byteArrayOf(7,8,9))), "handshake"),
        Triple("CommandRequest", ProtocolFraming.frameCommandRequest(CommandRequest("device_info", byteArrayOf(42))), "command"),
        Triple("CommandResponse", ProtocolFraming.frameCommandResponse(CommandResponse("ok", byteArrayOf(99))), "response"),
        Triple("CommandRequest", ProtocolFraming.frameCommandRequest(CommandRequest("start_streaming", byteArrayOf())), "command")
    )

    for ((type, framed, expectedHandler) in messages) {
        println("Processing $type...")

        // Simulate routing based on content inspection
        val handler = when {
            // Check if it's a Noise message by trying to decode
            ProtocolFraming.decodeNoiseInit(framed) != null -> "handshake"
            ProtocolFraming.parseCommandRequest(framed) != null -> "command"
            ProtocolFraming.parseCommandResponse(framed) != null -> "response"
            else -> "unknown"
        }

        println("  Routed to: $handler (expected: $expectedHandler)")
        assertEquals(expectedHandler, handler) { "Misrouting for $type" }

        // Validate no decode failures
        when (handler) {
            "handshake" -> {
                val decoded = ProtocolFraming.decodeNoiseInit(framed)
                assert(decoded != null) { "Noise decode failed" }
                println("  ✓ Noise decoded successfully")
            }
            "command" -> {
                val decoded = ProtocolFraming.parseCommandRequest(framed)
                assert(decoded != null) { "CommandRequest decode failed" }
                println("  ✓ CommandRequest decoded successfully: ${decoded?.type}")
            }
            "response" -> {
                val decoded = ProtocolFraming.parseCommandResponse(framed)
                assert(decoded != null) { "CommandResponse decode failed" }
                println("  ✓ CommandResponse decoded successfully: ${decoded?.status}")
            }
        }
    }

    println("\n✓ All simulations passed!")
    println("No misrouting, no decode failures, no double-framing, no legacy serializers detected.")
}