with open('app/src/main/java/com/mystx/app/ui/processtext/QuickExplainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Text(
                        text = result ?: "",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }''',
'''                if (isLoading) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching answer...", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Text(
                            text = if (result.isNullOrBlank()) "Error: API returned an empty response." else result!!,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }'''
)

with open('app/src/main/java/com/mystx/app/ui/processtext/QuickExplainActivity.kt', 'w') as f:
    f.write(content)
