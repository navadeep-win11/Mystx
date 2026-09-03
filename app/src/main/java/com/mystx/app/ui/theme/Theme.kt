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
// Mystx visual identity — "Abyss Glass"
//
// Deep teal-abyss surfaces with translucent glass panels floating over a cyan
// aqua aurora. Poppins carries the brand voice. The palette is intentionally
// distinct from the original app's so the product reads as a different one.
// ---------------------------------------------------------------------------

/** Gradient start — luminous cyan. */
val MystAqua = Color(0xFF22D3EE)

/** Gradient end — deep teal. */
val MystTeal = Color(0xFF2DD4BF)

/** Warm signal accent used sparingly for highlights. */
val MystAmber = Color(0xFFFFB454)

/** The brand gradient, left to right: cyan -> teal. */
fun mystBrandGradient() = listOf(MystAqua, MystTeal)

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
    background = Color(0xFF07131A),
    surface = Color(0x151A2C31),          // glass panel: translucent teal-slate
    surfaceVariant = Color(0x201C3842),   // glass item
    surfaceContainerHigh = Color(0x2A22424E),
    onBackground = Color(0xFFE2F3EF),
    onSurface = Color(0xFFE2F3EF),
    onSurfaceVariant = Color(0xFF93A8A3),
    outline = Color(0x2EFFFFFF),          // hairline white glass edge
    primary = MystTeal,
    onPrimary = Color(0xFF06231C),
    primaryContainer = Color(0x332DD4BF),
    onPrimaryContainer = Color(0xFFB9F5EC),
    secondary = MystAqua,
    tertiary = Color(0xFF3DDC97),
    tertiaryContainer = Color(0x2E3DDC97),
    error = Color(0xFFFF6B81),
    errorContainer = Color(0x33FF6B81)
)

private val LightColorScheme = lightColorScheme(
    background = Color(0xFFF0FAF8),
    surface = Color(0xCCFFFFFF),          // frosted white glass
    surfaceVariant = Color(0x99EDF8F5),
    surfaceContainerHigh = Color(0x80E1F1ED),
    onBackground = Color(0xFF10201D),
    onSurface = Color(0xFF10201D),
    onSurfaceVariant = Color(0xFF5C7370),
    outline = Color(0x59FFFFFF),          // glass edge highlight on light
    primary = Color(0xFF0D9488),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCF3EC),
    onPrimaryContainer = Color(0xFF06332C),
    secondary = Color(0xFF0369A1),
    tertiary = Color(0xFF0E9F6E),
    tertiaryContainer = Color(0xFFD3F5E9),
    error = Color(0xFFDC2F55),
    errorContainer = Color(0xFFFDDDE3)
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
