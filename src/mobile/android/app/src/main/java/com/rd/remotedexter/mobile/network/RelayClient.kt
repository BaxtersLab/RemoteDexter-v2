package com.rd.mobile.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Base64

class RelayClient(private val baseUrl: String) {
    private val client = OkHttpClient()

    fun getNonce(): ByteArray {
        val req = Request.Builder()
            .url("$baseUrl/nonce")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw RuntimeException("nonce request failed")
            }
            val body = resp.body?.string() ?: throw RuntimeException("nonce empty body")
            val json = JSONObject(body)
            val nonceB64 = json.getString("nonce")
            return Base64.getDecoder().decode(nonceB64)
        }
    }

    fun postKnock(req: KnockRequest): Boolean {
        val json = JSONObject()
            .put("signature", req.signature)
            .put("tagPublicKeyID", req.tagPublicKeyID)
            .put("timestamp", req.timestamp)
            .put("audience", req.audience)
            .put("metadata", JSONObject(req.metadata))

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$baseUrl/knock")
            .post(body)
            .build()

        client.newCall(request).execute().use { resp ->
            return resp.isSuccessful
        }
    }
}

