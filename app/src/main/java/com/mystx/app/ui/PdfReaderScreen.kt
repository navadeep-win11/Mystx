package com.mystx.app.ui
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import app.grapheneos.pdfviewer.PdfViewerScreen
import app.grapheneos.pdfviewer.viewModel.PdfViewModel

@Composable
fun PdfReaderScreen(
    pdfUri: Uri,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: PdfViewModel = viewModel()
    
    
    var selectedText by remember { mutableStateOf<String?>(null) }
    var isPopupVisible by remember { mutableStateOf(false) }
    var isExplainButtonVisible by remember { mutableStateOf(false) }

    LaunchedEffect(pdfUri) {
        viewModel.setUri(pdfUri)
        viewModel.resetDocumentState()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PdfViewerScreen(
            viewModel = viewModel,
            initialMimeError = false,
            onRequestRecreate = {},
            onWebViewCreated = {},
            onWebViewDestroyed = {},
            onSelectionChanged = { text ->
                val trimmed = text.trim()
                if (trimmed.isNotEmpty() && trimmed != "null") {
                    selectedText = trimmed
                    if (!isPopupVisible) {
                        isExplainButtonVisible = true
                    }
                } else {
                    isExplainButtonVisible = false
                    if (!isPopupVisible) {
                        selectedText = null
                    }
                }
            }
        )

        
        AnimatedVisibility(
            visible = isExplainButtonVisible && !isPopupVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    isExplainButtonVisible = false
                    isPopupVisible = true
                },
                icon = { Text("✨") },
                text = { Text("Mystx Explain", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        AnimatedVisibility(
            visible = isPopupVisible,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            if (selectedText != null) {
                DraggableExplainPopup(
                    selectedText = selectedText!!,
                    onClose = { 
                        isPopupVisible = false 
                        selectedText = null
                    }
                )
            }
        }
    }
}
