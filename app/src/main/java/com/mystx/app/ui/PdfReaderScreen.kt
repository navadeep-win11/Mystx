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
import android.view.View

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

import android.webkit.WebViewClient
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
import com.mystx.app.LocalServer
import java.net.URLEncoder
import com.mystx.app.api.GeminiClient
import com.mystx.app.api.OpenAICompatibleClient
import com.mystx.app.manager.KeyManager
import com.mystx.app.model.HistoryManager
import com.mystx.app.model.PrefKeys
import com.mystx.app.service.CommandOutcome
import com.mystx.app.service.runTextCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PdfReaderScreen(
    pdfUri: Uri,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var localPdfPath by remember { mutableStateOf<String?>(null) }
    var server by remember { mutableStateOf<LocalServer?>(null) }
    var serverPort by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            server?.stop()
        }
    }

    var selectedText by remember { mutableStateOf<String?>(null) }
    var isPopupVisible by remember { mutableStateOf(false) }

    LaunchedEffect(pdfUri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(pdfUri)
                val tempFile = File(context.cacheDir, "temp_viewer.pdf")
                if (server == null) {
                    val newServer = LocalServer(context)
                    newServer.start()
                    server = newServer
                    serverPort = newServer.listeningPort
                }
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                localPdfPath = tempFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BackHandler {
        onClose()
    }

    Box(modifier = Modifier.fillMaxSize()) {
            if (localPdfPath != null && serverPort != null) {
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
                        settings.displayZoomControls = false; setLayerType(View.LAYER_TYPE_SOFTWARE, null)

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
                        }
                        addJavascriptInterface(JsBridge(), "AndroidBridge")

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                        
                        
                        webViewClient = object : WebViewClient() {

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript("""
                                    window.onerror = function(msg, url, line) {
                                        var d = document.createElement('div');
                                        d.style.position = 'absolute';
                                        d.style.top = '50px';
                                        d.style.left = '10px';
                                        d.style.background = 'red';
                                        d.style.color = 'white';
                                        d.style.zIndex = '9999';
                                        d.style.fontSize = '16px';
                                        d.style.padding = '10px';
                                        d.innerHTML = 'JS Error: ' + msg + ' at line ' + line;
                                        document.body.appendChild(d);
                                    };
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
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Aesthetic Floating Button that appears ONLY when text is selected
        AnimatedVisibility(
            visible = selectedText != null && !isPopupVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    isPopupVisible = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("✨ Explain")
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close PDF", tint = Color.White)
        }

        if (isPopupVisible && selectedText != null) {
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

@Composable
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
