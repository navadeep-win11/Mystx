package com.mystx.app.model

import androidx.compose.runtime.Immutable
import org.json.JSONObject

/**
 * Categories for grouping commands inside Command Studio.
 */
enum class CommandCategory(val id: String, val displayName: String) {
    ALL("all", "All"),
    WRITING("writing", "Writing"),
    TRANSLATION("translation", "Translation"),
    CODING("coding", "Coding"),
    PRODUCTIVITY("productivity", "Productivity"),
    SOCIAL("social", "Social"),
    CUSTOM("custom", "Custom");

    companion object {
        val FILTER_CATEGORIES = listOf(ALL, WRITING, TRANSLATION, CODING, PRODUCTIVITY, SOCIAL, CUSTOM)
        val EDITABLE_CATEGORIES = listOf(WRITING, TRANSLATION, CODING, PRODUCTIVITY, SOCIAL, CUSTOM)

        fun fromId(id: String?): CommandCategory =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CUSTOM
    }
}

/**
 * Rich representation of a command for Command Studio.
 *
 * Contains presentation metadata, per-command model and temperature overrides,
 * category, and enabled state, while remaining fully compatible with the underlying [Command].
 */
@Immutable
data class RichCommand(
    val id: String,
    val name: String,
    val trigger: String,
    val category: CommandCategory,
    val promptTemplate: String,
    val modelOverride: String?,
    val temperature: Float?,
    val enabled: Boolean,
    val isBuiltIn: Boolean,
    val type: CommandType = CommandType.AI
) {
    val metaKey: String get() = keyFor(trigger)

    fun withMeta(meta: CommandMeta): RichCommand = copy(
        name = meta.name.ifBlank { name },
        category = meta.category,
        promptTemplate = meta.promptTemplate ?: promptTemplate,
        modelOverride = meta.modelOverride,
        temperature = meta.temperature,
        enabled = meta.enabled
    )

    fun toMeta(): CommandMeta = CommandMeta(
        name = name,
        category = category,
        promptTemplate = promptTemplate,
        modelOverride = modelOverride,
        temperature = temperature,
        enabled = enabled
    )

    companion object {
        fun keyFor(trigger: String): String =
            if (trigger.isEmpty()) "" else trigger.substring(1).lowercase()

        fun fromCommand(command: Command): RichCommand = RichCommand(
            id = keyFor(command.trigger),
            name = StudioDefaults.nameFor(command),
            trigger = command.trigger,
            category = StudioDefaults.categoryFor(command),
            promptTemplate = command.prompt,
            modelOverride = null,
            temperature = null,
            enabled = true,
            isBuiltIn = command.isBuiltIn,
            type = command.type
        )
    }
}

/**
 * Serialized metadata of a command's Command Studio configuration.
 */
data class CommandMeta(
    val name: String = "",
    val category: CommandCategory = CommandCategory.CUSTOM,
    val promptTemplate: String? = null,
    val modelOverride: String? = null,
    val temperature: Float? = null,
    val enabled: Boolean = true
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("category", category.id)
        if (promptTemplate != null) obj.put("prompt", promptTemplate)
        if (modelOverride != null) obj.put("model", modelOverride)
        if (temperature != null) obj.put("temp", temperature.toDouble())
        obj.put("enabled", enabled)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String): CommandMeta? = try {
            val obj = JSONObject(json)
            CommandMeta(
                name = obj.optString("name", ""),
                category = CommandCategory.fromId(obj.optString("category", CommandCategory.CUSTOM.id)),
                promptTemplate = obj.optString("prompt", "").takeIf { it.isNotEmpty() },
                modelOverride = obj.optString("model", "").takeIf { it.isNotEmpty() },
                temperature = if (obj.has("temp")) obj.getDouble("temp").toFloat() else null,
                enabled = obj.optBoolean("enabled", true)
            )
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Curated presentation defaults for built-in and default commands.
 */
object StudioDefaults {
    val NAMES = mapOf(
        "fix" to "Fix Grammar",
        "improve" to "Improve",
        "shorten" to "Shorten",
        "expand" to "Expand",
        "formal" to "Formal",
        "casual" to "Casual",
        "emoji" to "Add Emojis",
        "human" to "Humanize",
        "reply" to "Reply",
        "undo" to "Undo",
        "copy" to "Copy",
        "cut" to "Cut",
        "paste" to "Paste",
        "replace" to "Replace"
    )

    val CATEGORIES = mapOf(
        "fix" to CommandCategory.WRITING,
        "improve" to CommandCategory.WRITING,
        "shorten" to CommandCategory.WRITING,
        "expand" to CommandCategory.WRITING,
        "formal" to CommandCategory.WRITING,
        "casual" to CommandCategory.WRITING,
        "emoji" to CommandCategory.SOCIAL,
        "human" to CommandCategory.WRITING,
        "reply" to CommandCategory.SOCIAL,
        "undo" to CommandCategory.PRODUCTIVITY,
        "copy" to CommandCategory.PRODUCTIVITY,
        "cut" to CommandCategory.PRODUCTIVITY,
        "paste" to CommandCategory.PRODUCTIVITY,
        "replace" to CommandCategory.PRODUCTIVITY
    )

    fun displayName(trigger: String): String =
        trigger.drop(1).replaceFirstChar { it.uppercase() }.ifBlank { trigger }

    fun categoryFor(command: Command): CommandCategory {
        val key = RichCommand.keyFor(command.trigger).substringBefore(':')
        if (key == "translate" || command.trigger.contains("translate:")) {
            return CommandCategory.TRANSLATION
        }
        return CATEGORIES[key] ?: CommandCategory.CUSTOM
    }

    fun nameFor(command: Command): String {
        val key = RichCommand.keyFor(command.trigger)
        if (key.startsWith("translate:")) {
            val lang = key.substringAfter("translate:").uppercase()
            return "Translate ($lang)"
        }
        if (key == "translate") return "Translate"
        return NAMES[key] ?: displayName(command.trigger)
    }
}
