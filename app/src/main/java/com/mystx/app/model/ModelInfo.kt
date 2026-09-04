package com.mystx.app.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Standardized representation of an AI model across all providers.
 *
 * @property id The machine-readable model identifier sent in API requests (e.g. "gemini-2.0-flash", "llama-3.3-70b-versatile").
 * @property displayName The human-friendly name displayed in UI dropdowns (e.g. "Gemini 2.0 Flash").
 * @property provider The provider type identifier (e.g. [ProviderType.GEMINI], [ProviderType.GROQ]).
 */
data class ModelInfo(
    val id: String,
    val displayName: String = id,
    val provider: String = ""
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", displayName)
        put("provider", provider)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): ModelInfo {
            val id = obj.optString("id", "").trim()
            val name = obj.optString("name", id).trim()
            val provider = obj.optString("provider", "").trim()
            return ModelInfo(
                id = id,
                displayName = if (name.isNotBlank()) name else id,
                provider = provider
            )
        }

        fun toJsonArray(models: List<ModelInfo>): String {
            val arr = JSONArray()
            for (m in models) {
                arr.put(m.toJsonObject())
            }
            return arr.toString()
        }

        fun fromJsonArray(json: String): List<ModelInfo> {
            if (json.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                val list = mutableListOf<ModelInfo>()
                val seen = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    val model = if (obj != null) {
                        fromJsonObject(obj)
                    } else {
                        val rawId = arr.optString(i, "").trim()
                        if (rawId.isNotBlank()) ModelInfo(rawId, rawId) else null
                    }
                    if (model != null && model.id.isNotBlank() && seen.add(model.id)) {
                        list.add(model)
                    }
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
