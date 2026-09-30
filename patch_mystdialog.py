with open('app/src/main/java/com/mystx/app/ui/components/CommonComponents.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''fun MystDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    dismissLabel: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,''',
'''import androidx.compose.ui.window.DialogProperties

@Composable
fun MystDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    dismissLabel: String? = null,
    isCancellable: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = isCancellable, dismissOnClickOutside = isCancellable),'''
)

with open('app/src/main/java/com/mystx/app/ui/components/CommonComponents.kt', 'w') as f:
    f.write(content)
