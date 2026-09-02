package com.mystx.app.model

/**
 * Single source of truth for the B.ai (b.ai) models Mystx offers.
 *
 * B.ai is an OpenAI-compatible AI gateway (https://api.b.ai/v1) that fronts many upstream
 * models behind one account credit balance. Its exact catalog depends on what the operator
 * currently routes, so the ids below are a curated set of widely-mirrored, stable ids rather
 * than a fetched list. A key is validated against GET /v1/models when it is added — the
 * request succeeds for any account with a valid key, regardless of which entries below it
 * can actually route.
 *
 * Users who need a model id outside this list can switch to the Custom provider and enter
 * https://api.b.ai/v1 as the endpoint with any model id.
 */
object BaiModels {

    /** One entry per offered model: its request id and its display label. */
    private data class Spec(val id: String, val label: String)

    // Ordered fast/cheap -> higher quality. Only ids that reliably TRANSFORM (never merely
    // answer) the user's text are suitable here; small chat-tuned instruct models work best.
    private val SPECS: List<Spec> = listOf(
        Spec("gpt-4o-mini", "GPT-4o mini"),
        Spec("gpt-4o", "GPT-4o"),
        Spec("gpt-4.1-mini", "GPT-4.1 mini"),
        Spec("deepseek-chat", "DeepSeek Chat"),
        Spec("gemini-2.0-flash", "Gemini 2.0 Flash")
    )

    /** Default model = first spec entry, so it can never point outside the catalog. */
    val DEFAULT: String = SPECS.first().id

    /** Model IDs, in display order (used for validation and by the provider config). */
    val ALL: List<String> = SPECS.map { it.id }

    /** (id, label) pairs for the Settings dropdown — shows a friendly name, stores the id. */
    val OPTIONS: List<Pair<String, String>> = SPECS.map { it.id to it.label }

    /** Friendly display label for [model]; falls back to the id if unknown. */
    fun label(model: String): String = SPECS.firstOrNull { it.id == model }?.label ?: model

    /** Coerce a stored/selected model to a currently-supported one. */
    fun sanitize(value: String?): String = if (value in ALL) value!! else DEFAULT
}
