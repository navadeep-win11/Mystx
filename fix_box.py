with open('app/src/main/java/com/mystx/app/ui/DraggableExplainPopup.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                } else {
                    Text(
                        text = if (result.isNullOrBlank()) "Error: API returned an empty response. Try selecting a different text." else result!!,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
                    )
                }''',
'''                } else {
                    Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                        Text(
                            text = if (result.isNullOrBlank()) "Error: API returned an empty response. Try selecting a different text." else result!!,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }'''
)

with open('app/src/main/java/com/mystx/app/ui/DraggableExplainPopup.kt', 'w') as f:
    f.write(content)
