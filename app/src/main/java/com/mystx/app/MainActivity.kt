package com.mystx.app

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mystx.app.ui.CommandsScreen
import com.mystx.app.ui.components.MystDock
import com.mystx.app.ui.components.MystDockItem
import com.mystx.app.ui.components.MystAuroraBackdrop
import com.mystx.app.ui.DashboardScreen
import com.mystx.app.ui.KeysScreen
import com.mystx.app.ui.SettingsScreen
import com.mystx.app.ui.theme.MystxTheme

enum class Tab(@param:StringRes val titleRes: Int, val icon: ImageVector) {
    Dashboard(R.string.dashboard_title, Icons.Default.Home),
    Keys(R.string.keys_title, Icons.Default.Lock),
    Commands(R.string.commands_title, Icons.AutoMirrored.Filled.List),
    Settings(R.string.settings_title, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MystxTheme {
                MystxMainScreen()
            }
        }
    }
}

@Composable
fun MystxMainScreen(vm: MystxViewModel = viewModel()) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedTab by rememberSaveable { mutableStateOf(Tab.Dashboard) }

    // Request notification permission on first launch (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> // Result not needed — we just need to prompt once
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putBoolean("notification_permission_requested", true).apply()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val alreadyRequested = prefs.getBoolean("notification_permission_requested", false)
                if (!alreadyRequested) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } catch (_: Exception) {
                // A corrupted pref must not crash this activity — it shares the process with
                // the accessibility service (#125).
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MystAuroraBackdrop()
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp)
        ) { innerPadding ->
            val screens = remember {
                Tab.entries.associateWith { tab ->
                    movableContentOf {
                        when (tab) {
                            Tab.Dashboard -> DashboardScreen(vm.keyManager, vm.commandManager, vm.statsManager)
                            Tab.Keys -> KeysScreen(vm.keyManager, vm.prefs)
                            Tab.Commands -> CommandsScreen(vm.commandManager)
                            Tab.Settings -> SettingsScreen(vm.commandManager, vm.prefs, vm.keyManager)
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.padding(innerPadding).statusBarsPadding(),
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal)
                        AnimatedContentTransitionScope.SlideDirection.Left
                    else
                        AnimatedContentTransitionScope.SlideDirection.Right
                    slideIntoContainer(direction, tween(280, easing = FastOutSlowInEasing)) togetherWith
                        slideOutOfContainer(direction, tween(280, easing = FastOutSlowInEasing))
                },
                label = "tab_transition"
            ) { tab ->
                screens[tab]?.invoke()
            }
        }

        // Floating glass dock — hovers over the content, centered, above the nav bar inset.
        MystDock(
            items = Tab.entries.map { MystDockItem(icon = it.icon, labelRes = it.titleRes) },
            selected = selectedTab.ordinal,
            onSelect = { index -> selectedTab = Tab.entries[index] },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
                .widthIn(max = 420.dp)
        )
    }
}
