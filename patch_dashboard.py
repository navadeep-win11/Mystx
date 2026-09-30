with open('app/src/main/java/com/mystx/app/ui/DashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''    updateInfo?.let { info ->
        MystDialog(
            title = "Update Available",
            message = "A new version of Mystx (${info.version}) is available. Would you like to download it now?",
            confirmLabel = "Update",
            dismissLabel = "Later",
            onConfirm = {
                uriHandler.openUri(info.url)
                updateInfo = null
            },
            onDismissRequest = { updateInfo = null }
        )
    }''',
'''    updateInfo?.let { info ->
        MystDialog(
            title = "Update Required",
            message = "A new version of Mystx (${info.version}) is available. Please update the app to continue using it.",
            confirmLabel = "Update",
            dismissLabel = null,
            isCancellable = false,
            onConfirm = {
                uriHandler.openUri("https://mystxnavadeep2.qd.je")
            },
            onDismissRequest = { }
        )
    }'''
)

with open('app/src/main/java/com/mystx/app/ui/DashboardScreen.kt', 'w') as f:
    f.write(content)
