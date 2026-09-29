with open('app/src/main/java/com/mystx/app/ui/PdfReaderScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''            onSelectionChanged = { text ->
                val trimmed = text.trim()
                if (trimmed.isNotEmpty() && trimmed != "null") {
                    selectedText = trimmed
                    if (!isPopupVisible) {
                        isExplainButtonVisible = true
                    }
                } else {
                    isExplainButtonVisible = false
                    if (!isPopupVisible) {
                        selectedText = null
                    }
                }
            }''',
'''            onSelectionChanged = { text ->
                val trimmed = text.trim()
                if (trimmed.isNotEmpty() && trimmed != "null") {
                    if (!isPopupVisible) {
                        selectedText = trimmed
                        isExplainButtonVisible = true
                    }
                } else {
                    isExplainButtonVisible = false
                    if (!isPopupVisible) {
                        selectedText = null
                    }
                }
            }'''
)

with open('app/src/main/java/com/mystx/app/ui/PdfReaderScreen.kt', 'w') as f:
    f.write(content)
