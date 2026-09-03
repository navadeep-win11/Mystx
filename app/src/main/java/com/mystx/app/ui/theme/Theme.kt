package com.mystx.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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

// ---------------------------------------------------------------------------
// Mystx visual identity — glassmorphism
//
// Surfaces are translucent "frosted" panels floating over a soft violet/blue
// gradient wash. Poppins carries the brand voice. Dark glass reads as smoke
// over indigo; light glass reads as frosted white over pastel violet.
// ---------------------------------------------------------------------------

/** Brand accent — the violet end of the Mystx gradient. */
val MystViolet = Color(0xFF8B7CFF)

/** Brand accent — the electric-blue end of the Mystx gradient. */
val MystBlue = Color(0xFF4EA8FF)

/** Poppins family bundled at res/font — the single brand typeface. */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/** Material defaults with Poppins applied to every style slot. */
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

private val DarkColorScheme = darkColorScheme(
    background = Color(0xFF0A0A14),
    surface = Color(0x14131324),          // glass panel: translucent indigo
    surfaceVariant = Color(0x1E1C2F42),   // glass item: more translucent
    surfaceContainerHigh = Color(0x24222E52),
    onBackground = Color(0xFFEAEAFA),
    onSurface = Color(0xFFEAEAFA),
    onSurfaceVariant = Color(0xFFA6A4C4),
    outline = Color(0x33FFFFFF),          // hairline white border of glass
    primary = MystViolet,
    onPrimary = Color(0xFF0B0B18),
    primaryContainer = Color(0x2E8B7CFF),
    onPrimaryContainer = Color(0xFFD6D1FF),
    secondary = MystBlue,
    error = Color(0xFFFF5E7E),
    tertiary = Color(0xFF3DE8A0),
    tertiaryContainer = Color(0x2E3DE8A0)
)

private val LightColorScheme = lightColorScheme(
    background = Color(0xFFF2F0FA),
    surface = Color(0xCCFFFFFF),          // frosted white glass
    surfaceVariant = Color(0x99F2F0FB),
    surfaceContainerHigh = Color(0x80E9E6F8),
    onBackground = Color(0xFF191826),
    onSurface = Color(0xFF191826),
    onSurfaceVariant = Color(0xFF63617E),
    outline = Color(0x59FFFFFF),          // glass edge highlight on light
    primary = Color(0xFF5B48E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E1FF),
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
        typography = mystTypography(),
        content = content
    )
}
