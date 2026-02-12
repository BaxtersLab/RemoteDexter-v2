package com.rd.remotedexter.mobile.security

import android.content.Context
import android.util.Log
import com.rd.remotedexter.mobile.shared.protocol.TrustedDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * TrustStore manages persistent storage of trusted devices on Android
 */
class TrustStore(private val context: Context) {

    companion object {
        private const val TAG = "TrustStore"
        private const val TRUSTED_DEVICES_FILE = "trusted_devices.json"
    }

    private val trustedDevicesFile: File by lazy {
        File(context.getDir("rd", Context.MODE_PRIVATE), TRUSTED_DEVICES_FILE)
    }

    private val devices = mutableMapOf<String, TrustedDevice>()

    init {
        loadDevices()
    }

    /**
     * Generates a unique device ID from the public key using SHA-256
     */
    private fun generateDeviceID(publicKey: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKey)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Adds a new trusted device
     */
    suspend fun addDevice(deviceName: String, publicKey: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val deviceID = generateDeviceID(publicKey)

            // Check if device already exists
            if (devices.containsKey(deviceID)) {
                return@withContext Result.failure(Exception("Device already trusted"))
            }

            val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

            val device = TrustedDevice(
                deviceName = deviceName,
                deviceID = deviceID,
                publicKey = publicKey,
                addedAt = now,
                lastSeen = now
            )

            devices[deviceID] = device

            saveDevices()

            Log.i(TAG, "Added trusted device: $deviceName ($deviceID)")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to add device", e)
            Result.failure(e)
        }
    }

    /**
     * Removes a trusted device
     */
    suspend fun removeDevice(deviceID: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!devices.containsKey(deviceID)) {
                return@withContext Result.failure(Exception("Device not found"))
            }

            devices.remove(deviceID)
            saveDevices()

            Log.i(TAG, "Removed trusted device: $deviceID")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove device", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if a device with the given public key is trusted
     */
    fun isTrusted(publicKey: ByteArray): Boolean {
        val deviceID = generateDeviceID(publicKey)
        return devices.containsKey(deviceID)
    }

    /**
     * Returns all trusted devices
     */
    suspend fun listDevices(): Result<List<TrustedDevice>> = withContext(Dispatchers.IO) {
        try {
            Result.success(devices.values.toList())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list devices", e)
            Result.failure(e)
        }
    }

    /**
     * Gets a specific trusted device by ID
     */
    fun getDevice(deviceID: String): TrustedDevice? {
        return devices[deviceID]
    }

    /**
     * Updates the last seen timestamp for a device
     */
    suspend fun updateLastSeen(deviceID: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val device = devices[deviceID]
                ?: return@withContext Result.failure(Exception("Device not found"))

            val updatedDevice = device.copy(
                lastSeen = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            )

            devices[deviceID] = updatedDevice
            saveDevices()

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last seen", e)
            Result.failure(e)
        }
    }

    /**
     * Clears all trusted devices (used for lost device protocol)
     */
    suspend fun clearAllDevices(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            devices.clear()
            saveDevices()

            Log.i(TAG, "Cleared all trusted devices")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear devices", e)
            Result.failure(e)
        }
    }

    /**
     * Loads trusted devices from JSON file
     */
    private fun loadDevices() {
        try {
            if (!trustedDevicesFile.exists()) {
                Log.d(TAG, "Trusted devices file does not exist, starting with empty store")
                return
            }

            val jsonString = trustedDevicesFile.readText()
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val device = TrustedDevice(
                    deviceName = jsonObject.getString("device_name"),
                    deviceID = jsonObject.getString("device_id"),
                    publicKey = jsonObject.getString("public_key").toByteArray(),
                    addedAt = jsonObject.getString("added_at"),
                    lastSeen = jsonObject.getString("last_seen")
                )

                devices[device.deviceID] = device
            }

            Log.i(TAG, "Loaded ${devices.size} trusted devices")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load trusted devices", e)
            // Start with empty store on error
            devices.clear()
        }
    }

    /**
     * Saves trusted devices to JSON file
     */
    private fun saveDevices() {
        try {
            val jsonArray = JSONArray()

            for (device in devices.values) {
                val jsonObject = JSONObject().apply {
                    put("device_name", device.deviceName)
                    put("device_id", device.deviceID)
                    put("public_key", device.publicKey.toString(Charsets.ISO_8859_1))
                    put("added_at", device.addedAt)
                    put("last_seen", device.lastSeen)
                }
                jsonArray.put(jsonObject)
            }

            trustedDevicesFile.writeText(jsonArray.toString(2))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save trusted devices", e)
            throw e
        }
    }
}