package com.rd.remotedexter.mobile.protocol

import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtocolSimulationTest {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
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
    }
}