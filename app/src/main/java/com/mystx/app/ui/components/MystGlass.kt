package com.mystx.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mystx.app.ui.theme.MystBlue
import com.mystx.app.ui.theme.MystViolet

/**
 * Ambient gradient wash behind every screen: a base vertical gradient plus two
 * soft "light blobs" (violet top-left, blue bottom-right). Glass surfaces in
 * the theme are translucent, so this is what shows through and gives the
 * frosted-glass panels their depth. Pure gradient painting — no blur hardware
 * needed, so it renders identically on API 23+.
 */
@Composable
fun MystAuroraBackdrop(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val baseTop = if (dark) Color(0xFF0A0A14) else Color(0xFFF4F2FC)
    val baseBottom = if (dark) Color(0xFF16122E) else Color(0xFFE9E4FA)
    val blobAlpha = if (dark) 0.32f else 0.30f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(baseTop, baseBottom)))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-60).dp)
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MystViolet.copy(alpha = blobAlpha), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 80.dp)
                .size(360.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MystBlue.copy(alpha = blobAlpha), Color.Transparent)
                    )
                )
        )
        // Small accent blob center-right to break symmetry.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 120.dp)
                .size(200.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MystViolet.copy(alpha = blobAlpha * 0.5f), Color.Transparent)
                    )
                )
        )
    }
}
