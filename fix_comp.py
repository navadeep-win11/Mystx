import re
with open('app/src/main/java/com/mystx/app/ui/components/CommonComponents.kt', 'r') as f:
    content = f.read()

content = re.sub(r'@Composable\s+@Composable', '@Composable', content)

with open('app/src/main/java/com/mystx/app/ui/components/CommonComponents.kt', 'w') as f:
    f.write(content)
