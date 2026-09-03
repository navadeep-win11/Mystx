package com.mystx.app.manager

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.mystx.app.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommandStudioTest {
    private lateinit var context: Application
    private lateinit var commandManager: CommandManager
    private lateinit var store: CommandStudioStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("commands", 0).edit().clear().commit()
        context.getSharedPreferences("settings", 0).edit().clear().commit()
        context.getSharedPreferences("command_studio", 0).edit().clear().commit()

        commandManager = CommandManager(context)
        store = CommandStudioStore(context, commandManager)
    }

    // 1. Command creation
    @Test
    fun testCommandCreation() {
        val newCmd = RichCommand(
            id = "summary",
            name = "Summarize Text",
            trigger = "?summary",
            category = CommandCategory.PRODUCTIVITY,
            promptTemplate = "Summarize the following:\n\n{text}",
            modelOverride = null,
            temperature = 0.3f,
            enabled = true,
            isBuiltIn = false
        )
        val savedTrigger = store.saveCustom(newCmd)
        assertEquals("?summary", savedTrigger)

        val all = store.getRichCommands()
        val found = all.find { it.trigger == "?summary" }
        assertNotNull(found)
        assertEquals("Summarize Text", found!!.name)
        assertEquals(CommandCategory.PRODUCTIVITY, found.category)
        assertEquals("Summarize the following:\n\n{text}", found.promptTemplate)
        assertEquals(0.3f, found.temperature)
        assertTrue(found.enabled)
        assertFalse(found.isBuiltIn)
    }

    // 2. Command persistence
    @Test
    fun testCommandPersistence() {
        val custom = RichCommand(
            id = "testpersist",
            name = "Persistent Command",
            trigger = "?testpersist",
            category = CommandCategory.CODING,
            promptTemplate = "Format as code:\n\n{text}",
            modelOverride = "gemini-3.6-flash",
            temperature = 0.7f,
            enabled = true,
            isBuiltIn = false
        )
        store.saveCustom(custom)

        // Instantiate a new store from the same underlying context/SharedPreferences
        val newStore = CommandStudioStore(context, CommandManager(context))
        val reloaded = newStore.getRichCommands().find { it.trigger == "?testpersist" }
        assertNotNull(reloaded)
        assertEquals("Persistent Command", reloaded!!.name)
        assertEquals(CommandCategory.CODING, reloaded.category)
        assertEquals("gemini-3.6-flash", reloaded.modelOverride)
        assertEquals(0.7f, reloaded.temperature)
        assertTrue(reloaded.enabled)
    }

    // 3. Duplicate trigger validation
    @Test
    fun testDuplicateTriggerValidation() {
        // Built-in ?fix exists
        assertTrue(store.isTriggerTaken("?fix", excludingId = null))
        assertFalse(store.isTriggerTaken("?fix", excludingId = "fix"))
        assertFalse(store.isTriggerTaken("?uniquebrandnew", excludingId = null))

        // Create a custom command and verify duplicate detection
        val custom = RichCommand(
            id = "mycmd",
            name = "My Cmd",
            trigger = "?mycmd",
            category = CommandCategory.CUSTOM,
            promptTemplate = "Hello {text}",
            modelOverride = null,
            temperature = null,
            enabled = true,
            isBuiltIn = false
        )
        store.saveCustom(custom)

        assertTrue(store.isTriggerTaken("?mycmd", excludingId = null))
        assertFalse(store.isTriggerTaken("?mycmd", excludingId = "mycmd"))
    }

    // 4. Trigger parsing
    @Test
    fun testTriggerParsing() {
        assertEquals("es", PromptPlaceholders.languageFromTrigger("?translate:es"))
        assertEquals("fr", PromptPlaceholders.languageFromTrigger("?translate:fr"))
        assertEquals("pt-br", PromptPlaceholders.languageFromTrigger("?translate:pt-br"))
        assertNull(PromptPlaceholders.languageFromTrigger("?translate"))
        assertNull(PromptPlaceholders.languageFromTrigger("?fix"))
        assertNull(PromptPlaceholders.languageFromTrigger("?translate:"))
    }

    // 5. Placeholder replacement
    @Test
    fun testPlaceholderReplacement() {
        val template = "Translate to {language} in {tone} tone for {app}:\n\n{text}"
        val ctx = PromptPlaceholders.Context(
            text = "Hello world",
            language = "Spanish",
            tone = "Polite",
            instruction = "Be concise",
            app = "com.whatsapp"
        )
        val rendered = PromptPlaceholders.render(template, ctx)
        assertEquals("Translate to Spanish in Polite tone for com.whatsapp:\n\nHello world", rendered)

        // Missing optional placeholders safely resolve to empty string
        val ctxMissing = PromptPlaceholders.Context(text = "Just text")
        val renderedMissing = PromptPlaceholders.render(template, ctxMissing)
        assertTrue(renderedMissing.contains("Just text"))
        assertFalse(renderedMissing.contains("{language}"))
        assertFalse(renderedMissing.contains("{tone}"))
        assertFalse(renderedMissing.contains("{app}"))
    }

    // 6. {text} replacement & validation
    @Test
    fun testTextReplacement() {
        val template = "Rewrite: {text}"
        val rendered = PromptPlaceholders.render(template, PromptPlaceholders.Context(text = "sample input"))
        assertEquals("Rewrite: sample input", rendered)

        // Template mode requires {text}
        assertTrue(PromptPlaceholders.isTemplateMode("Translate to {language}:"))
        assertTrue(PromptPlaceholders.requiresText("Translate to {language}:"))
        assertFalse(PromptPlaceholders.requiresText("Translate to {language}:\n{text}"))
        // Legacy template without variables does not require {text}
        assertFalse(PromptPlaceholders.isTemplateMode("Fix grammar and spelling."))
        assertFalse(PromptPlaceholders.requiresText("Fix grammar and spelling."))
    }

    // 7. Model override resolution
    @Test
    fun testModelOverrideResolution() {
        val custom = RichCommand(
            id = "code",
            name = "Coding Model",
            trigger = "?code",
            category = CommandCategory.CODING,
            promptTemplate = "Write code:\n\n{text}",
            modelOverride = "llama-3.3-70b-versatile",
            temperature = 0.1f,
            enabled = true,
            isBuiltIn = false
        )
        store.saveCustom(custom)
        val fetched = store.getRichCommands().first { it.trigger == "?code" }
        assertEquals("llama-3.3-70b-versatile", fetched.modelOverride)
    }

    // 8. Global model fallback
    @Test
    fun testGlobalModelFallback() {
        val custom = RichCommand(
            id = "fallbackcmd",
            name = "Fallback Command",
            trigger = "?fallback",
            category = CommandCategory.WRITING,
            promptTemplate = "Improve:\n\n{text}",
            modelOverride = null,
            temperature = null,
            enabled = true,
            isBuiltIn = false
        )
        store.saveCustom(custom)
        val fetched = store.getRichCommands().first { it.trigger == "?fallback" }
        assertNull(fetched.modelOverride)
        assertNull(fetched.temperature)
    }

    // 9. Temperature configuration
    @Test
    fun testTemperatureConfiguration() {
        val custom = RichCommand(
            id = "temp",
            name = "Temp Command",
            trigger = "?temp",
            category = CommandCategory.SOCIAL,
            promptTemplate = "Funny: {text}",
            modelOverride = null,
            temperature = 1.8f,
            enabled = true,
            isBuiltIn = false
        )
        store.saveCustom(custom)
        val fetched = store.getRichCommands().first { it.trigger == "?temp" }
        assertNotNull(fetched.temperature)
        assertEquals(1.8f, fetched.temperature!!, 0.001f)
    }

    // 10. Enabled/disabled commands
    @Test
    fun testEnabledDisabledCommands() {
        val custom = RichCommand(
            id = "toggle",
            name = "Toggle Command",
            trigger = "?toggle",
            category = CommandCategory.CUSTOM,
            promptTemplate = "{text}",
            modelOverride = null,
            temperature = null,
            enabled = true,
            isBuiltIn = false
        )
        store.saveCustom(custom)
        assertTrue(store.getRichCommands().first { it.trigger == "?toggle" }.enabled)

        store.setEnabled("toggle", false)
        assertFalse(store.getRichCommands().first { it.trigger == "?toggle" }.enabled)

        store.setEnabled("toggle", true)
        assertTrue(store.getRichCommands().first { it.trigger == "?toggle" }.enabled)
    }

    // 11. Existing command compatibility
    @Test
    fun testExistingCommandCompatibility() {
        val all = store.getRichCommands()
        val fix = all.find { it.trigger == "?fix" }
        assertNotNull(fix)
        assertEquals("Fix Grammar", fix!!.name)
        assertEquals(CommandCategory.WRITING, fix.category)
        assertTrue(fix.enabled)
        // Default AI commands are editable/customizable, system commands are builtIn
        assertFalse(fix.isBuiltIn)

        val undo = all.find { it.trigger == "?undo" }
        assertNotNull(undo)
        assertTrue(undo!!.isBuiltIn)

        // Verify findRich for text containing ?fix
        val matched = store.findRich("Please correct this?fix")
        assertNotNull(matched)
        assertEquals("?fix", matched!!.trigger)
        assertEquals("Fix Grammar", matched.name)

        // Dynamic translate trigger
        val translateMatched = store.findRich("hello?translate:es")
        assertNotNull(translateMatched)
        assertEquals("?translate:es", translateMatched!!.trigger)
        assertEquals("Translate (ES)", translateMatched.name)
        assertEquals(CommandCategory.TRANSLATION, translateMatched.category)
    }

    // 12. Command deletion
    @Test
    fun testCommandDeletion() {
        val custom = RichCommand(
            id = "delete",
            name = "Delete Me",
            trigger = "?delete",
            category = CommandCategory.CUSTOM,
            promptTemplate = "Delete {text}",
            modelOverride = null,
            temperature = null,
            enabled = true,
            isBuiltIn = false
        )
        store.saveCustom(custom)
        assertNotNull(store.getRichCommands().find { it.trigger == "?delete" })

        // Deleting custom command succeeds
        val deleted = store.deleteCustom("delete")
        assertTrue(deleted)
        assertNull(store.getRichCommands().find { it.trigger == "?delete" })

        // Built-in commands cannot be deleted
        val deleteBuiltIn = store.deleteCustom("undo")
        assertFalse(deleteBuiltIn)
        assertNotNull(store.getRichCommands().find { it.trigger == "?undo" })
    }

    // 13. Search/filtering
    @Test
    fun testSearchAndFiltering() {
        val all = store.getRichCommands()

        // Filter by category
        val writingOnly = store.filter(all, "", CommandCategory.WRITING)
        assertTrue(writingOnly.isNotEmpty())
        assertTrue(writingOnly.all { it.category == CommandCategory.WRITING })

        // Filter by query (name)
        val fixQuery = store.filter(all, "grammar", null)
        assertTrue(fixQuery.any { it.trigger == "?fix" })

        // Filter by trigger query
        val undoQuery = store.filter(all, "?undo", null)
        assertEquals(1, undoQuery.size)
        assertEquals("?undo", undoQuery.first().trigger)

        // No matches
        val noMatches = store.filter(all, "nonexistentcommandxyz", null)
        assertTrue(noMatches.isEmpty())
    }
}
