package com.mystx.app.ui.studio

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystx.app.R
import com.mystx.app.api.GeminiClient
import com.mystx.app.api.OpenAICompatibleClient
import com.mystx.app.manager.CommandManager
import com.mystx.app.manager.CommandStudioStore
import com.mystx.app.manager.KeyManager
import com.mystx.app.model.*
import com.mystx.app.provider.Providers
import com.mystx.app.service.CommandOutcome
import com.mystx.app.service.runTextCommand
import com.mystx.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandEditorScreen(
    initialCommand: RichCommand?,
    store: CommandStudioStore,
    keyManager: KeyManager,
    geminiClient: GeminiClient,
    openAIClient: OpenAICompatibleClient,
    prefs: SharedPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isEditing = initialCommand != null
    val isBuiltIn = initialCommand?.isBuiltIn == true
    val prefix = store.commandManager.getTriggerPrefix()

    // Localized validation strings
    val errorTriggerEmpty = stringResource(R.string.command_studio_error_trigger_empty)
    val errorTriggerPrefix = stringResource(R.string.command_studio_error_trigger_prefix, prefix)
    val errorEmptyTrigger = stringResource(R.string.commands_error_empty_trigger)
    val errorTriggerDuplicate = stringResource(R.string.command_studio_error_trigger_duplicate)
    val errorNameEmpty = stringResource(R.string.command_studio_error_name_empty)
    val errorPromptEmpty = stringResource(R.string.command_studio_error_prompt_empty)
    val errorMissingText = stringResource(R.string.command_studio_error_missing_text)
    val errorSafetyBlocked = stringResource(R.string.error_safety_blocked)

    // Form fields
    var name by rememberSaveable { mutableStateOf(initialCommand?.name ?: "") }
    var trigger by rememberSaveable { mutableStateOf(initialCommand?.trigger ?: prefix) }
    var category by rememberSaveable { mutableStateOf(initialCommand?.category ?: CommandCategory.CUSTOM) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var modelOverride by rememberSaveable { mutableStateOf(initialCommand?.modelOverride ?: "") }
    var modelExpanded by remember { mutableStateOf(false) }

    val globalTemp = prefs.getFloat(PrefKeys.TEMPERATURE, 0.5f)
    var temperature by rememberSaveable { mutableStateOf(initialCommand?.temperature ?: globalTemp) }
    var useCustomTemp by rememberSaveable { mutableStateOf(initialCommand?.temperature != null) }

    val initialPrompt = initialCommand?.promptTemplate ?: "Rewrite the following text:\n\n{text}"
    var promptFieldValue by remember { mutableStateOf(TextFieldValue(initialPrompt)) }

    var enabled by rememberSaveable { mutableStateOf(initialCommand?.enabled ?: true) }

    // Validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var triggerError by remember { mutableStateOf<String?>(null) }
    var promptError by remember { mutableStateOf<String?>(null) }

    // Dialog state
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Test runner state
    var sampleText by rememberSaveable { mutableStateOf("The quick brown fox jumps over the lazy dog.") }
    var isTesting by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testResultIsError by remember { mutableStateOf(false) }

    // Models options based on current provider
    val provider = remember { Providers.forType(prefs.getString(PrefKeys.PROVIDER_TYPE, null)) }
    val availableModels: List<Pair<String, String>> = remember(provider) {
        when (provider.type) {
            ProviderType.GEMINI -> GeminiModels.OPTIONS
            ProviderType.GROQ -> GroqModels.OPTIONS
            ProviderType.BAI -> BaiModels.OPTIONS
            else -> {
                val custom = prefs.getString(PrefKeys.CUSTOM_MODEL, "") ?: ""
                if (custom.isNotEmpty()) listOf(custom to custom) else emptyList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
            .padding(bottom = 112.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MystIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.command_studio_title).uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = if (isEditing) stringResource(R.string.command_studio_edit_command)
                           else stringResource(R.string.command_studio_create_title),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Card 1: Command Basics (Name, Trigger, Category, Enabled)
        MystCard {
            // Command Name
            Text(
                text = stringResource(R.string.command_studio_name_label),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            MystTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                placeholder = { Text(stringResource(R.string.command_studio_name_hint)) },
                isError = nameError != null
            )
            nameError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trigger
            Text(
                text = stringResource(R.string.command_studio_trigger_label, prefix),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            MystTextField(
                value = trigger,
                onValueChange = { input ->
                    val clean = input.take(CommandManager.MAX_TRIGGER_LENGTH)
                    trigger = clean
                    triggerError = when {
                        clean.isBlank() -> errorTriggerEmpty
                        !clean.startsWith(prefix) -> errorTriggerPrefix
                        clean.length <= prefix.length -> errorEmptyTrigger
                        store.isTriggerTaken(clean, excludingId = initialCommand?.id) -> errorTriggerDuplicate
                        else -> null
                    }
                },
                readOnly = isBuiltIn, // Built-in triggers like ?fix or ?undo cannot change triggers
                placeholder = { Text("${prefix}custom") },
                isError = triggerError != null
            )
            triggerError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Dropdown
            Text(
                text = stringResource(R.string.command_studio_category_label),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                MystTextField(
                    value = category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    CommandCategory.EDITABLE_CATEGORIES.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.displayName) },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Enabled Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.command_studio_enabled_label),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        enabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card 2: AI Model & Temperature
        MystCard {
            Text(
                text = stringResource(R.string.command_studio_model_label),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            ExposedDropdownMenuBox(
                expanded = modelExpanded,
                onExpandedChange = { modelExpanded = !modelExpanded }
            ) {
                val currentModelDisplay = if (modelOverride.isBlank() || modelOverride.equals("Global", ignoreCase = true)) {
                    stringResource(R.string.command_studio_model_global)
                } else {
                    availableModels.firstOrNull { it.first == modelOverride }?.second ?: modelOverride
                }
                MystTextField(
                    value = currentModelDisplay,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    expanded = modelExpanded,
                    onDismissRequest = { modelExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.command_studio_model_global)) },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            modelOverride = ""
                            modelExpanded = false
                        }
                    )
                    availableModels.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                modelOverride = id
                                modelExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Temperature Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.command_studio_temperature_label),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", temperature),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = temperature,
                onValueChange = {
                    val newVal = Math.round(it * 10) / 10f
                    if (newVal != temperature) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        temperature = newVal
                        useCustomTemp = true
                    }
                },
                valueRange = 0f..2f,
                steps = 19,
                modifier = Modifier.fillMaxWidth().height(26.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card 3: Prompt Template & Placeholders
        MystCard {
            Text(
                text = stringResource(R.string.command_studio_prompt_label),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Variable insertion helper chips
            Text(
                text = stringResource(R.string.command_studio_insert_variable) + ":",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PromptPlaceholders.ALL.forEach { placeholder ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val token = "{$placeholder}"
                                val sel = promptFieldValue.selection
                                val text = promptFieldValue.text
                                val newText = text.replaceRange(sel.start, sel.end, token)
                                val newCursor = sel.start + token.length
                                promptFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newCursor)
                                )
                                promptError = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ {$placeholder}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = promptFieldValue,
                onValueChange = {
                    promptFieldValue = it.copy(text = it.text.take(CommandManager.MAX_PROMPT_LENGTH))
                    promptError = null
                },
                placeholder = { Text(stringResource(R.string.command_studio_prompt_hint)) },
                shape = MystCompactShape,
                isError = promptError != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 220.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            promptError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card 4: Test Command
        MystCard {
            Text(
                text = stringResource(R.string.command_studio_test_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.command_studio_test_input_label),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            MystTextField(
                value = sampleText,
                onValueChange = { sampleText = it },
                singleLine = false,
                modifier = Modifier.heightIn(min = 60.dp, max = 100.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Generated Prompt Preview
            val renderedPreview = remember(promptFieldValue.text, sampleText, trigger) {
                val lang = PromptPlaceholders.languageFromTrigger(trigger)
                val testCtx = PromptPlaceholders.Context(
                    text = sampleText,
                    language = lang,
                    tone = "Friendly",
                    instruction = "Sample instruction",
                    app = "com.mystx.test"
                )
                PromptPlaceholders.render(promptFieldValue.text, testCtx)
            }

            Text(
                text = stringResource(R.string.command_studio_test_preview) + ":",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MystCompactShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    text = renderedPreview.ifBlank { "(empty prompt)" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Run Test button
            MystTonalButton(
                text = if (isTesting) stringResource(R.string.command_studio_test_running)
                       else stringResource(R.string.command_studio_test_run),
                onClick = {
                    if (isTesting) return@MystTonalButton
                    isTesting = true
                    testResultText = null
                    testResultIsError = false

                    scope.launch {
                        val outcome = withContext(Dispatchers.IO) {
                            runTextCommand(
                                context = context,
                                keyManager = keyManager,
                                geminiClient = geminiClient,
                                openAIClient = openAIClient,
                                prompt = renderedPreview,
                                text = sampleText,
                                modelOverride = modelOverride.ifBlank { null },
                                temperatureOverride = temperature
                            )
                        }
                        isTesting = false
                        when (outcome) {
                            is CommandOutcome.Success -> {
                                testResultText = outcome.text
                                testResultIsError = false
                            }
                            is CommandOutcome.Refusal -> {
                                testResultText = errorSafetyBlocked
                                testResultIsError = true
                            }
                            is CommandOutcome.Unavailable -> {
                                testResultText = outcome.message
                                testResultIsError = true
                            }
                            is CommandOutcome.Failure -> {
                                testResultText = outcome.message
                                testResultIsError = true
                            }
                        }
                    }
                },
                enabled = !isTesting && promptFieldValue.text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            testResultText?.let { result ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.command_studio_test_result_label) + ":",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (testResultIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MystCompactShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (testResultIsError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = result,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Save, Delete, Reset
        val promptText = promptFieldValue.text.trim()
        MystGradientButton(
            text = stringResource(R.string.command_studio_save),
            onClick = {
                // Validation
                var hasError = false
                val trimmedName = name.trim()
                val trimmedTrigger = trigger.trim()

                if (trimmedName.isBlank()) {
                    nameError = errorNameEmpty
                    hasError = true
                }
                if (trimmedTrigger.isBlank()) {
                    triggerError = errorTriggerEmpty
                    hasError = true
                } else if (!trimmedTrigger.startsWith(prefix)) {
                    triggerError = errorTriggerPrefix
                    hasError = true
                } else if (trimmedTrigger.length <= prefix.length) {
                    triggerError = errorEmptyTrigger
                    hasError = true
                } else if (store.isTriggerTaken(trimmedTrigger, excludingId = initialCommand?.id)) {
                    triggerError = errorTriggerDuplicate
                    hasError = true
                }

                if (promptText.isBlank()) {
                    promptError = errorPromptEmpty
                    hasError = true
                } else if (PromptPlaceholders.requiresText(promptText)) {
                    promptError = errorMissingText
                    hasError = true
                }

                if (hasError) return@MystGradientButton

                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                val modelVal = if (modelOverride.isBlank() || modelOverride.equals("Global", ignoreCase = true)) null else modelOverride
                val tempVal = if (useCustomTemp) temperature else null

                if (isBuiltIn) {
                    // Built-in command: persist meta overrides
                    val updated = initialCommand!!.copy(
                        name = trimmedName,
                        category = category,
                        promptTemplate = promptText,
                        modelOverride = modelVal,
                        temperature = tempVal,
                        enabled = enabled
                    )
                    store.saveMeta(updated)
                    onBack()
                } else {
                    // Custom command: save command and meta
                    val commandToSave = RichCommand(
                        id = RichCommand.keyFor(trimmedTrigger),
                        name = trimmedName,
                        trigger = trimmedTrigger,
                        category = category,
                        promptTemplate = promptText,
                        modelOverride = modelVal,
                        temperature = tempVal,
                        enabled = enabled,
                        isBuiltIn = false,
                        type = initialCommand?.type ?: CommandType.AI
                    )
                    val initialTrig = initialCommand?.trigger ?: trimmedTrigger
                    val ok = store.saveCustom(commandToSave, replacing = initialTrig)
                    if (ok != null) {
                        onBack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (isEditing) {
            Spacer(modifier = Modifier.height(10.dp))
            if (!isBuiltIn) {
                MystTonalButton(
                    text = stringResource(R.string.command_studio_delete),
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                MystTonalButton(
                    text = stringResource(R.string.command_studio_reset),
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm && initialCommand != null) {
        MystDialog(
            title = stringResource(R.string.command_studio_delete_confirm_title),
            message = stringResource(R.string.command_studio_delete_confirm_msg),
            confirmLabel = stringResource(R.string.command_studio_delete),
            dismissLabel = stringResource(R.string.commands_cancel),
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                store.deleteCustom(initialCommand.id)
                showDeleteConfirm = false
                onBack()
            },
            onDismissRequest = { showDeleteConfirm = false }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirm && initialCommand != null) {
        MystDialog(
            title = stringResource(R.string.command_studio_reset_confirm_title),
            message = stringResource(R.string.command_studio_reset_confirm_msg),
            confirmLabel = stringResource(R.string.command_studio_reset),
            dismissLabel = stringResource(R.string.commands_cancel),
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                store.resetBuiltIn(initialCommand.id)
                showResetConfirm = false
                onBack()
            },
            onDismissRequest = { showResetConfirm = false }
        )
    }
}
