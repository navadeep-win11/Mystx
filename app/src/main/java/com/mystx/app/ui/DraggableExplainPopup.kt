package com.mystx.app.ui

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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import com.mystx.app.manager.KeyManager
import com.mystx.app.model.PrefKeys
import com.mystx.app.model.HistoryManager
import com.mystx.app.service.CommandOutcome
import com.mystx.app.api.GeminiClient
import com.mystx.app.api.OpenAICompatibleClient
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.math.max
import android.content.Context
import com.mystx.app.service.runTextCommand


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
            } else {

            val outcome = try {
                runTextCommand(
                    context, keyManager, geminiClient, openAIClient,
                    prompt, selectedText
                )
            } catch (e: Exception) {
                CommandOutcome.Failure("Error: ${e.message}")
            }
            
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
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(width.dp, height.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
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
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
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
