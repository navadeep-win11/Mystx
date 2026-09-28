package com.mystx.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebViewClient
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mystx.app.LocalServer
import com.mystx.app.MystxViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.pdf.viewer.fragment.PdfViewerFragment


@Composable
fun PdfReaderScreen(pdfUri: Uri, onClose: () -> Unit) {
    val context = LocalContext.current
    var isPopupVisible by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var localPdfPath by remember { mutableStateOf<String?>(null) }
    var server by remember { mutableStateOf<LocalServer?>(null) }
    var serverPort by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            server?.stop()
        }
    }

    LaunchedEffect(pdfUri) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(pdfUri)
                    val tempFile = File(context.cacheDir, "temp_viewer.pdf")
                    val outputStream = FileOutputStream(tempFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    
                    if (Build.VERSION.SDK_INT < 31) {
                        if (server == null) {
                            val newServer = LocalServer(context)
                            newServer.start()
                            server = newServer
                            serverPort = newServer.listeningPort
                        }
                    }
                    localPdfPath = tempFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (localPdfPath != null) {
            if (Build.VERSION.SDK_INT >= 31) {
                // GOOGLE NATIVE PDF VIEWER FOR ANDROID 12+
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
            } else if (serverPort != null) {
                // PDF.JS FALLBACK FOR ANDROID 11 AND BELOW
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowFileAccessFromFileURLs = true
                            settings.allowUniversalAccessFromFileURLs = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false

                            class JsBridge {
                                @androidx.annotation.Keep
                                @JavascriptInterface
                                fun onSelectionChanged(text: String) {
                                    val trimmed = text.trim()
                                    if (trimmed.isNotEmpty() && trimmed != "null") {
                                        selectedText = trimmed
                                    } else {
                                        if (!isPopupVisible) {
                                            selectedText = null
                                        }
                                    }
                                }
                                
                                @androidx.annotation.Keep
                                @JavascriptInterface
                                fun requestExplain() {
                                    if (!selectedText.isNullOrEmpty()) {
                                        isPopupVisible = true
                                    }
                                }
                            }
                            
                            addJavascriptInterface(JsBridge(), "AndroidBridge")

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript("""
                                        document.addEventListener("selectionchange", function() {
                                            var text = window.getSelection().toString();
                                            window.AndroidBridge.onSelectionChanged(text);
                                        });
                                    """.trimIndent(), null)
                                }
                            }

                            val encodedFileUrl = URLEncoder.encode("http://127.0.0.1:$serverPort/cache/temp_viewer.pdf", "UTF-8")
                            val viewerUrl = "http://127.0.0.1:$serverPort/assets/pdfjs/web/viewer.html?file=$encodedFileUrl"
                            loadUrl(viewerUrl)
                        }
                    }
                )
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Close button
        SmallFloatingActionButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close PDF")
        }

        // Animated Explain Button for Android 11 and below (Native PDF viewer uses global intent)
        if (Build.VERSION.SDK_INT < 31) {
            AnimatedVisibility(
                visible = !selectedText.isNullOrEmpty() && !isPopupVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                FloatingActionButton(
                    onClick = { isPopupVisible = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        "✨ Explain",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // The draggable explanation popup window
        AnimatedVisibility(
            visible = isPopupVisible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            DraggableExplainPopup(
                selectedText = selectedText,
                onClose = { isPopupVisible = false }
            )
        }
    }
}
fun DraggableExplainPopup(
    selectedText: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var offsetX by remember { mutableStateOf(50f) }
    var offsetY by remember { mutableStateOf(200f) }
    var width by remember { mutableStateOf(320f) }
    var height by remember { mutableStateOf(400f) }

    var result by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedText) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val keyManager = KeyManager(context)
            val geminiClient = GeminiClient()
            val openAIClient = OpenAICompatibleClient()

            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val lang = prefs.getString(PrefKeys.EXPLAIN_LANGUAGE, "Tenglish") ?: "Tenglish"
            
            val prompt = """
                Analyze the following text:
                "${selectedText}"
                
                If it is a single word or short phrase, provide its meaning and a simple example sentence.
                If it is a question, provide a concise answer.
                Please respond natively in ${lang}.
            """.trimIndent()

            val cached = HistoryManager.findCachedResponse(context, selectedText, "QuickExplain")
            if (cached != null) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    result = cached
                }
                return@withContext
            }

            val outcome = runTextCommand(
                context, keyManager, geminiClient, openAIClient,
                prompt, selectedText
            )
            
            withContext(Dispatchers.Main) {
                isLoading = false
                when (outcome) {
                    is CommandOutcome.Success -> {
                        result = outcome.text
                        HistoryManager.addEntry(context, selectedText, "QuickExplain", outcome.text, true)
                    }
                    is CommandOutcome.Failure -> result = outcome.message
                    is CommandOutcome.Refusal -> result = "Safety blocked"
                    is CommandOutcome.Unavailable -> result = outcome.message
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(width.dp, height.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✨ Mystx Explain", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.clickable { onClose() })
            }
            
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Text(
                    text = "\"$selectedText\"",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Text(
                        text = result ?: "",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .background(Color.Transparent)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        width = max(250f, width + dragAmount.x)
                        height = max(250f, height + dragAmount.y)
                    }
                }
        ) {
            Text("◢", modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp), color = Color.Gray, fontSize = 12.sp)
        }
    }
}
// Need to add DisposableEffect but wait, I can just patch it cleanly using sed or python.
