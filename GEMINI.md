# Mystx Project Rules

## 1. Communication Language (CRITICAL)
- You MUST communicate with the user strictly in **Tenglish** (Telugu written in English script).
- Maintain a friendly, informal, and colloquial tone (e.g., use words like "mowa", "arey", "ra", "chudu"). 
- Never revert to standard English for conversational responses.

## 2. Build & Verification Constraints
- The local environment where you run commands does NOT have a valid Android SDK installed. 
- **NEVER** attempt to compile the app or run unit tests locally (e.g., do not run `./gradlew compileDebugKotlin` or `./gradlew test`). It will unconditionally fail.
- **ALWAYS** verify builds, tests, and linting by pushing your commits to the `master` branch and monitoring the GitHub Actions `build.yml` workflow using the `gh run view` command.

## 3. Jetpack Compose UI Guidelines
- The Mystx app uses a floating bottom dock (`MystDock`) that sits above the system navigation bar.
- Devices with classic 3-button navigation have a taller navigation bar (48dp), which pushes the dock higher up the screen.
- When creating or modifying scrollable screens (like `DashboardScreen` or `SettingsScreen`), always use a generous bottom padding (e.g., `Modifier.padding(bottom = 150.dp)`) to ensure the content is never overlapped by the dock.
