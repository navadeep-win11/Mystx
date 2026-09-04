package com.mystx.app.service.discovery

import com.mystx.app.api.ApiClientUtils
import com.mystx.app.model.GeminiModels
import com.mystx.app.model.ModelInfo
import com.mystx.app.model.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Discovers available models for Google Gemini via the Generative Language API.
 * Queries `https://generativelanguage.googleapis.com/v1beta/models` and filters
 * to models that support `generateContent`.
 */
class GeminiModelDiscovery : ProviderModelDiscovery {

    override val providerType: String = ProviderType.GEMINI

    override fun fallbackModels(): List<ModelInfo> =
        GeminiModels.OPTIONS.map { (id, label) ->
            ModelInfo(id = id, displayName = label, provider = ProviderType.GEMINI)
        }

    override suspend fun discover(apiKey: String?, endpoint: String): Result<List<ModelInfo>> =
        withContext(Dispatchers.IO) {
            val key = apiKey?.trim()
            if (key.isNullOrBlank()) {
                return@withContext Result.failure(Exception("API key required for Gemini model discovery"))
            }

            var connection: HttpURLConnection? = null
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=100"
                connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("x-goog-api-key", key)
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val body = ApiClientUtils.readResponseBounded(connection)
                    val models = parseGeminiModels(body)
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

    internal fun parseGeminiModels(json: String): List<ModelInfo> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<ModelInfo>()
        val seen = mutableSetOf<String>()
        try {
            val root = JSONObject(json)
            val arr = root.optJSONArray("models") ?: return emptyList()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val rawName = obj.optString("name", "").trim()
                val id = rawName.removePrefix("models/").trim()
                if (id.isBlank() || !seen.add(id)) continue

                // Verify the model supports text generation
                val methods = obj.optJSONArray("supportedGenerationMethods")
                var supportsGenerateContent = false
                if (methods != null) {
                    for (j in 0 until methods.length()) {
                        if (methods.optString(j) == "generateContent") {
                            supportsGenerateContent = true
                            break
                        }
                    }
                }
                // Skip embedding, vision-only, or non-generative models
                if (!supportsGenerateContent) continue

                val rawDisplayName = obj.optString("displayName", "").trim()
                val displayName = if (rawDisplayName.isNotBlank()) {
                    rawDisplayName
                } else {
                    GeminiModels.label(id)
                }

                list.add(ModelInfo(id = id, displayName = displayName, provider = ProviderType.GEMINI))
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }
}
