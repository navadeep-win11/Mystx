package com.mystx.app.ui

import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.pdf.viewer.fragment.PdfViewerFragment

@Composable
fun PdfReaderScreen(pdfUri: Uri, onClose: () -> Unit) {
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
