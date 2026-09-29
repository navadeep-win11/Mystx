with open('app/src/main/java/com/mystx/app/ui/DraggableExplainPopup.kt', 'r') as f:
    content = f.read()

# 1. Add triggerFetch and skipCache
content = content.replace(
'''    var result by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedText) {''',
'''    var result by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var triggerFetch by remember { mutableStateOf(0) }
    var skipCache by remember { mutableStateOf(false) }

    LaunchedEffect(selectedText, triggerFetch) {'''
)

# 2. Modify cache check
content = content.replace(
'''            val cached = HistoryManager.findCachedResponse(context, selectedText, "QuickExplain")
            if (!cached.isNullOrBlank()) {''',
'''            val cached = if (skipCache) null else HistoryManager.findCachedResponse(context, selectedText, "QuickExplain")
            if (!cached.isNullOrBlank()) {'''
)

# 3. Remove withTimeout and TimeoutCancellationException
content = content.replace(
'''            val outcome = try {
                withTimeout(15_000L) {
                    runTextCommand(
                        context, keyManager, geminiClient, openAIClient,
                        prompt, selectedText
                    )
                }
            } catch (e: TimeoutCancellationException) {
                CommandOutcome.Failure("Network timeout (15s). The AI is not responding.")
            } catch (e: Exception) {''',
'''            val outcome = try {
                runTextCommand(
                    context, keyManager, geminiClient, openAIClient,
                    prompt, selectedText
                )
            } catch (e: Exception) {'''
)

# 4. Add Regenerate button to the Header
content = content.replace(
'''                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.clickable { onClose() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))''',
'''                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!isLoading) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { 
                                    skipCache = true
                                    triggerFetch++
                                }
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.clickable { onClose() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))'''
)

# 5. Make sure Icons.Default.Refresh is imported
if 'import androidx.compose.material.icons.filled.Refresh' not in content:
    content = content.replace(
        'import androidx.compose.material.icons.filled.Close',
        'import androidx.compose.material.icons.filled.Close\nimport androidx.compose.material.icons.filled.Refresh'
    )

with open('app/src/main/java/com/mystx/app/ui/DraggableExplainPopup.kt', 'w') as f:
    f.write(content)
