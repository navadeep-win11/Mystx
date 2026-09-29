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
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse

import android.webkit.WebViewClient

import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
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

        var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedText by remember { mutableStateOf<String?>(null) }
    var isPopupVisible by remember { mutableStateOf(false) }

    LaunchedEffect(pdfUri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(pdfUri)
                val tempFile = File(context.cacheDir, "temp_viewer.pdf")

                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                localPdfPath = tempFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.stackTraceToString()
            }
        }
    }

    BackHandler {
        onClose()
    }

    Box(modifier = Modifier.fillMaxSize()) {
                    if (errorMessage != null) {
            Column(modifier = Modifier.align(Alignment.Center).padding(16.dp)) {
                Text("Error opening PDF:\n$errorMessage", color = Color.Red)
            }
        } else if (localPdfPath != null) {
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
                                    isPopupVisible = true
                                } else {
                                    if (!isPopupVisible) {
                                        selectedText = null
                                    }
                                }
                            }
                            
                            // GrapheneOS mocks
                            @androidx.annotation.Keep @JavascriptInterface fun getPage(): Int = 1
                            @androidx.annotation.Keep @JavascriptInterface fun getZoomRatio(): Float = 1.0f
                            @androidx.annotation.Keep @JavascriptInterface fun getDocumentOrientationDegrees(): Int = 0
                            @androidx.annotation.Keep @JavascriptInterface fun getInsetLeft(): Int = 0
                            @androidx.annotation.Keep @JavascriptInterface fun getInsetTop(): Int = 0
                            @androidx.annotation.Keep @JavascriptInterface fun getInsetRight(): Int = 0
                            @androidx.annotation.Keep @JavascriptInterface fun getInsetBottom(): Int = 0
                            @androidx.annotation.Keep @JavascriptInterface fun getMaxRenderPixels(): Int = 10000000
                            @androidx.annotation.Keep @JavascriptInterface fun getPassword(): String = ""
                            @androidx.annotation.Keep @JavascriptInterface fun showPasswordPrompt() {}
                            @androidx.annotation.Keep @JavascriptInterface fun invalidPassword() {}
                            @androidx.annotation.Keep @JavascriptInterface fun onLoaded() {}
                            @androidx.annotation.Keep @JavascriptInterface fun setNumPages(pages: Int) {}
                            @androidx.annotation.Keep @JavascriptInterface fun setDocumentProperties(props: String) {}
                            @androidx.annotation.Keep @JavascriptInterface fun setHasDocumentOutline(has: Boolean) {}
                            @androidx.annotation.Keep @JavascriptInterface fun onLoadError() {}
                            @androidx.annotation.Keep @JavascriptInterface fun setZoomRatio(zoom: Float) {}
                            @androidx.annotation.Keep @JavascriptInterface fun getZoomFocusX(): Float = 0f
                            @androidx.annotation.Keep @JavascriptInterface fun getZoomFocusY(): Float = 0f
                            @androidx.annotation.Keep @JavascriptInterface fun getMaxZoomRatio(): Float = 5.0f
                            @androidx.annotation.Keep @JavascriptInterface fun getMinZoomRatio(): Float = 0.5f
                            @androidx.annotation.Keep @JavascriptInterface fun setDocumentOutline(outline: String?) {}
                        }
                        addJavascriptInterface(JsBridge(), "channel")

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                        
                        

                                                val assetLoader = WebViewAssetLoader.Builder()
                            .setDomain("localhost")
                            .addPathHandler("/viewer/", object : WebViewAssetLoader.PathHandler {
                                val delegate = WebViewAssetLoader.AssetsPathHandler(ctx)
                                override fun handle(path: String): WebResourceResponse? {
                                    return delegate.handle("viewer/" + path)
                                }
                            })
                            .addPathHandler("/placeholder.pdf", object : WebViewAssetLoader.PathHandler {
                                override fun handle(path: String): WebResourceResponse? {
                                    return try {
                                        val file = java.io.File(ctx.cacheDir, "temp_viewer.pdf")
                                        val response = WebResourceResponse("application/pdf", null, java.io.FileInputStream(file))
                                        val headers = mutableMapOf<String, String>()
                                        headers["Access-Control-Allow-Origin"] = "*"
                                        response.responseHeaders = headers
                                        response
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            })
                            .build()

                        webViewClient = object : WebViewClientCompat() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                val response = assetLoader.shouldInterceptRequest(request.url)
                                if (response != null) {
                                    var headers = response.responseHeaders
                                    if (headers == null) {
                                        headers = mutableMapOf()
                                    }
                                    headers["Access-Control-Allow-Origin"] = "*"
                                    response.responseHeaders = headers
                                }
                                return response
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                view.evaluateJavascript("""
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
                                    // Make text selectable
                                    var style = document.createElement('style');
                                    style.innerHTML = '.textLayer { touch-action: auto !important; }';
                                    document.head.appendChild(style);
                                    
                                    document.addEventListener("selectionchange", function() {
                                        var text = window.getSelection().toString();
                                        window.channel.onSelectionChanged(text);
                                    });
                                    
                                    // Start GrapheneOS document load
                                    if (typeof window.loadDocument === 'function') {
                                        window.loadDocument();
                                    } else {
                                        setTimeout(function() {
                                            if (typeof window.loadDocument === 'function') {
                                                window.loadDocument();
                                            }
                                        }, 500);
                                    }
                                """.trimIndent(), null)
                            }
                        }

                        // Use standard URL encoding for the file parameter
                        loadUrl("https://localhost/viewer/index.html")
                    }
                }
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
