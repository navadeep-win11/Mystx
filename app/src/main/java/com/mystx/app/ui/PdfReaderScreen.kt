package com.mystx.app.ui

import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.pdf.viewer.fragment.PdfViewerFragment
import kotlin.math.max

@Composable
fun PdfReaderScreen(pdfUri: Uri, onClose: () -> Unit) {
    val context = LocalContext.current
    var isPopupVisible by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf<String?>(null) }
    
    // We don't need manual JS bridges! Android native PdfViewerFragment uses native selection!
    // Since Mystx is registered for PROCESS_TEXT globally, the "Mystx Explain" option will natively appear
    // in the Android copy/paste toolbar when text is selected in the PDF!
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val frameLayout = FrameLayout(ctx).apply {
                    id = View.generateViewId()
                }
                
                val activity = ctx as? FragmentActivity
                if (activity != null) {
                    val fragment = PdfViewerFragment()
                    activity.supportFragmentManager.beginTransaction()
                        .replace(frameLayout.id, fragment)
                        .commit()
                        
                    fragment.documentUri = pdfUri
                }
                
                frameLayout
            }
        )

        // Floating Close Button
        SmallFloatingActionButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close PDF")
        }
    }
}
