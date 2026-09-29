import re

with open('app/src/main/java/com/mystx/app/ui/PdfReaderScreen.kt', 'r') as f:
    content = f.read()

# Add imports
imports = """
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
"""
content = content.replace("import android.webkit.WebViewClient\n", "import android.webkit.WebViewClient\n" + imports)

# Remove LocalServer imports and state
content = re.sub(r'import com.mystx.app.LocalServer\n', '', content)
content = re.sub(r'var server by remember \{ mutableStateOf<LocalServer\?\(null\) \}\n', '', content)
content = re.sub(r'var serverPort by remember \{ mutableStateOf<Int\?>\(null\) \}\n', '', content)
content = re.sub(r'    DisposableEffect\(Unit\) \{\n        onDispose \{\n            server\?.stop\(\)\n        \}\n    \}\n', '', content)

# Remove server logic
server_logic = """                if (server == null) {
                    val newServer = LocalServer(context)
                    newServer.start()
                    server = newServer
                    serverPort = newServer.listeningPort
                }"""
content = content.replace(server_logic, "")

# Replace condition
content = content.replace("else if (localPdfPath != null && serverPort != null)", "else if (localPdfPath != null)")

# Replace WebView configuration
old_webview = """                        webViewClient = object : WebViewClient() {

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript(\"\"\"
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
                                \"\"\".trimIndent(), null)
                            }
                        }

                        val encodedFileUrl = URLEncoder.encode("http://127.0.0.1:$serverPort/cache/temp_viewer.pdf", "UTF-8")
                        val viewerUrl = "http://127.0.0.1:$serverPort/assets/pdfjs/web/viewer.html?file=$encodedFileUrl"
                        loadUrl(viewerUrl)"""

new_webview = """
                        val assetLoader = WebViewAssetLoader.Builder()
                            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                            .addPathHandler("/cache/", WebViewAssetLoader.InternalStoragePathHandler(ctx, ctx.cacheDir))
                            .build()

                        webViewClient = object : WebViewClientCompat() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                return assetLoader.shouldInterceptRequest(request.url)
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                view.evaluateJavascript(\"\"\"
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
                                \"\"\".trimIndent(), null)
                            }
                        }

                        // Use standard URL encoding for the file parameter
                        val encodedFileUrl = URLEncoder.encode("https://appassets.androidplatform.net/cache/temp_viewer.pdf", "UTF-8")
                        val viewerUrl = "https://appassets.androidplatform.net/assets/pdfjs/web/viewer.html?file=$encodedFileUrl"
                        loadUrl(viewerUrl)"""

content = content.replace(old_webview, new_webview)

with open('app/src/main/java/com/mystx/app/ui/PdfReaderScreen.kt', 'w') as f:
    f.write(content)

