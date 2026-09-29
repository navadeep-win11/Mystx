import re

with open("app/src/main/java/com/mystx/app/ui/PdfReaderScreen.kt", "r") as f:
    content = f.read()

# Replace the assetLoader building block
new_loader = """                        val assetLoader = WebViewAssetLoader.Builder()
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
                            .build()"""

content = re.sub(r'val assetLoader = WebViewAssetLoader\.Builder\(\).*?\.build\(\)', new_loader, content, flags=re.DOTALL)

# Replace the JsBridge
new_bridge = """                        class JsBridge {
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
                        addJavascriptInterface(JsBridge(), "channel")"""

content = re.sub(r'class JsBridge \{.*?\n                        \}\n                        addJavascriptInterface\(JsBridge\(\), "AndroidBridge"\)', new_bridge, content, flags=re.DOTALL)

# Update onPageFinished listener
new_onpagefinished = """                            override fun onPageFinished(view: WebView, url: String) {
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
                                    // Make text selectable by overriding GrapheneOS defaults if necessary
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
                                \"\"\".trimIndent(), null)
                            }"""

content = re.sub(r'override fun onPageFinished.*?\}\n                            \}', new_onpagefinished + '\n                            }', content, flags=re.DOTALL)

# Replace loadUrl
content = re.sub(r'val encodedFileUrl.*?loadUrl\(viewerUrl\)', 'loadUrl("https://localhost/viewer/index.html")', content, flags=re.DOTALL)

with open("app/src/main/java/com/mystx/app/ui/PdfReaderScreen.kt", "w") as f:
    f.write(content)
