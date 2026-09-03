<div align="center">

<br>

<img src="playstore-icon.png" width="140" alt="Mystx Icon" />

<br>

# Mystx

### System-wide AI text assistant for Android — powered by Gemini, Groq, **B.ai**, or any OpenAI-compatible endpoint

Type a trigger like **`?fix`** at the end of any text, in any app, and watch it get replaced — instantly.

<br>

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#-getting-started)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#%EF%B8%8F-tech-stack)
[![License: MIT](https://img.shields.io/badge/MIT-blue?style=for-the-badge&logo=opensourceinitiative&logoColor=white)](LICENSE)

[![Latest Release](https://img.shields.io/github/v/release/navadeep-win11/Mystx?style=flat-square&label=Latest&color=brightgreen)](https://github.com/navadeep-win11/Mystx/releases/latest)
[![Build](https://img.shields.io/github/actions/workflow/status/navadeep-win11/Mystx/build.yml?branch=master&style=flat-square&label=CI)](https://github.com/navadeep-win11/Mystx/actions/workflows/build.yml)

<br>

[<img src="https://img.shields.io/badge/⬇_Download_APK-282828?style=for-the-badge" alt="Download APK" height="36">](https://github.com/navadeep-win11/Mystx/releases/latest)

<br>

</div>

## ✨ Features

- **Works system-wide** — WhatsApp, Gmail, Twitter/X, Messages, Notes, and more. No copy-pasting, no app switching.
- **Type a trigger, get a transformation** — `?fix` fixes grammar, `?formal` rewrites formally, and more.
- **Text-selection menu** — select text in any app and run a command from the popup.
- **Custom commands** — define your own triggers and prompts.
- **Four AI providers** — Google Gemini, Groq, **B.ai** (`https://api.b.ai/v1`), and any custom OpenAI-compatible endpoint.
- **Multi-key rotation** — add several API keys; Mystx rotates them automatically on rate limits.
- **Encrypted key storage** — keys live in the Android Keystore, never in plain text.
- **Backup & restore** — export/import your commands as JSON.
- **Private by design** — your text goes only to the AI provider you choose. No analytics, no tracking.
- Glassmorphism interface with Poppins typography — frosted panels over a violet/blue aurora.
- **43 languages** UI translation.

## 🚀 Getting Started

1. **Download** the APK from [Releases](https://github.com/navadeep-win11/Mystx/releases/latest) and install it.
2. **Enable the accessibility service**: Settings → Accessibility → Mystx → On.
3. **Add an API key** in the **Keys** tab:
   - Gemini: [aistudio.google.com/api-keys](https://aistudio.google.com/api-keys)
   - Groq: [console.groq.com/keys](https://console.groq.com/keys)
   - B.ai: [b.ai](https://b.ai)
4. Open any app, type your text, end it with **`?fix`**, and watch it transform.

## ⚙️ Built-in Commands

| Trigger | Action |
|---------|--------|
| `?fix` | Fix grammar and spelling |
| `?formal` | Make it formal |
| `?casual` | Make it casual |
| `?translate <lang>` | Translate to a language |
| `?copy` | Copy selected text (text-selection menu) |

Add your own in the **Commands** tab.

## 🛠️ Building from Source

```bash
git clone https://github.com/navadeep-win11/Mystx.git
cd Mystx
./gradlew assembleDebug        # debug APK
./gradlew assembleRelease      # needs signing secrets (CI does this)
```

Or open the project in **Android Studio**. Every push to `master` runs the CI pipeline,
which builds a signed APK and publishes it to [Releases](https://github.com/navadeep-win11/Mystx/releases).

## 🙌 Credits

Mystx is built on top of [SwiftSlate](https://github.com/Musheer360/SwiftSlate) by
[Musheer360](https://github.com/Musheer360) (MIT License) — rebranded and extended with
the B.ai provider and a new look.

## 📄 License

[MIT](LICENSE)
