package com.mystx.app

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

class LocalServer(private val context: Context) : NanoHTTPD(0) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        return try {
            if (uri.startsWith("/assets/")) {
                val assetPath = uri.removePrefix("/assets/")
                                val mimeType = when {
                    uri.endsWith(".js") -> "application/javascript"
                    uri.endsWith(".css") -> "text/css"
                    uri.endsWith(".html") -> "text/html"
                    uri.endsWith(".svg") -> "image/svg+xml"
                    uri.endsWith(".png") -> "image/png"
                    else -> NanoHTTPD.getMimeTypeForFile(uri)
                }
                // Read into memory to provide Content-Length and avoid chunked transfer bugs in WebView
                val bytes = context.assets.open(assetPath).readBytes()
                val inputStream = ByteArrayInputStream(bytes)
                newFixedLengthResponse(Response.Status.OK, mimeType, inputStream, bytes.size.toLong()).apply { addHeader("Access-Control-Allow-Origin", "*") }
            } else if (uri.startsWith("/cache/")) {
                val cacheFileName = uri.removePrefix("/cache/")
                val file = File(context.cacheDir, cacheFileName)
                if (file.exists()) {
                    val inputStream = FileInputStream(file)
                    newFixedLengthResponse(Response.Status.OK, "application/pdf", inputStream, file.length()).apply { addHeader("Access-Control-Allow-Origin", "*") }
                } else {
                    newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
                }
            } else {
                newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.message)
        }
    }
}
