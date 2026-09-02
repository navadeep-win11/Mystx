package com.mystx.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---------------------------------------------------------------------------
// Mystx visual identity
//
// Dark: deep indigo-black surfaces ("obsidian") with a violet→electric-blue
// gradient accent. Light: cool paper surfaces with an indigo primary so the
// brand accent stays readable on both.
// ---------------------------------------------------------------------------

/** Brand accent — the violet end of the Mystx gradient. */
val MystViolet = Color(0xFF7C6CFF)

/** Brand accent — the electric-blue end of the Mystx gradient. */
val MystBlue = Color(0xFF4EA8FF)

private val DarkColorScheme = darkColorScheme(
    background = Color(0xFF08080F),
    surface = Color(0xFF101019),
    surfaceVariant = Color(0xFF181826),
    surfaceContainerHigh = Color(0xFF20202F),
    onBackground = Color(0xFFE4E4F0),
    onSurface = Color(0xFFE4E4F0),
    onSurfaceVariant = Color(0xFF9A9AB0),
    outline = Color(0xFF2A2A3E),
    primary = MystViolet,
    onPrimary = Color(0xFF0B0B14),
    primaryContainer = Color(0xFF221F45),
    onPrimaryContainer = Color(0xFFC9C2FF),
    secondary = MystBlue,
    error = Color(0xFFFF5470),
    tertiary = Color(0xFF3DE8A0),
    tertiaryContainer = Color(0xFF123328)
)

private val LightColorScheme = lightColorScheme(
    background = Color(0xFFF6F5FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEBF7),
    surfaceContainerHigh = Color(0xFFE2DFF2),
    onBackground = Color(0xFF17161F),
    onSurface = Color(0xFF17161F),
    onSurfaceVariant = Color(0xFF6A6880),
    outline = Color(0xFFD9D5EC),
    primary = Color(0xFF5A48E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF241A70),
    secondary = Color(0xFF2E7FD9),
    error = Color(0xFFE5256B),
    tertiary = Color(0xFF0FA675),
    tertiaryContainer = Color(0xFFD7F5E9)
)

@Composable
fun MystxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
