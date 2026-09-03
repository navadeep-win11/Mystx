package com.mystx.app.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mystx.app.R
import com.mystx.app.MystxApp
import com.mystx.app.manager.CommandManager
import com.mystx.app.manager.KeyManager
import com.mystx.app.manager.StatsManager
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mystx.app.ui.components.MystGradientButton
import com.mystx.app.ui.components.MystStatTile
import com.mystx.app.ui.components.MystTonalButton
import com.mystx.app.ui.components.MystCardShape
import com.mystx.app.ui.components.ScreenTitle
import com.mystx.app.ui.components.MystCard
import com.mystx.app.ui.components.MystDivider
import com.mystx.app.ui.components.MystPill
import com.mystx.app.ui.components.mystGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

private fun checkServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    return enabledServices.any {
        it.resolveInfo.serviceInfo.packageName == context.packageName
    }
}

/**
 * Best-effort read of the framework's hidden `crashed` flag: a service stuck in the "crashed
 * services" limbo still reports as enabled, so [checkServiceEnabled] cannot see it. Returns
 * false whenever the reflection is unavailable — everything here is guarded (#125). Deliberate
 * hidden-API reflection: on API 36+ the read throws, the catch returns false, and the banner
 * simply falls back to the crash-marker pref.
 */
@SuppressLint("SoonBlockedPrivateApi")
private fun isServiceCrashed(context: Context): Boolean {
    return try {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val field = AccessibilityServiceInfo::class.java.getDeclaredField("crashed")
        am.getInstalledAccessibilityServiceList().any {
            try {
                it.resolveInfo.serviceInfo.packageName == context.packageName && field.getBoolean(it)
            } catch (_: Exception) {
                false
            }
        }
    } catch (_: Exception) {
        false
    }
}

/** Timestamp of the last uncaught crash recorded by [MystxApp], or 0. */
private fun readCrashMarker(context: Context): Long =
    try {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getLong(MystxApp.PREF_SERVICE_DIED_AT, 0L)
    } catch (_: Exception) {
        0L
    }

private fun clearCrashMarker(context: Context) {
    try {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().remove(MystxApp.PREF_SERVICE_DIED_AT).apply()
    } catch (_: Exception) {
    }
}

@Composable
fun DashboardScreen(keyManager: KeyManager, commandManager: CommandManager, statsManager: StatsManager) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isServiceEnabled by remember { mutableStateOf(checkServiceEnabled(context)) }
    // Not seeded from keyManager.getKeys(): that decrypts through AndroidKeyStore on the main
    // thread. The LaunchedEffect below fills it in on the IO dispatcher, as it already did on
    // every subsequent resume.
    var keyCount by remember { mutableIntStateOf(0) }
    // Set when the process died unexpectedly (crash marker) or the framework holds the service
    // in the crashed limbo (hidden flag) — the enabled-state check cannot see either.
    var showKilledBanner by remember { mutableStateOf(false) }

    // Stats state
    var monthlyRequests by remember { mutableIntStateOf(statsManager.monthlyRequests) }
    var favoriteCommand by remember { mutableStateOf(statsManager.favoriteCommand) }
    var dailyCounts by remember { mutableStateOf(statsManager.dailyCounts()) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(context) {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val listener = AccessibilityManager.AccessibilityStateChangeListener {
            isServiceEnabled = checkServiceEnabled(context)
        }
        am.addAccessibilityStateChangeListener(listener)
        onDispose { am.removeAccessibilityStateChangeListener(listener) }
    }

    LaunchedEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val (newEnabled, newKeyCount, killed) = withContext(Dispatchers.IO) {
                Triple(
                    checkServiceEnabled(context),
                    keyManager.getKeys().size,
                    readCrashMarker(context) > 0L || isServiceCrashed(context)
                )
            }
            isServiceEnabled = newEnabled
            keyCount = newKeyCount
            monthlyRequests = statsManager.monthlyRequests
            favoriteCommand = statsManager.favoriteCommand
            dailyCounts = statsManager.dailyCounts()
            showKilledBanner = killed
        }
    }

    val noData = stringResource(R.string.dashboard_no_data)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 16.dp).padding(bottom = 112.dp)
    ) {
        ScreenTitle(stringResource(R.string.dashboard_title))

        // Hero status panel — gradient border ring, floating enable action.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MystCardShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), MystCardShape)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MystPill(
                            text = (if (isServiceEnabled) stringResource(R.string.service_status_active)
                            else stringResource(R.string.service_status_inactive)).uppercase()
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isServiceEnabled) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.error
                                )
                        )
                    }
                    if (!isServiceEnabled) {
                        MystGradientButton(
                            text = stringResource(R.string.service_enable),
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                MystDivider()
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_api_keys_title),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.dashboard_keys_configured, keyCount),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = androidx.compose.ui.text.TextStyle(brush = mystGradient())
                    )
                }
                if (keyCount == 0) {
                    Text(
                        text = stringResource(R.string.dashboard_add_key_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interrupted-service banner: the toggle can still read "on" while the process is dead.
        if (showKilledBanner) {
            MystCard {
                Text(
                    text = stringResource(R.string.dashboard_service_killed_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.dashboard_service_killed_message),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MystGradientButton(
                        text = stringResource(R.string.service_enable),
                        onClick = {
                            clearCrashMarker(context)
                            showKilledBanner = false
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MystTonalButton(
                        text = stringResource(R.string.dashboard_service_killed_dismiss),
                        onClick = {
                            clearCrashMarker(context)
                            showKilledBanner = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Stats grid — two glass tiles.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MystStatTile(
                value = "$monthlyRequests",
                label = stringResource(R.string.dashboard_monthly_requests),
                modifier = Modifier.weight(1f)
            )
            MystStatTile(
                value = favoriteCommand ?: noData,
                label = stringResource(R.string.dashboard_favorite_command),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 7-day activity panel with gradient bars.
        MystCard {
            Text(
                text = stringResource(R.string.dashboard_last_7_days),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            val maxCount = dailyCounts.maxOfOrNull { it.second } ?: 0
            val dayNameFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
            val dateParseFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dailyCounts.forEach { (dateStr, count) ->
                    val dayLabel = try {
                        val date = dateParseFmt.parse(dateStr)
                        dayNameFmt.format(date!!).take(3)
                    } catch (_: Exception) { "?" }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (count > 0) "$count" else "",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(if (maxCount > 0) (count.toFloat() / maxCount).coerceAtLeast(if (count > 0) 0.05f else 0f) else 0f)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(mystGradient())
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayLabel,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
