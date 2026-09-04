package com.mystx.app.service.discovery

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.mystx.app.model.BaiModels
import com.mystx.app.model.GeminiModels
import com.mystx.app.model.GroqModels
import com.mystx.app.model.ModelInfo
import com.mystx.app.model.ProviderType
import com.mystx.app.provider.BaiConfig
import com.mystx.app.provider.CustomConfig
import com.mystx.app.provider.GeminiConfig
import com.mystx.app.provider.GroqConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelDiscoveryServiceTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences("test_settings", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    // --- ModelInfo Serialization Tests ---

    @Test
    fun modelInfo_serializationRoundTrip() {
        val original = listOf(
            ModelInfo(id = "gemini-2.0-flash", displayName = "Gemini 2.0 Flash", provider = ProviderType.GEMINI),
            ModelInfo(id = "gemini-1.5-pro", displayName = "Gemini 1.5 Pro", provider = ProviderType.GEMINI)
        )
        val json = ModelInfo.toJsonArray(original)
        val restored = ModelInfo.fromJsonArray(json)

        assertEquals(2, restored.size)
        assertEquals("gemini-2.0-flash", restored[0].id)
        assertEquals("Gemini 2.0 Flash", restored[0].displayName)
        assertEquals(ProviderType.GEMINI, restored[0].provider)

        assertEquals("gemini-1.5-pro", restored[1].id)
        assertEquals("Gemini 1.5 Pro", restored[1].displayName)
        assertEquals(ProviderType.GEMINI, restored[1].provider)
    }

    @Test
    fun modelInfo_fromJsonArray_handlesLegacyStringArray() {
        val rawArrayJson = "[\"model-a\", \"model-b\"]"
        val restored = ModelInfo.fromJsonArray(rawArrayJson)

        assertEquals(2, restored.size)
        assertEquals("model-a", restored[0].id)
        assertEquals("model-a", restored[0].displayName)
        assertEquals("model-b", restored[1].id)
        assertEquals("model-b", restored[1].displayName)
    }

    @Test
    fun modelInfo_fromJsonArray_toleratesMalformedJson() {
        assertEquals(emptyList<ModelInfo>(), ModelInfo.fromJsonArray(""))
        assertEquals(emptyList<ModelInfo>(), ModelInfo.fromJsonArray("not-json"))
        assertEquals(emptyList<ModelInfo>(), ModelInfo.fromJsonArray("{ \"not\": \"an array\" }"))
    }

    @Test
    fun modelInfo_fromJsonArray_deduplicatesById() {
        val json = """
            [
                {"id": "m1", "name": "First M1"},
                {"id": "m1", "name": "Duplicate M1"},
                {"id": "m2", "name": "Second"}
            ]
        """.trimIndent()
        val restored = ModelInfo.fromJsonArray(json)
        assertEquals(2, restored.size)
        assertEquals("m1", restored[0].id)
        assertEquals("First M1", restored[0].displayName)
        assertEquals("m2", restored[1].id)
    }

    // --- Gemini Parsing & Discovery Tests ---

    @Test
    fun geminiDiscovery_parseGeminiModels_extractsAndFilters() {
        val geminiDiscovery = GeminiModelDiscovery()
        val json = """
            {
                "models": [
                    {
                        "name": "models/gemini-2.0-flash",
                        "displayName": "Gemini 2.0 Flash",
                        "supportedGenerationMethods": ["generateContent", "countTokens"]
                    },
                    {
                        "name": "models/text-embedding-004",
                        "displayName": "Text Embedding 004",
                        "supportedGenerationMethods": ["embedContent"]
                    },
                    {
                        "name": "models/imagen-3.0",
                        "displayName": "Imagen 3.0",
                        "supportedGenerationMethods": ["generateImages"]
                    },
                    {
                        "name": "models/gemini-1.5-flash-8b",
                        "supportedGenerationMethods": ["generateContent"]
                    }
                ]
            }
        """.trimIndent()

        val models = geminiDiscovery.parseGeminiModels(json)
        assertEquals(2, models.size)

        assertEquals("gemini-2.0-flash", models[0].id)
        assertEquals("Gemini 2.0 Flash", models[0].displayName)
        assertEquals(ProviderType.GEMINI, models[0].provider)

        assertEquals("gemini-1.5-flash-8b", models[1].id)
        assertEquals("gemini-1.5-flash-8b", models[1].displayName)
        assertEquals(ProviderType.GEMINI, models[1].provider)
    }

    @Test
    fun geminiDiscovery_fallbackModels_matchesCatalog() {
        val geminiDiscovery = GeminiModelDiscovery()
        val fallback = geminiDiscovery.fallbackModels()

        assertTrue(fallback.isNotEmpty())
        assertEquals(GeminiModels.OPTIONS.size, fallback.size)
        assertEquals(GeminiModels.DEFAULT, fallback.first().id)
        assertEquals(GeminiModels.OPTIONS.first().second, fallback.first().displayName)
    }

    @Test
    fun geminiDiscovery_discoverWithoutKeyFails() = runBlocking {
        val geminiDiscovery = GeminiModelDiscovery()
        val result = geminiDiscovery.discover("", "")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API key required") == true)
    }

    // --- Groq Parsing & Discovery Tests ---

    @Test
    fun groqDiscovery_parseGroqModels_filtersWhisperAndInactive() {
        val groqDiscovery = GroqModelDiscovery()
        val json = """
            {
                "object": "list",
                "data": [
                    {
                        "id": "openai/gpt-oss-120b",
                        "active": true
                    },
                    {
                        "id": "whisper-large-v3",
                        "active": true
                    },
                    {
                        "id": "llama-guard-3-8b",
                        "active": true
                    },
                    {
                        "id": "qwen/qwen3.6-27b",
                        "active": true
                    },
                    {
                        "id": "deprecated-model",
                        "active": false
                    }
                ]
            }
        """.trimIndent()

        val models = groqDiscovery.parseGroqModels(json)
        assertEquals(2, models.size)
        assertEquals("openai/gpt-oss-120b", models[0].id)
        assertEquals("GPT-OSS 120B", models[0].displayName) // Uses GroqModels.label
        assertEquals("qwen/qwen3.6-27b", models[1].id)
        assertEquals("Qwen 3.6 27B", models[1].displayName)
    }

    @Test
    fun groqDiscovery_fallbackModels_matchesCatalog() {
        val groqDiscovery = GroqModelDiscovery()
        val fallback = groqDiscovery.fallbackModels()

        assertTrue(fallback.isNotEmpty())
        assertEquals(GroqModels.OPTIONS.size, fallback.size)
        assertEquals(GroqModels.DEFAULT, fallback.first().id)
    }

    @Test
    fun groqDiscovery_discoverWithoutKeyFails() = runBlocking {
        val groqDiscovery = GroqModelDiscovery()
        val result = groqDiscovery.discover(null, "")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API key required") == true)
    }

    // --- B.ai Parsing & Discovery Tests ---

    @Test
    fun baiDiscovery_parseBaiModels_extractsModels() {
        val baiDiscovery = BaiModelDiscovery()
        val json = """
            {
                "data": [
                    {"id": "gpt-4o-mini"},
                    {"id": "deepseek-chat"},
                    {"id": "claude-3-5-sonnet"}
                ]
            }
        """.trimIndent()

        val models = baiDiscovery.parseBaiModels(json)
        assertEquals(3, models.size)
        assertEquals("gpt-4o-mini", models[0].id)
        assertEquals("GPT-4o mini", models[0].displayName) // Friendly label
        assertEquals("deepseek-chat", models[1].id)
        assertEquals("DeepSeek Chat", models[1].displayName)
        assertEquals("claude-3-5-sonnet", models[2].id)
        assertEquals("claude-3-5-sonnet", models[2].displayName)
    }

    @Test
    fun baiDiscovery_fallbackModels_matchesCatalog() {
        val baiDiscovery = BaiModelDiscovery()
        val fallback = baiDiscovery.fallbackModels()

        assertTrue(fallback.isNotEmpty())
        assertEquals(BaiModels.OPTIONS.size, fallback.size)
        assertEquals(BaiModels.DEFAULT, fallback.first().id)
    }

    @Test
    fun baiDiscovery_discoverWithoutKeyFails() = runBlocking {
        val baiDiscovery = BaiModelDiscovery()
        val result = baiDiscovery.discover("   ", "")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API key required") == true)
    }

    // --- Custom / OpenAI-Compatible Discovery Tests ---

    @Test
    fun openAiCompatDiscovery_fallbackIsEmpty() {
        val discovery = OpenAICompatibleModelDiscovery()
        assertTrue(discovery.fallbackModels().isEmpty())
    }

    @Test
    fun openAiCompatDiscovery_requiresEndpoint() = runBlocking {
        val discovery = OpenAICompatibleModelDiscovery()
        val result = discovery.discover("key", "")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Endpoint URL is required") == true)
    }

    @Test
    fun openAiCompatDiscovery_validatesEndpointScheme() = runBlocking {
        val discovery = OpenAICompatibleModelDiscovery()
        val result = discovery.discover("key", "ftp://my-host.com")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Endpoint must be https") == true)
    }

    // --- ModelDiscoveryService Cache & Registry Tests ---

    @Test
    fun service_registryRetrievesProviders() {
        assertNotNull(ModelDiscoveryService.getDiscovery(ProviderType.GEMINI))
        assertNotNull(ModelDiscoveryService.getDiscovery(ProviderType.GROQ))
        assertNotNull(ModelDiscoveryService.getDiscovery(ProviderType.BAI))
        assertNotNull(ModelDiscoveryService.getDiscovery(ProviderType.CUSTOM))
    }

    @Test
    fun service_cacheKeyGeneration() {
        assertEquals("cached_models_gemini", ModelDiscoveryService.getCacheKey(ProviderType.GEMINI))
        assertEquals("cached_models_groq", ModelDiscoveryService.getCacheKey(ProviderType.GROQ))
        assertEquals("cached_models_bai", ModelDiscoveryService.getCacheKey(ProviderType.BAI))

        val key1 = ModelDiscoveryService.getCacheKey(ProviderType.CUSTOM, "https://api.openai.com/v1")
        val key2 = ModelDiscoveryService.getCacheKey(ProviderType.CUSTOM, "https://api.openai.com/v1/")
        assertEquals(key1, key2) // trailing slash normalized

        val key3 = ModelDiscoveryService.getCacheKey(ProviderType.CUSTOM, "http://192.168.1.100:11434/v1")
        assertFalse(key1 == key3) // distinct endpoints yield distinct keys
    }

    @Test
    fun service_getCachedModels_returnsFallbackWhenEmpty() {
        val models = ModelDiscoveryService.getCachedModels(ProviderType.GEMINI, prefs)
        assertEquals(GeminiModels.OPTIONS.size, models.size)
        assertEquals(GeminiModels.DEFAULT, models.first().id)
    }

    @Test
    fun service_saveAndGetCachedModels() {
        val discovered = listOf(
            ModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash", ProviderType.GEMINI),
            ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", ProviderType.GEMINI)
        )
        ModelDiscoveryService.saveCachedModels(ProviderType.GEMINI, discovered, prefs)

        val retrieved = ModelDiscoveryService.getCachedModels(ProviderType.GEMINI, prefs)
        assertEquals(2, retrieved.size)
        assertEquals("gemini-2.0-flash", retrieved[0].id)
        assertEquals("Gemini 2.0 Flash", retrieved[0].displayName)
    }

    @Test
    fun service_clearCachedModels_resetsToFallback() {
        val discovered = listOf(
            ModelInfo("gemini-custom-1", "Custom", ProviderType.GEMINI)
        )
        ModelDiscoveryService.saveCachedModels(ProviderType.GEMINI, discovered, prefs)
        assertEquals(1, ModelDiscoveryService.getCachedModels(ProviderType.GEMINI, prefs).size)

        ModelDiscoveryService.clearCachedModels(ProviderType.GEMINI, prefs)
        val afterClear = ModelDiscoveryService.getCachedModels(ProviderType.GEMINI, prefs)
        assertEquals(GeminiModels.OPTIONS.size, afterClear.size)
    }

    @Test
    fun service_providerIsolationInCache() {
        val geminiList = listOf(ModelInfo("gemini-test", "Gemini Test", ProviderType.GEMINI))
        val groqList = listOf(ModelInfo("groq-test", "Groq Test", ProviderType.GROQ))

        ModelDiscoveryService.saveCachedModels(ProviderType.GEMINI, geminiList, prefs)
        ModelDiscoveryService.saveCachedModels(ProviderType.GROQ, groqList, prefs)

        val retrievedGemini = ModelDiscoveryService.getCachedModels(ProviderType.GEMINI, prefs)
        val retrievedGroq = ModelDiscoveryService.getCachedModels(ProviderType.GROQ, prefs)

        assertEquals(1, retrievedGemini.size)
        assertEquals("gemini-test", retrievedGemini[0].id)

        assertEquals(1, retrievedGroq.size)
        assertEquals("groq-test", retrievedGroq[0].id)
    }

    @Test
    fun service_getModelLabel_resolvesCachedAndFallback() {
        val discovered = listOf(ModelInfo("custom-discovered", "Friendly Custom Name", ProviderType.GEMINI))
        ModelDiscoveryService.saveCachedModels(ProviderType.GEMINI, discovered, prefs)

        // Cached label
        assertEquals("Friendly Custom Name", ModelDiscoveryService.getModelLabel("custom-discovered", ProviderType.GEMINI, prefs))

        // Known static catalog fallback
        assertEquals("Gemini 3.5 Flash-Lite", ModelDiscoveryService.getModelLabel("gemini-3.5-flash-lite", ProviderType.GEMINI, prefs))
        assertEquals("GPT-OSS 120B", ModelDiscoveryService.getModelLabel("openai/gpt-oss-120b", ProviderType.GROQ, prefs))

        // Unknown model fallback to id
        assertEquals("unknown-model", ModelDiscoveryService.getModelLabel("unknown-model", ProviderType.GEMINI, prefs))
    }

    // --- ProviderConfig.sanitizeModel Compatibility Tests ---

    @Test
    fun providerConfig_sanitizeModel_preservesDynamicModels() {
        // GeminiConfig: keeps discovered models, coerces blank and retired to default
        assertEquals("gemini-2.0-flash", GeminiConfig.sanitizeModel("gemini-2.0-flash"))
        assertEquals("gemini-1.5-pro", GeminiConfig.sanitizeModel("gemini-1.5-pro"))
        assertEquals(GeminiModels.DEFAULT, GeminiConfig.sanitizeModel("gemini-2.5-flash-lite"))
        assertEquals(GeminiModels.DEFAULT, GeminiConfig.sanitizeModel(""))
        assertEquals(GeminiModels.DEFAULT, GeminiConfig.sanitizeModel(null))

        // GroqConfig: keeps discovered models, coerces blank to default
        assertEquals("llama-3.3-70b-versatile", GroqConfig.sanitizeModel("llama-3.3-70b-versatile"))
        assertEquals("meta-llama/llama-4-scout-17b-16e-instruct", GroqConfig.sanitizeModel("meta-llama/llama-4-scout-17b-16e-instruct"))
        assertEquals(GroqModels.DEFAULT, GroqConfig.sanitizeModel(""))
        assertEquals(GroqModels.DEFAULT, GroqConfig.sanitizeModel(null))

        // BaiConfig: keeps discovered models, coerces blank to default
        assertEquals("claude-3-5-sonnet", BaiConfig.sanitizeModel("claude-3-5-sonnet"))
        assertEquals(BaiModels.DEFAULT, BaiConfig.sanitizeModel(""))
        assertEquals(BaiModels.DEFAULT, BaiConfig.sanitizeModel(null))

        // CustomConfig: keeps trimmed model, coerces null to empty
        assertEquals("mistral-large", CustomConfig.sanitizeModel("  mistral-large  "))
        assertEquals("", CustomConfig.sanitizeModel(null))
    }
}
