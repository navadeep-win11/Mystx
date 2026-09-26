package com.mystx.app.ui.processtext

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mystx.app.R
import com.mystx.app.service.CommandOutcome
import com.mystx.app.api.GeminiClient
import com.mystx.app.model.HistoryManager
import com.mystx.app.model.PrefKeys
import android.content.Context
import com.mystx.app.manager.KeyManager
import com.mystx.app.api.OpenAICompatibleClient
import com.mystx.app.service.runTextCommand
import com.mystx.app.ui.theme.MystxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickExplainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString() ?: ""
        if (selectedText.isBlank()) {
            finish()
            return
        }

        // Setup window to be floating and movable
        window.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window.setGravity(android.view.Gravity.CENTER)

        setContent {
            MystxTheme(forceLight = true) {
                QuickExplainScreen(
                    selectedText = selectedText,
                    onClose = { finish() },
                    onDrag = { dx, dy ->
                        val params = window.attributes
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        window.attributes = params
                    }
                )
            }
        }
    }
}

@Composable
fun QuickExplainScreen(selectedText: String, onClose: () -> Unit, onDrag: (Float, Float) -> Unit) {
    var result by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(selectedText) {
        // Run AI request
        withContext(Dispatchers.IO) {
            val keyManager = KeyManager(context)
            val geminiClient = GeminiClient()
            val openAIClient = OpenAICompatibleClient()

            // Construct specific prompt
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
                        // Save to history (isSelection = true)
                        HistoryManager.addEntry(context, selectedText, "QuickExplain", outcome.text, true)
                    }
                    is CommandOutcome.Failure -> result = outcome.message
                    is CommandOutcome.Refusal -> result = "Safety blocked"
                    is CommandOutcome.Unavailable -> result = outcome.message
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .width(320.dp)
            .heightIn(max = 400.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mystx Explain",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.clickable { onClose() }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "\"$selectedText\"",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
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
}
