package com.mystx.app.ui

import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystx.app.BuildConfig
import com.mystx.app.R
import com.mystx.app.api.ApiClientUtils
import com.mystx.app.api.OpenAICompatibleClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mystx.app.manager.CommandManager
import com.mystx.app.manager.KeyManager
import com.mystx.app.model.BaiModels
import com.mystx.app.model.GeminiModels
import com.mystx.app.model.GroqModels
import com.mystx.app.model.PrefKeys
import com.mystx.app.model.ProviderType
import com.mystx.app.provider.EndpointValidator
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mystx.app.ui.components.MystDialog
import com.mystx.app.ui.components.MystGradientButton
import com.mystx.app.ui.components.MystTonalButton
import com.mystx.app.ui.components.ScreenTitle
import com.mystx.app.ui.components.MystCard
import com.mystx.app.ui.components.MystDivider
import com.mystx.app.ui.components.MystTextField

import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    commandManager: CommandManager,
    prefs: SharedPreferences,
    keyManager: KeyManager,
    onNavigateToCommandStudio: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current

    val scope = rememberCoroutineScope()
    var saveEndpointJob by remember { mutableStateOf<Job?>(null) }
    var saveModelJob by remember { mutableStateOf<Job?>(null) }

    var providerType by remember { mutableStateOf(prefs.getString(PrefKeys.PROVIDER_TYPE, ProviderType.GEMINI) ?: ProviderType.GEMINI) }
    var providerExpanded by remember { mutableStateOf(false) }

    var selectedModel by remember { mutableStateOf(GeminiModels.sanitize(prefs.getString(PrefKeys.GEMINI_MODEL, GeminiModels.DEFAULT))) }
    var modelExpanded by remember { mutableStateOf(false) }
    val geminiModels = GeminiModels.OPTIONS

    var groqModel by remember { mutableStateOf(GroqModels.sanitize(prefs.getString(PrefKeys.GROQ_MODEL, GroqModels.DEFAULT))) }
    var groqModelExpanded by remember { mutableStateOf(false) }
    val groqModels = GroqModels.OPTIONS

    var baiModel by remember { mutableStateOf(BaiModels.sanitize(prefs.getString(PrefKeys.BAI_MODEL, BaiModels.DEFAULT))) }
    var baiModelExpanded by remember { mutableStateOf(false) }
    val baiModels = BaiModels.OPTIONS

    var customEndpoint by rememberSaveable { mutableStateOf(prefs.getString(PrefKeys.CUSTOM_ENDPOINT, "") ?: "") }
    var customModel by rememberSaveable { mutableStateOf(prefs.getString(PrefKeys.CUSTOM_MODEL, "") ?: "") }
    var endpointError by remember { mutableStateOf<String?>(null) }
    // Fetched model ids for the Custom provider dropdown. Session state only — refetched on
    // demand, never persisted (the stored pref stays the plain custom_model string).
    var customModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var customModelExpanded by remember { mutableStateOf(false) }
    var isFetchingModels by remember { mutableStateOf(false) }
    var fetchMessage by remember { mutableStateOf<String?>(null) }
    var fetchSuccess by remember { mutableStateOf(false) }
    var apiKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    val openAIClient = remember { OpenAICompatibleClient() }

    var triggerPrefix by remember { mutableStateOf(commandManager.getTriggerPrefix()) }
    var prefixError by remember { mutableStateOf<String?>(null) }
    var temperature by remember { mutableStateOf(prefs.getFloat(PrefKeys.TEMPERATURE, 0.5f)) }

    val prefixErrorLength = stringResource(R.string.settings_prefix_error_length)
    val prefixErrorWhitespace = stringResource(R.string.settings_prefix_error_whitespace)
    val prefixErrorAlphanumeric = stringResource(R.string.settings_prefix_error_alphanumeric)
    val endpointErrorScheme = stringResource(R.string.settings_endpoint_error_scheme)
    val endpointErrorSpaces = stringResource(R.string.settings_endpoint_error_spaces)
    val fetchModelsMsg = stringResource(R.string.settings_fetch_models)
    val fetchingModelsMsg = stringResource(R.string.settings_fetch_models_loading)
    val modelsLoadedMsg = stringResource(R.string.settings_fetch_models_success)
    val modelsEmptyMsg = stringResource(R.string.settings_fetch_models_empty)
    val modelsFailedMsg = stringResource(R.string.settings_fetch_models_failed)
    val signinRequiredMsg = stringResource(R.string.error_provider_auth_required)

    // Registered keys are decrypted through the Keystore — load off the main thread, as
    // KeysScreen does. The first key is sent as Bearer when fetching models; keyless local
    // servers get no header at all.
    LaunchedEffect(Unit) {
        apiKeys = withContext(Dispatchers.IO) { keyManager.getKeys() }
    }

    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupSuccess by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            saveEndpointJob?.cancel()
            saveModelJob?.cancel()
            val editor = prefs.edit()
            var needsWrite = false
            if (customEndpoint != (prefs.getString(PrefKeys.CUSTOM_ENDPOINT, "") ?: "")) {
                val isValid = customEndpoint.isBlank() ||
                    EndpointValidator.validate(customEndpoint) == EndpointValidator.Error.NONE
                if (isValid) {
                    editor.putString(PrefKeys.CUSTOM_ENDPOINT, customEndpoint)
                    needsWrite = true
                }
            }
            if (customModel != (prefs.getString(PrefKeys.CUSTOM_MODEL, "") ?: "")) {
                editor.putString(PrefKeys.CUSTOM_MODEL, customModel)
                needsWrite = true
            }
            if (needsWrite) editor.apply()
        }
    }
    val exportSuccessMsg = stringResource(R.string.backup_export_success)
    val exportErrorMsg = stringResource(R.string.backup_export_error)
    val importSuccessMsg = stringResource(R.string.backup_import_success)
    val importErrorMsg = stringResource(R.string.backup_import_error)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(it)?.use { os ->
                            os.write(commandManager.exportCommands().toByteArray())
                        }
                    }
                    backupMessage = exportSuccessMsg
                    backupSuccess = true
                } catch (_: Exception) {
                    backupMessage = exportErrorMsg
                    backupSuccess = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                            val text = reader.readText().removePrefix("\uFEFF")
                            if (text.length > 1_000_000) null else text
                        } ?: ""
                    }
                    if (commandManager.importCommands(json)) {
                        backupMessage = importSuccessMsg
                        backupSuccess = true
                    } else {
                        backupMessage = importErrorMsg
                        backupSuccess = false
                    }
                } catch (_: Exception) {
                    backupMessage = importErrorMsg
                    backupSuccess = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 16.dp).padding(bottom = 112.dp)
    ) {
        ScreenTitle(stringResource(R.string.settings_title))

        // Command Studio Entry Card
        MystCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToCommandStudio()
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.command_studio_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.command_studio_desc),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.command_studio_title),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Card 1: Provider + Model
        MystCard {
            Text(
                text = stringResource(R.string.settings_provider_title),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = !providerExpanded }
            ) {
                MystTextField(
                    value = when (providerType) {
                        ProviderType.GEMINI -> stringResource(R.string.settings_provider_gemini)
                        ProviderType.GROQ -> stringResource(R.string.settings_provider_groq)
                        ProviderType.BAI -> stringResource(R.string.settings_provider_bai)
                        else -> stringResource(R.string.settings_provider_custom)
                    },
                    onValueChange = {},
                    readOnly = true,
                    
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_provider_gemini)) },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            providerType = ProviderType.GEMINI
                            prefs.edit().putString(PrefKeys.PROVIDER_TYPE, ProviderType.GEMINI).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                            providerExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_provider_groq)) },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            providerType = ProviderType.GROQ
                            prefs.edit().putString(PrefKeys.PROVIDER_TYPE, ProviderType.GROQ).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                            providerExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_provider_bai)) },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            providerType = ProviderType.BAI
                            prefs.edit().putString(PrefKeys.PROVIDER_TYPE, ProviderType.BAI).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                            providerExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_provider_custom)) },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            providerType = ProviderType.CUSTOM
                            prefs.edit().putString(PrefKeys.PROVIDER_TYPE, ProviderType.CUSTOM).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                            providerExpanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (providerType == ProviderType.GEMINI) {
                Text(
                    text = stringResource(R.string.settings_model_title),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = !modelExpanded }
                ) {
                    MystTextField(
                        value = GeminiModels.label(selectedModel),
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
                        geminiModels.forEach { (id, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedModel = id
                                    prefs.edit().putString(PrefKeys.GEMINI_MODEL, id).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            } else if (providerType == ProviderType.BAI) {
                Text(
                    text = stringResource(R.string.settings_model_title),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = baiModelExpanded,
                    onExpandedChange = { baiModelExpanded = !baiModelExpanded }
                ) {
                    MystTextField(
                        value = BaiModels.label(baiModel),
                        onValueChange = {},
                        readOnly = true,

                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        expanded = baiModelExpanded,
                        onDismissRequest = { baiModelExpanded = false }
                    ) {
                        baiModels.forEach { (id, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    baiModel = id
                                    prefs.edit().putString(PrefKeys.BAI_MODEL, id).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                                    baiModelExpanded = false
                                }
                            )
                        }
                    }
                }
            } else if (providerType == ProviderType.GROQ) {
                Text(
                    text = stringResource(R.string.settings_model_title),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = groqModelExpanded,
                    onExpandedChange = { groqModelExpanded = !groqModelExpanded }
                ) {
                    MystTextField(
                        value = GroqModels.label(groqModel),
                        onValueChange = {},
                        readOnly = true,
                        
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        expanded = groqModelExpanded,
                        onDismissRequest = { groqModelExpanded = false }
                    ) {
                        groqModels.forEach { (id, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    groqModel = id
                                    prefs.edit().putString(PrefKeys.GROQ_MODEL, id).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                                    groqModelExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.settings_endpoint_title),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                MystTextField(
                    value = customEndpoint,
                    onValueChange = {
                        customEndpoint = it
                        // The endpoint changed — a previously fetched model list no longer
                        // describes this server.
                        customModels = emptyList()
                        fetchMessage = null
                        endpointError = when {
                            it.isBlank() -> null
                            it.contains(" ") -> endpointErrorSpaces
                            EndpointValidator.validate(it) == EndpointValidator.Error.NONE -> null
                            else -> endpointErrorScheme
                        }
                        if (endpointError == null) {
                            saveEndpointJob?.cancel()
                            saveEndpointJob = scope.launch {
                                delay(500)
                                prefs.edit().putString(PrefKeys.CUSTOM_ENDPOINT, it).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                            }
                        }
                    },
                    placeholder = { Text(stringResource(R.string.settings_endpoint_placeholder)) },
                    
                    isError = endpointError != null
                )
                endpointError?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_model_title),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (customModels.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = customModelExpanded,
                        onExpandedChange = { customModelExpanded = !customModelExpanded }
                    ) {
                        MystTextField(
                            value = customModel,
                            onValueChange = {
                                customModel = it
                                saveModelJob?.cancel()
                                saveModelJob = scope.launch {
                                    delay(500)
                                    prefs.edit().putString(PrefKeys.CUSTOM_MODEL, it).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                                }
                            },
                            placeholder = { Text(stringResource(R.string.settings_model_placeholder)) },
                            
                            // Editable anchor: the fetched list is a convenience, not a
                            // restriction — cloud models and off-list ids stay typeable.
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        )
                        ExposedDropdownMenu(
                            containerColor = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            expanded = customModelExpanded,
                            onDismissRequest = { customModelExpanded = false }
                        ) {
                            customModels.forEach { id ->
                                DropdownMenuItem(
                                    text = { Text(id) },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        customModel = id
                                        // Cancel any pending debounce so a half-typed value
                                        // cannot overwrite the selection 500ms later.
                                        saveModelJob?.cancel()
                                        prefs.edit().putString(PrefKeys.CUSTOM_MODEL, id).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                                        customModelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    MystTextField(
                        value = customModel,
                        onValueChange = {
                            customModel = it
                            saveModelJob?.cancel()
                            saveModelJob = scope.launch {
                                delay(500)
                                prefs.edit().putString(PrefKeys.CUSTOM_MODEL, it).remove(PrefKeys.STRUCTURED_OUTPUT_DISABLED_AT).apply()
                            }
                        },
                        placeholder = { Text(stringResource(R.string.settings_model_placeholder)) },
                        
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    MystGradientButton(
                        text = if (isFetchingModels) fetchingModelsMsg else fetchModelsMsg,
                        onClick = {
                            isFetchingModels = true
                            fetchMessage = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    openAIClient.fetchModels(apiKeys.firstOrNull(), customEndpoint)
                                }
                                isFetchingModels = false
                                result.onSuccess { ids ->
                                    if (ids.isEmpty()) {
                                        customModels = emptyList()
                                        fetchMessage = modelsEmptyMsg
                                        fetchSuccess = false
                                    } else {
                                        customModels = ids
                                        customModelExpanded = false
                                        fetchMessage = String.format(modelsLoadedMsg, ids.size)
                                        fetchSuccess = true
                                    }
                                }.onFailure { e ->
                                    customModels = emptyList()
                                    val raw = e.message ?: ""
                                    fetchMessage = if (raw.contains(ApiClientUtils.SIGNIN_REQUIRED_MARKER)) {
                                        signinRequiredMsg
                                    } else {
                                        modelsFailedMsg
                                    }
                                    fetchSuccess = false
                                }
                            }
                        },
                        enabled = customEndpoint.isNotBlank() && endpointError == null && !isFetchingModels
                    )
                }
                fetchMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = if (fetchSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_temperature_title),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = String.format("%.1f", temperature),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Slider(
                value = temperature,
                onValueChange = {
                    val newVal = Math.round(it * 10) / 10f
                    if (newVal != temperature) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        temperature = newVal
                    }
                },
                onValueChangeFinished = {
                    prefs.edit().putFloat(PrefKeys.TEMPERATURE, temperature).apply()
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
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Card 2: Trigger Prefix
        MystCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_trigger_prefix_desc, triggerPrefix),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(end = 16.dp)
                )
                MystTextField(
                    value = triggerPrefix,
                    onValueChange = { input ->
                        val filtered = input.take(1)
                        triggerPrefix = filtered
                        prefixError = when {
                            filtered.length != 1 -> prefixErrorLength
                            filtered[0].isWhitespace() -> prefixErrorWhitespace
                            filtered[0].isLetterOrDigit() -> prefixErrorAlphanumeric
                            else -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                commandManager.setTriggerPrefix(filtered)
                                null
                            }
                        }
                    },
                    isError = prefixError != null,
                    modifier = Modifier.width(64.dp)
                )
            }
            prefixError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Card 3: Backup
        MystCard {
            Text(
                text = stringResource(R.string.backup_desc),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MystGradientButton(
                    text = stringResource(R.string.backup_export),
                    onClick = {
                        backupMessage = null
                        exportLauncher.launch("mystx-commands.json")
                    },
                    modifier = Modifier.weight(1f)
                )
                MystTonalButton(
                    text = stringResource(R.string.backup_import),
                    onClick = {
                        backupMessage = null
                        showImportConfirm = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            backupMessage?.let { msg ->
                Text(
                    text = msg,
                    color = if (backupSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Card 4: About — version, Instagram. No GitHub link in the UI.
        MystCard {
            Text(
                text = stringResource(R.string.app_name) + " v" + BuildConfig.VERSION_NAME,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            MystDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_made_by),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(interactionSource = null, indication = null) {
                    uriHandler.openUri("https://www.instagram.com/mystx.navadeep?igsi=MWdyNzN4NWFmaTAyaA==")
                }
            )
        }
    }

    if (showImportConfirm) {
        MystDialog(
            title = stringResource(R.string.backup_import),
            message = stringResource(R.string.backup_import_confirm),
            confirmLabel = stringResource(R.string.backup_import),
            dismissLabel = stringResource(R.string.backup_import_cancel),
            onConfirm = {
                showImportConfirm = false
                importLauncher.launch(arrayOf("application/json"))
            },
            onDismissRequest = { showImportConfirm = false }
        )
    }
}
