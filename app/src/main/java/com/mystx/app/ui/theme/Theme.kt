package com.mystx.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.mystx.app.R

val MystAqua = Color(0xFF007AFF) // iOS Blue
val MystTeal = Color(0xFF0A84FF) // iOS Blue secondary
val MystAmber = Color(0xFFFF9500) // iOS Orange

fun mystBrandGradient() = listOf(MystAqua, MystTeal)

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

private fun mystTypography(): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = Poppins),
        displayMedium = base.displayMedium.copy(fontFamily = Poppins),
        displaySmall = base.displaySmall.copy(fontFamily = Poppins),
        headlineLarge = base.headlineLarge.copy(fontFamily = Poppins),
        headlineMedium = base.headlineMedium.copy(fontFamily = Poppins),
        headlineSmall = base.headlineSmall.copy(fontFamily = Poppins),
        titleLarge = base.titleLarge.copy(fontFamily = Poppins),
        titleMedium = base.titleMedium.copy(fontFamily = Poppins),
        titleSmall = base.titleSmall.copy(fontFamily = Poppins),
        bodyLarge = base.bodyLarge.copy(fontFamily = Poppins),
        bodyMedium = base.bodyMedium.copy(fontFamily = Poppins),
        bodySmall = base.bodySmall.copy(fontFamily = Poppins),
        labelLarge = base.labelLarge.copy(fontFamily = Poppins),
        labelMedium = base.labelMedium.copy(fontFamily = Poppins),
        labelSmall = base.labelSmall.copy(fontFamily = Poppins)
    )
}

private val IosLightColorScheme = lightColorScheme(
    background = Color(0xFFF2F2F7), // iOS grouped background
    surface = Color(0xCCFFFFFF),    // frosted white glass
    surfaceVariant = Color(0xE6FFFFFF), // highly opaque white glass
    surfaceContainerHigh = Color(0x80FFFFFF),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF8E8E93), // iOS secondary text
    outline = Color(0x1A000000),          // very subtle light gray border
    primary = Color(0xFF007AFF),          // iOS blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF004080),
    secondary = Color(0xFF34C759),        // iOS green
    tertiary = Color(0xFFFF9500),         // iOS orange
    tertiaryContainer = Color(0xFFFFECCC),
    error = Color(0xFFFF3B30),            // iOS red
    errorContainer = Color(0xFFFFEBEB)
)

@Composable
fun MystxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force light iOS theme regardless of system dark mode
    val colorScheme = IosLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = mystTypography(),
        content = content
    )
}
