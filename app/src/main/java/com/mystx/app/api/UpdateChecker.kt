package com.mystx.app.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    data class UpdateInfo(val version: String, val url: String, val body: String)

    suspend fun checkForUpdates(currentVersionName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/navadeep-win11/Mystx/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mystx-App")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseBody = reader.readText()
                reader.close()

                val json = JSONObject(responseBody)
                val tagName = json.getString("tag_name")
                val htmlUrl = json.getString("html_url")
                val releaseNotes = json.optString("body", "")

                val latestClean = tagName.replace("v", "")
                val currentClean = currentVersionName.replace("v", "")

                if (isNewerVersion(latestClean, currentClean)) {
                    return@withContext UpdateInfo(tagName, htmlUrl, releaseNotes)
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
