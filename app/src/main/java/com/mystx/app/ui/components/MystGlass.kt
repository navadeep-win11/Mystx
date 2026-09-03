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
import com.mystx.app.ui.theme.MystAqua
import com.mystx.app.ui.theme.MystTeal

/**
 * Ambient aurora behind every screen: deep abyss base with three soft light
 * fields (cyan top-left, teal bottom-right, faint teal center-right). The
 * translucent glass surfaces in the theme pick this up and read as frosted
 * glass. Pure gradient painting — no blur hardware needed, smooth on API 23+.
 */
@Composable
fun MystAuroraBackdrop(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val baseTop = if (dark) Color(0xFF07131A) else Color(0xFFF0FAF8)
    val baseBottom = if (dark) Color(0xFF102831) else Color(0xFFDFF3EE)
    val blobAlpha = if (dark) 0.30f else 0.26f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(baseTop, baseBottom)))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-90).dp, y = (-70).dp)
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MystAqua.copy(alpha = blobAlpha), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = 90.dp)
                .size(380.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MystTeal.copy(alpha = blobAlpha), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 130.dp)
                .size(220.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MystAqua.copy(alpha = blobAlpha * 0.45f), Color.Transparent)
                    )
                )
        )
    }
}
