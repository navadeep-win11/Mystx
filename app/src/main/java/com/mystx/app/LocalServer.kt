package com.mystx.app

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class LocalServer(private val context: Context) : NanoHTTPD(0) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        return try {
            if (uri.startsWith("/assets/")) {
                val assetPath = uri.removePrefix("/assets/")
                val mimeType = NanoHTTPD.getMimeTypeForFile(uri)
                val inputStream = context.assets.open(assetPath)
                newChunkedResponse(Response.Status.OK, mimeType, inputStream)
            } else if (uri.startsWith("/cache/")) {
                val cacheFileName = uri.removePrefix("/cache/")
                val file = File(context.cacheDir, cacheFileName)
                if (file.exists()) {
                    val inputStream = FileInputStream(file)
                    newFixedLengthResponse(Response.Status.OK, "application/pdf", inputStream, file.length())
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
