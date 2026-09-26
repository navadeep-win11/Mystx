package com.mystx.app.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object UpdateChecker {
    private val client = OkHttpClient()

    data class UpdateInfo(val version: String, val url: String, val body: String)

    suspend fun checkForUpdates(currentVersionName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/navadeep-win11/Mystx/releases/latest")
                .header("User-Agent", "Mystx-App")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use null
                    val json = JSONObject(body)
                    val tagName = json.getString("tag_name")
                    val htmlUrl = json.getString("html_url") // wait, it's html_url
                    val releaseNotes = json.optString("body", "")

                    // simple version string comparison, e.g. "v1.0.100" vs "1.0.100"
                    val latestClean = tagName.replace("v", "")
                    val currentClean = currentVersionName.replace("v", "")

                    if (isNewerVersion(latestClean, currentClean)) {
                        return@withContext UpdateInfo(tagName, json.getString("html_url"), releaseNotes)
                    }
                }
            }
        } catch (e: Exception) {
            // ignore network errors
        }
        null
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until len) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
