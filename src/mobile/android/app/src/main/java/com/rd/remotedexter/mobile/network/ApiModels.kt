package com.rd.mobile.network

data class KnockRequest(
    val signature: String,
    val tagPublicKeyID: String,
    val timestamp: Long,
    val audience: String,
    val metadata: Map<String, String>
)

data class SessionTicket(
    val TicketID: ByteArray,
    val RelayNonce: ByteArray,
    val AgentID: String,
    val ControllerPubKeyID: String,
    val IssuedAtUnix: Long,
    val ExpiresAtUnix: Long,
    val AgentEphemeralPubKey: ByteArray,
    val Signature: ByteArray
)

data class KeyEvent(
    val KeyCode: Int,
    val Type: String
)

