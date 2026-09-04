package com.mystx.app.service.discovery

import android.content.SharedPreferences
import com.mystx.app.model.BaiModels
import com.mystx.app.model.GeminiModels
import com.mystx.app.model.GroqModels
import com.mystx.app.model.ModelInfo
import com.mystx.app.model.ProviderType
import java.util.concurrent.ConcurrentHashMap

/**
 * Universal service for dynamic AI model discovery and caching across all providers.
 */
object ModelDiscoveryService {

    private val providers = ConcurrentHashMap<String, ProviderModelDiscovery>()

    init {
        register(GeminiModelDiscovery())
        register(GroqModelDiscovery())
        register(BaiModelDiscovery())
        register(OpenAICompatibleModelDiscovery())
    }

    /**
     * Registers a [ProviderModelDiscovery] strategy. Allows runtime extension for new providers.
     */
    fun register(discovery: ProviderModelDiscovery) {
        providers[discovery.providerType] = discovery
    }

    /**
     * Retrieves the discovery implementation for a provider type.
     */
    fun getDiscovery(providerType: String): ProviderModelDiscovery? =
        providers[ProviderType.sanitize(providerType)]

    /**
     * Computes the SharedPreferences cache key for a given provider and endpoint.
     */
    fun getCacheKey(providerType: String, endpoint: String = ""): String {
        val sanitized = ProviderType.sanitize(providerType)
        return if (sanitized == ProviderType.CUSTOM) {
            val normalized = endpoint.trim().trimEnd('/').lowercase()
            "cached_models_custom_${normalized.hashCode()}"
        } else {
            "cached_models_$sanitized"
        }
    }

    /**
     * Retrieves cached models for the provider from [prefs].
     * If no cached models exist, falls back to the static catalog for that provider.
     */
    fun getCachedModels(
        providerType: String,
        prefs: SharedPreferences,
        endpoint: String = ""
    ): List<ModelInfo> {
        val key = getCacheKey(providerType, endpoint)
        val json = prefs.getString(key, null)
        if (!json.isNullOrBlank()) {
            val cached = ModelInfo.fromJsonArray(json)
            if (cached.isNotEmpty()) {
                return cached
            }
        }
        val discovery = getDiscovery(providerType)
        return discovery?.fallbackModels() ?: emptyList()
    }

    /**
     * Saves discovered models to [prefs] under the provider's cache key.
     */
    fun saveCachedModels(
        providerType: String,
        models: List<ModelInfo>,
        prefs: SharedPreferences,
        endpoint: String = ""
    ) {
        val key = getCacheKey(providerType, endpoint)
        if (models.isEmpty()) {
            prefs.edit().remove(key).apply()
        } else {
            val json = ModelInfo.toJsonArray(models)
            prefs.edit().putString(key, json).apply()
        }
    }

    /**
     * Clears cached models for a provider/endpoint.
     */
    fun clearCachedModels(
        providerType: String,
        prefs: SharedPreferences,
        endpoint: String = ""
    ) {
        val key = getCacheKey(providerType, endpoint)
        prefs.edit().remove(key).apply()
    }

    /**
     * Queries the provider's API for available models, updates cache upon success,
     * and returns the result.
     */
    suspend fun discoverAndCache(
        providerType: String,
        apiKey: String?,
        endpoint: String = "",
        prefs: SharedPreferences
    ): Result<List<ModelInfo>> {
        val discovery = getDiscovery(providerType)
            ?: return Result.failure(Exception("Unsupported provider: $providerType"))

        val result = discovery.discover(apiKey, endpoint)
        result.onSuccess { models ->
            if (models.isNotEmpty()) {
                saveCachedModels(providerType, models, prefs, endpoint)
            }
        }
        return result
    }

    /**
     * Resolves a human-friendly display label for [modelId] under the given provider.
     * Checks cached models first, then falls back to static catalog labels.
     */
    fun getModelLabel(
        modelId: String,
        providerType: String,
        prefs: SharedPreferences,
        endpoint: String = ""
    ): String {
        if (modelId.isBlank()) return ""
        val models = getCachedModels(providerType, prefs, endpoint)
        val found = models.firstOrNull { it.id == modelId }
        if (found != null) return found.displayName

        return when (ProviderType.sanitize(providerType)) {
            ProviderType.GEMINI -> GeminiModels.label(modelId)
            ProviderType.GROQ -> GroqModels.label(modelId)
            ProviderType.BAI -> BaiModels.label(modelId)
            else -> modelId
        }
    }
}
