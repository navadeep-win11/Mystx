package com.mystx.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.mystx.app.R

val MystAqua = Color(0xFF0A84FF) // iOS Blue Dark
val MystTeal = Color(0xFF007AFF) // iOS Blue secondary
val MystAmber = Color(0xFFFF9F0A) // iOS Orange Dark

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

private val IosDarkColorScheme = darkColorScheme(
    background = Color(0xFF000000), // Pure black background
    surface = Color(0x33FFFFFF),    // Frosted dark glass
    surfaceVariant = Color(0x26FFFFFF),
    surfaceContainerHigh = Color(0x40FFFFFF),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0x99EBEBF5), // iOS secondary text dark
    outline = Color(0x33FFFFFF),          // very subtle light border
    primary = Color(0xFF0A84FF),          // iOS blue dark
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF004080),
    onPrimaryContainer = Color(0xFFE5F1FF),
    secondary = Color(0xFF30D158),        // iOS green dark
    tertiary = Color(0xFFFF9F0A),         // iOS orange dark
    tertiaryContainer = Color(0xFF4D2F00),
    error = Color(0xFFFF453A),            // iOS red dark
    errorContainer = Color(0xFF4D1511)
)

@Composable
fun MystxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force dark iOS theme
    val colorScheme = IosDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = mystTypography(),
        content = content
    )
}
