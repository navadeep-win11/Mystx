package com.mystx.app.service.discovery

import com.mystx.app.model.ModelInfo

/**
 * Common interface for provider-specific model discovery implementations.
 * Enables universal dynamic model discovery across all supported AI providers.
 */
interface ProviderModelDiscovery {
    /**
     * The unique provider type identifier (e.g. [com.mystx.app.model.ProviderType.GEMINI]).
     */
    val providerType: String

    /**
     * Discovers available models from the provider's API.
     *
     * @param apiKey The API key for authentication (may be null/blank for local endpoints).
     * @param endpoint The base URL or custom endpoint (if applicable to this provider).
     * @return A [Result] containing the list of discovered [ModelInfo] objects or an exception.
     */
    suspend fun discover(apiKey: String?, endpoint: String = ""): Result<List<ModelInfo>>

    /**
     * Fallback static catalog used when offline, unconfigured, or before discovery runs.
     */
    fun fallbackModels(): List<ModelInfo>
}
