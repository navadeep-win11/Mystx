with open('app/src/main/java/com/mystx/app/ui/processtext/QuickExplainActivity.kt', 'r') as f:
    content = f.read()

# Add state vars
content = content.replace(
'''    var result by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(selectedText) {''',
'''    var result by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var triggerFetch by remember { mutableStateOf(0) }
    var skipCache by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(selectedText, triggerFetch) {'''
)

# Modify cache
content = content.replace(
'''            val cached = HistoryManager.findCachedResponse(context, selectedText, "QuickExplain")
            if (!cached.isNullOrBlank()) {''',
'''            val cached = if (skipCache) null else HistoryManager.findCachedResponse(context, selectedText, "QuickExplain")
            if (!cached.isNullOrBlank()) {'''
)

# Add Regenerate button
content = content.replace(
'''                Text(
                    text = "Mystx Explain",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.clickable { onClose() }
                )
            }''',
'''                Text(
                    text = "Mystx Explain",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!isLoading) {
                        Icon(
                            androidx.compose.material.icons.filled.Refresh, 
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
            }'''
)

with open('app/src/main/java/com/mystx/app/ui/processtext/QuickExplainActivity.kt', 'w') as f:
    f.write(content)
