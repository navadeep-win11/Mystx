package com.mystx.app.manager

import android.content.Context
import android.content.SharedPreferences
import com.mystx.app.model.Command
import com.mystx.app.model.CommandCategory
import com.mystx.app.model.CommandMeta
import com.mystx.app.model.RichCommand
import com.mystx.app.model.StudioDefaults

/**
 * Persistence and state management layer for Command Studio.
 *
 * Wraps [CommandManager] and stores per-command Studio metadata (name, category,
 * model/temperature overrides, enabled state) in SharedPreferences.
 */
class CommandStudioStore(
    context: Context,
    val commandManager: CommandManager
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("command_studio", Context.MODE_PRIVATE)

    companion object {
        const val MIN_TEMPERATURE = 0f
        const val MAX_TEMPERATURE = 2f

        enum class TriggerError { NONE, EMPTY, NOT_PREFIXED, TOO_SHORT, TOO_LONG }

        fun validateTrigger(trigger: String, prefix: String): TriggerError = when {
            trigger.isBlank() -> TriggerError.EMPTY
            !trigger.startsWith(prefix) -> TriggerError.NOT_PREFIXED
            trigger.length <= prefix.length -> TriggerError.TOO_SHORT
            trigger.length > CommandManager.MAX_TRIGGER_LENGTH -> TriggerError.TOO_LONG
            else -> TriggerError.NONE
        }
    }

    /**
     * All commands merged with their persisted Command Studio metadata.
     */
    fun getRichCommands(): List<RichCommand> {
        val raw = commandManager.getCommands()
        val result = raw.map { cmd ->
            getRichForCommand(cmd)
        }
        return result.sortedByDescending { it.trigger.length }
    }

    /**
     * Converts a raw [Command] into a [RichCommand], applying stored metadata if present.
     */
    fun getRichForCommand(cmd: Command): RichCommand {
        val metaKey = metaKeyFor(RichCommand.keyFor(cmd.trigger))
        val meta = prefs.getString(metaKey, null)?.let { CommandMeta.fromJson(it) }
        val base = defaultRich(cmd)
        return if (meta == null) base else base.withMeta(meta)
    }

    private fun defaultRich(cmd: Command): RichCommand {
        val base = RichCommand.fromCommand(cmd)
        return if (cmd.isBuiltIn) base.copy(
            name = StudioDefaults.nameFor(cmd),
            category = StudioDefaults.categoryFor(cmd)
        ) else base
    }

    /**
     * Finds a matching command for the input text, resolving metadata and overrides.
     */
    fun findRich(text: String): RichCommand? {
        val cmd = commandManager.findCommand(text) ?: return null
        return getRichForCommand(cmd)
    }

    fun saveMeta(command: RichCommand) {
        prefs.edit().putString(metaKeyFor(command.id), command.toMeta().toJson()).apply()
    }

    fun removeMeta(id: String) {
        prefs.edit().remove(metaKeyFor(id)).apply()
    }

    private fun metaKeyFor(id: String) = "meta_$id"

    /**
     * Creates or updates a custom command.
     * Returns the stored trigger on success, or null on validation/storage failure.
     */
    fun saveCustom(command: RichCommand, replacing: String = command.trigger): String? {
        val prompt = command.promptTemplate
        val prefix = commandManager.getTriggerPrefix()
        if (!CommandManager.isValidCommand(command.trigger, prompt, prefix)) {
            return null
        }
        val stored = Command(command.trigger, prompt, false, command.type)
        val ok = commandManager.saveCustomCommand(stored, replacing)
        if (ok) {
            val oldId = RichCommand.keyFor(replacing)
            if (oldId.isNotEmpty() && oldId != command.id) {
                removeMeta(oldId)
            }
            saveMeta(command)
        }
        return if (ok) command.trigger else null
    }

    /**
     * Checks whether a trigger is already taken by another command.
     */
    fun isTriggerTaken(trigger: String, excludingId: String?): Boolean =
        getRichCommands().any { it.id != excludingId && it.trigger.equals(trigger, ignoreCase = false) }

    /**
     * Deletes a custom command. Built-ins are protected and cannot be deleted.
     */
    fun deleteCustom(id: String): Boolean {
        val target = getRichCommands().firstOrNull { it.id == id } ?: return false
        if (target.isBuiltIn) return false
        commandManager.removeCustomCommand(target.trigger)
        removeMeta(id)
        return true
    }

    /**
     * Restores a built-in command to its default presentation and behavior.
     */
    fun resetBuiltIn(id: String): Boolean {
        val target = getRichCommands().firstOrNull { it.id == id } ?: return false
        if (!target.isBuiltIn) return false
        removeMeta(id)
        return true
    }

    /**
     * Toggles whether a command is active.
     */
    fun setEnabled(id: String, enabled: Boolean): Boolean {
        val target = getRichCommands().firstOrNull { it.id == id } ?: return false
        saveMeta(target.copy(enabled = enabled))
        return true
    }

    /**
     * Pure search and category filter.
     */
    fun filter(
        commands: List<RichCommand>,
        query: String,
        category: CommandCategory?
    ): List<RichCommand> {
        val q = query.trim().lowercase()
        return commands.filter { cmd ->
            val matchCategory = category == null || category == CommandCategory.ALL || cmd.category == category
            val matchQuery = q.isEmpty() ||
                cmd.name.lowercase().contains(q) ||
                cmd.trigger.lowercase().contains(q) ||
                cmd.category.name.lowercase().contains(q) ||
                cmd.category.displayName.lowercase().contains(q)
            matchCategory && matchQuery
        }
    }
}
