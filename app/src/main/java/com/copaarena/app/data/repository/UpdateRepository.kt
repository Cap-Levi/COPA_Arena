package com.copaarena.app.data.repository

import com.copaarena.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateCheckResult {
    data class UpdateAvailable(val latestVersion: String, val releaseUrl: String) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/** Checks GitHub Releases for a newer build than the one currently installed. No auth
 *  needed — this hits the public releases API for a public repo. */
@Singleton
class UpdateRepository @Inject constructor() {

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/Cap-Levi/COPA_Arena/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("No releases found yet")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.getString("tag_name").removePrefix("v")
            val releaseUrl = json.optString("html_url", "https://github.com/Cap-Levi/COPA_Arena/releases/latest")

            if (isNewer(tagName, BuildConfig.VERSION_NAME)) {
                UpdateCheckResult.UpdateAvailable(tagName, releaseUrl)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Couldn't check for updates")
        }
    }

    /** Compares dotted version strings numerically (1.2.10 > 1.2.9), padding missing parts with 0. */
    private fun isNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val size = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until size) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }
}
