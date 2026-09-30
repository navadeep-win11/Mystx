with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace(
'''plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}''',
'''plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}'''
)

content = content.replace(
'''dependencies {
    implementation("androidx.webkit:webkit:1.11.0")''',
'''dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.webkit:webkit:1.11.0")'''
)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
