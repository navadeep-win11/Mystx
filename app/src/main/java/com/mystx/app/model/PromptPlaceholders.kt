package com.mystx.app.model

import java.util.Locale

/**
 * Placeholder resolution for Command Studio prompt templates.
 *
 * Supported placeholders:
 *   {text}        selected / current input text
 *   {language}    target / configured language (e.g. from ?translate:es -> "es")
 *   {tone}        requested tone, if available
 *   {instruction} additional command instruction, if available
 *   {app}         application / package context, if available
 *
 * Safe handling:
 *   - Known tokens without available values are replaced with empty string.
 *   - Unknown tokens remain unchanged.
 *   - Legacy templates without tokens remain unchanged.
 */
object PromptPlaceholders {

    const val TEXT = "text"
    const val LANGUAGE = "language"
    const val TONE = "tone"
    const val INSTRUCTION = "instruction"
    const val APP = "app"

    val ALL = listOf(TEXT, LANGUAGE, TONE, INSTRUCTION, APP)

    data class Context(
        val text: String,
        val language: String? = null,
        val tone: String? = null,
        val instruction: String? = null,
        val app: String? = null
    )

    private fun token(name: String) = "{$name}"

    fun render(template: String, context: Context): String {
        if (!template.contains('{')) return template
        var out = template
            .replace(token(TEXT), context.text)
            .replace(token(LANGUAGE), context.language ?: "")
            .replace(token(TONE), context.tone ?: "")
            .replace(token(INSTRUCTION), context.instruction ?: "")
            .replace(token(APP), context.app ?: "")
        out = out.replace(Regex("[ \\t]*\\n[ \\t]*\\n[ \\t]*\\n+"), "\n\n").trim()
        return out
    }

    /**
     * Resolves language code from trigger (e.g. "?translate:es" -> "es").
     */
    fun languageFromTrigger(trigger: String): String? {
        val idx = trigger.lastIndexOf(':')
        if (idx <= 0 || idx == trigger.length - 1) return null
        val value = trigger.substring(idx + 1).trim()
        return value.ifBlank { null }
    }

    fun isTemplateMode(template: String): Boolean =
        ALL.any { template.contains(token(it)) }

    fun requiresText(template: String): Boolean =
        isTemplateMode(template) && !template.contains(token(TEXT))
}
