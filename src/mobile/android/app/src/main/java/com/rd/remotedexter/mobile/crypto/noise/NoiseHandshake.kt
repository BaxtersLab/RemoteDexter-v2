package com.rd.mobile.crypto.noise

import com.remotedexter.mobile.network.SessionTicket
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.NamedParameterSpec
import java.security.spec.XECPublicKeySpec
import javax.crypto.KeyAgreement

class HandshakeMessage1(
    val TicketID: ByteArray,
    val RelayNonce: ByteArray,
    val ControllerEphemeralPub: ByteArray
)

class HandshakeMessage2(
    val AgentEphemeralPub: ByteArray
)

class HandshakeMessage3(
    val Ack: Boolean
)

object NoiseHandshake {
    fun generateEphemeral(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("X25519")
        kpg.initialize(NamedParameterSpec("X25519"))
        return kpg.generateKeyPair()
    }

    fun start(ticket: SessionTicket, controllerEphemeralPub: ByteArray): HandshakeMessage1 {
        return HandshakeMessage1(
            TicketID = ticket.TicketID,
            RelayNonce = ticket.RelayNonce,
            ControllerEphemeralPub = controllerEphemeralPub
        )
    }

    fun finalize(
        ticket: SessionTicket,
        controllerPrivate: ByteArray,
        agentEphemeralPub: ByteArray,
        controllerEphemeralPub: ByteArray
    ): NoiseState {
        val sharedSecret = deriveSharedSecret(controllerPrivate, agentEphemeralPub)
        val transcript = buildTranscript(ticket, controllerEphemeralPub, agentEphemeralPub)
        val transcriptHash = MessageDigest.getInstance("SHA-256").digest(transcript)
        val keys = NoiseHKDF.deriveKeys(sharedSecret, transcriptHash)
        val sessionId = NoiseHKDF.sessionIdFromTranscript(transcriptHash)
        return NoiseState(sessionId = sessionId, transcriptHash = transcriptHash, keys = keys)
    }

    fun buildTranscript(ticket: SessionTicket, controllerPub: ByteArray, agentPub: ByteArray): ByteArray {
        val agentIdBytes = ticket.AgentID.toByteArray(Charsets.UTF_8)
        val controllerIdBytes = ticket.ControllerPubKeyID.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(
            ticket.TicketID.size +
                ticket.RelayNonce.size +
                agentIdBytes.size +
                controllerIdBytes.size +
                8 +
                8 +
                controllerPub.size +
                agentPub.size
        )
        buffer.put(ticket.TicketID)
        buffer.put(ticket.RelayNonce)
        buffer.put(agentIdBytes)
        buffer.put(controllerIdBytes)
        buffer.putLong(ticket.IssuedAtUnix)
        buffer.putLong(ticket.ExpiresAtUnix)
        buffer.put(controllerPub)
        buffer.put(agentPub)
        return buffer.array()
    }

    private fun deriveSharedSecret(privateKeyBytes: ByteArray, peerPublicBytes: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("X25519")
        val pubSpec = XECPublicKeySpec(NamedParameterSpec("X25519"), peerPublicBytes)
        val publicKey = kf.generatePublic(pubSpec)
        val privateKey = kf.generatePrivate(java.security.spec.XECPrivateKeySpec(NamedParameterSpec("X25519"), privateKeyBytes))
        val ka = KeyAgreement.getInstance("X25519")
        ka.init(privateKey)
        ka.doPhase(publicKey, true)
        return ka.generateSecret()
    }
}

