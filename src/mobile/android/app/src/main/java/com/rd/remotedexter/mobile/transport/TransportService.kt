package com.rd.remotedexter.mobile.transport

import com.rd.remotedexter.mobile.protocol.CommandRequest
import com.rd.remotedexter.mobile.protocol.CommandResponse
import com.rd.remotedexter.mobile.protocol.ProtocolFraming

interface TransportService {
    fun isAvailable(): Boolean
    fun connect(messageHandler: ((ByteArray) -> Unit)? = null): Boolean
    fun send(data: ByteArray): Boolean
    fun receive(): ByteArray?
    fun disconnect()

    // New framed command methods
    fun sendCommand(command: CommandRequest): Boolean {
        val framed = ProtocolFraming.frameCommandRequest(command)
        return send(framed)
    }

    fun receiveCommand(): CommandRequest? {
        val framed = receive()
        return framed?.let { ProtocolFraming.parseCommandRequest(it) }
    }

    fun sendResponse(response: CommandResponse): Boolean {
        val framed = ProtocolFraming.frameCommandResponse(response)
        return send(framed)
    }

    fun receiveResponse(): CommandResponse? {
        val framed = receive()
        return framed?.let { ProtocolFraming.parseCommandResponse(it) }
    }
}