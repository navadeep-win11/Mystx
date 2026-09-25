package com.mystx.app.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object HistoryManager {
    private const val MAX_HISTORY = 100
    private const val FILE_NAME = "history.json"
    
    data class HistoryItem(
        val originalText: String,
        val commandTrigger: String,
        val response: String,
        val timestamp: Long,
        val isSelection: Boolean
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("originalText", originalText)
                put("commandTrigger", commandTrigger)
                put("response", response)
                put("timestamp", timestamp)
                put("isSelection", isSelection)
            }
        }
        
        companion object {
            fun fromJson(json: JSONObject): HistoryItem {
                return HistoryItem(
                    originalText = json.optString("originalText", ""),
                    commandTrigger = json.optString("commandTrigger", ""),
                    response = json.optString("response", ""),
                    timestamp = json.optLong("timestamp", 0L),
                    isSelection = json.optBoolean("isSelection", false)
                )
            }
        }
    }

    private var cachedHistory = mutableListOf<HistoryItem>()
    private var isLoaded = false

    @Synchronized
    private fun loadHistory(context: Context) {
        if (isLoaded) return
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            isLoaded = true
            return
        }
        try {
            val content = file.readText()
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                cachedHistory.add(HistoryItem.fromJson(array.getJSONObject(i)))
            }
            // Sort descending by timestamp
            cachedHistory.sortByDescending { it.timestamp }
        } catch (_: Exception) {}
        isLoaded = true
    }

    @Synchronized
    private fun saveHistory(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        try {
            val array = JSONArray()
            cachedHistory.forEach { array.put(it.toJson()) }
            file.writeText(array.toString())
        } catch (_: Exception) {}
    }

    @Synchronized
    fun addEntry(context: Context, originalText: String, commandTrigger: String, response: String, isSelection: Boolean) {
        loadHistory(context)
        val newItem = HistoryItem(originalText, commandTrigger, response, System.currentTimeMillis(), isSelection)
        // Remove exact duplicate if exists
        cachedHistory.removeAll { it.originalText == originalText && it.commandTrigger == commandTrigger && it.isSelection == isSelection }
        cachedHistory.add(0, newItem)
        if (cachedHistory.size > MAX_HISTORY) {
            cachedHistory = cachedHistory.take(MAX_HISTORY).toMutableList()
        }
        saveHistory(context)
    }

    @Synchronized
    fun getHistory(context: Context): List<HistoryItem> {
        loadHistory(context)
        return cachedHistory.toList()
    }

    fun findCachedResponse(context: Context, originalText: String, commandTrigger: String): String? {
        loadHistory(context)
        // Only fetch cache for non-selection (keyboard typed) items
        return cachedHistory.find { !it.isSelection && it.originalText == originalText && it.commandTrigger == commandTrigger }?.response
    }
}
