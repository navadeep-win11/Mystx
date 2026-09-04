package com.mystx.app.service.discovery

import com.mystx.app.api.ApiClientUtils
import com.mystx.app.model.GroqModels
import com.mystx.app.model.ModelInfo
import com.mystx.app.model.ProviderType
import com.mystx.app.provider.GroqConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Discovers available models for Groq via `https://api.groq.com/openai/v1/models`.
 * Filters out audio/whisper and guardrail models that cannot perform text transformation.
 */
class GroqModelDiscovery : ProviderModelDiscovery {

    override val providerType: String = ProviderType.GROQ

    override fun fallbackModels(): List<ModelInfo> =
        GroqModels.OPTIONS.map { (id, label) ->
            ModelInfo(id = id, displayName = label, provider = ProviderType.GROQ)
        }

    override suspend fun discover(apiKey: String?, endpoint: String): Result<List<ModelInfo>> =
        withContext(Dispatchers.IO) {
            val key = apiKey?.trim()
            if (key.isNullOrBlank()) {
                return@withContext Result.failure(Exception("API key required for Groq model discovery"))
            }

            var connection: HttpURLConnection? = null
            try {
                val url = "${GroqConfig.ENDPOINT}/models"
                connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $key")
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val body = ApiClientUtils.readResponseBounded(connection)
                    val models = parseGroqModels(body)
                    Result.success(if (models.isNotEmpty()) models else fallbackModels())
                } else {
                    val errorBody = ApiClientUtils.readErrorBody(connection)
                    val apiMessage = ApiClientUtils.extractApiErrorMessage(errorBody)
                    val detail = if (apiMessage.isNotBlank()) apiMessage else "Error $responseCode"
                    Result.failure(Exception(detail))
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }

    internal fun parseGroqModels(json: String): List<ModelInfo> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<ModelInfo>()
        val seen = mutableSetOf<String>()
        try {
            val root = JSONObject(json)
            val arr = root.optJSONArray("data") ?: return emptyList()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                if (id.isBlank() || !seen.add(id)) continue

                // Check active flag if present
                val active = obj.optBoolean("active", true)
                if (!active) continue

                // Filter out non-chat / non-text models
                val lower = id.lowercase()
                if (lower.contains("whisper") ||
                    lower.contains("guard") ||
                    lower.contains("distil-whisper")
                ) {
                    continue
                }

                val displayName = GroqModels.label(id)
                list.add(ModelInfo(id = id, displayName = displayName, provider = ProviderType.GROQ))
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }
}
