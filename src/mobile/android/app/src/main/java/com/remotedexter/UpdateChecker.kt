package com.remotedexter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker(private val context: Context) {

    companion object {
        private const val TAG = "UpdateChecker"
        private const val GITHUB_API_URL = "https://api.github.com/repos/your-org/remotedexter/releases/latest"
        private const val CURRENT_VERSION = "1.0.0"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Check for updates
    fun checkForUpdates(showDialogIfUpToDate: Boolean = false) {
        scope.launch {
            try {
                val latestRelease = fetchLatestRelease()
                val latestVersion = latestRelease.getString("tag_name").removePrefix("v")

                withContext(Dispatchers.Main) {
                    if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                        showUpdateDialog(latestRelease)
                    } else if (showDialogIfUpToDate) {
                        showUpToDateDialog()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check for updates", e)
                withContext(Dispatchers.Main) {
                    if (showDialogIfUpToDate) {
                        showErrorDialog()
                    }
                }
            }
        }
    }

    // Fetch latest release from GitHub API
    private suspend fun fetchLatestRelease(): JSONObject {
        return withContext(Dispatchers.IO) {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP error code: $responseCode")
                }

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                JSONObject(response.toString())
            } finally {
                connection.disconnect()
            }
        }
    }

    // Check if version A is newer than version B
    private fun isNewerVersion(versionA: String, versionB: String): Boolean {
        val partsA = versionA.split(".").map { it.toIntOrNull() ?: 0 }
        val partsB = versionB.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            val partA = partsA.getOrElse(i) { 0 }
            val partB = partsB.getOrElse(i) { 0 }

            if (partA > partB) return true
            if (partA < partB) return false
        }

        return false
    }

    // Show update available dialog
    private fun showUpdateDialog(release: JSONObject) {
        val version = release.getString("tag_name")
        val body = release.optString("body", "No release notes available")
        val apkUrl = release.getJSONArray("assets")
            .let { assets ->
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        return@let asset.getString("browser_download_url")
                    }
                }
                null
            }

        AlertDialog.Builder(context)
            .setTitle("Update Available")
            .setMessage("A new version of RemoteDexter is available: $version\n\nRelease Notes:\n$body")
            .setPositiveButton("Download") { _, _ ->
                apkUrl?.let { downloadApk(it) }
            }
            .setNegativeButton("Later", null)
            .setNeutralButton("What's New") { _, _ ->
                showReleaseNotesDialog(body)
            }
            .show()
    }

    // Show up-to-date dialog
    private fun showUpToDateDialog() {
        AlertDialog.Builder(context)
            .setTitle("Up to Date")
            .setMessage("You are running the latest version of RemoteDexter ($CURRENT_VERSION)")
            .setPositiveButton("OK", null)
            .show()
    }

    // Show error dialog
    private fun showErrorDialog() {
        AlertDialog.Builder(context)
            .setTitle("Update Check Failed")
            .setMessage("Unable to check for updates. Please check your internet connection and try again.")
            .setPositiveButton("OK", null)
            .show()
    }

    // Show release notes dialog
    private fun showReleaseNotesDialog(notes: String) {
        AlertDialog.Builder(context)
            .setTitle("Release Notes")
            .setMessage(notes)
            .setPositiveButton("Close", null)
            .show()
    }

    // Download and install APK
    private fun downloadApk(apkUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open download URL", e)
            AlertDialog.Builder(context)
                .setTitle("Download Failed")
                .setMessage("Unable to open download link. Please visit the GitHub releases page manually.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // Cleanup resources
    fun destroy() {
        scope.cancel()
    }
}