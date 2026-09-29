with open('app/src/main/java/com/mystx/app/ui/processtext/QuickExplainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    Card(
        modifier = Modifier
            .width(320.dp)
            .heightIn(max = 400.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {''',
'''    Card(
        modifier = Modifier
            .width(320.dp)
            .heightIn(max = 400.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
    ) {'''
)

with open('app/src/main/java/com/mystx/app/ui/processtext/QuickExplainActivity.kt', 'w') as f:
    f.write(content)
