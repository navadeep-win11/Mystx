package com.mystx.app.model

object ProviderType {
    const val GEMINI = "gemini"
    const val GROQ = "groq"
    const val BAI = "bai"
    const val CUSTOM = "custom"

    private val VALID = setOf(GEMINI, GROQ, BAI, CUSTOM)
    fun sanitize(value: String?): String = if (value in VALID) value!! else GEMINI
}
