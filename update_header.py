with open('app/src/main/java/com/mystx/app/ui/DraggableExplainPopup.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.clickable { onClose() })
            }''',
'''                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!isLoading) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Regenerate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { 
                                skipCache = true
                                triggerFetch++
                            }
                        )
                    }
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.clickable { onClose() })
                }
            }'''
)

with open('app/src/main/java/com/mystx/app/ui/DraggableExplainPopup.kt', 'w') as f:
    f.write(content)
