with open('app/src/main/java/com/mystx/app/ui/DraggableExplainPopup.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'import kotlin.math.roundToInt',
    'import kotlin.math.roundToInt\nimport kotlinx.coroutines.TimeoutCancellationException\nimport kotlinx.coroutines.withTimeout'
)

content = content.replace(
'''            val outcome = try {
                runTextCommand(
                    context, keyManager, geminiClient, openAIClient,
                    prompt, selectedText
                )
            } catch (e: Exception) {
                CommandOutcome.Failure("Error: ${e.message}")
            }''',
'''            val outcome = try {
                withTimeout(15_000L) {
                    runTextCommand(
                        context, keyManager, geminiClient, openAIClient,
                        prompt, selectedText
                    )
                }
            } catch (e: TimeoutCancellationException) {
                CommandOutcome.Failure("Network timeout (15s). The AI is not responding.")
            } catch (e: Exception) {
                CommandOutcome.Failure("Error: ${e.message}")
            }'''
)

content = content.replace(
'''                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Text(
                        text = if (result.isNullOrBlank()) "No answer received from AI. It might have been blocked or returned empty. Try selecting different text." else result!!,''',
'''                if (isLoading) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching answer...", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }
                } else {
                    Text(
                        text = if (result.isNullOrBlank()) "Error: API returned an empty response. Try selecting a different text." else result!!,'''
)

with open('app/src/main/java/com/mystx/app/ui/DraggableExplainPopup.kt', 'w') as f:
    f.write(content)
