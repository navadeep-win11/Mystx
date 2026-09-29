with open('app/src/main/java/com/mystx/app/ui/DraggableExplainPopup.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f))',
    '.background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))'
)

with open('app/src/main/java/com/mystx/app/ui/DraggableExplainPopup.kt', 'w') as f:
    f.write(content)
