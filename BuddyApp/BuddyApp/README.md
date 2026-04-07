# Buddy — AI Companion App

> Not your typical AI assistant. Buddy is a real, intelligent, emotionally aware AI companion inspired by JARVIS from Iron Man.

---

## 🚀 Quick Setup (5 minutes)

### Requirements
- Android Studio Hedgehog (2023.1.1) or later — [Download free](https://developer.android.com/studio)
- Java 8+
- Android device or emulator (Android 8.0+)
- Anthropic API key — [Get one at console.anthropic.com](https://console.anthropic.com)

### Steps

1. **Open in Android Studio**
   - Launch Android Studio
   - Click "Open" → select the `BuddyApp` folder (this folder)
   - Wait for Gradle sync to complete (~2-3 min first time)

2. **Connect your phone**
   - Enable Developer Options on Android: Settings → About Phone → tap Build Number 7 times
   - Enable USB Debugging: Settings → Developer Options → USB Debugging ON
   - Plug in via USB, trust the computer prompt

3. **Run the app**
   - Click the green ▶️ Run button in Android Studio
   - OR: Build → Build APK → find it in `app/build/outputs/apk/debug/`

4. **First launch**
   - Tap "Set up now" when Buddy introduces himself
   - Enter your Anthropic API key (starts with `sk-ant-`)
   - Optionally fill in your name, age, occupation, city (helps Buddy personalize)
   - Tap "Save Settings"

5. **Start talking**
   - Say **"Hey Buddy"** — Buddy activates automatically
   - Or tap the 🎤 mic button
   - Or just type in the text field

---

## 🧠 Features

| Feature | Status |
|---|---|
| JARVIS-like AI personality | ✅ Via Claude API |
| Wake word "Hey Buddy" | ✅ Always listening |
| Voice input (English/Hindi/Bengali) | ✅ Android STT |
| Deep male voice output | ✅ Android TTS (tuned) |
| Camera vision + description | ✅ Claude Vision API |
| Persistent memory (on-device) | ✅ Encrypted SQLite |
| Proactive suggestions | ✅ Background service |
| Auto-start on boot | ✅ Boot receiver |
| Multilingual auto-detect | ✅ Script detection |

---

## 🔑 API Key

Get your key at **console.anthropic.com**

The key is stored only in your app's private SharedPreferences — never sent anywhere except directly to Anthropic's API for conversation processing.

---

## 📱 Permissions Needed

| Permission | Why |
|---|---|
| Microphone | Voice input + wake word detection |
| Camera | Visual scene understanding |
| Internet | Anthropic API calls |
| Notifications | Proactive suggestions from Buddy |
| Boot completed | Auto-start wake word service |

---

## 🎙️ Wake Word

Say **"Hey Buddy"** anywhere, any time. The service runs in background.

Note: Wake word detection uses Android's speech recognizer which needs brief internet. For fully offline wake word, you can integrate Porcupine (paid) or Vosk (free) later.

---

## 🌐 Languages

Buddy auto-detects your language from the script:
- **English** — Latin characters
- **हिंदी (Hindi)** — Devanagari script
- **বাংলা (Bengali)** — Bengali script

Just speak in your language and Buddy adapts.

---

## 🏗️ Architecture

```
com.buddy.app/
├── ai/
│   └── BuddyAI.kt          — Claude API integration + system prompt
├── camera/
│   └── CameraHelper.kt     — CameraX capture + base64 encoding
├── memory/
│   ├── MemoryDatabase.kt   — Room database setup
│   ├── MemoryEntities.kt   — Data models (interactions, profile, patterns)
│   ├── MemoryDao.kt        — Database queries
│   └── MemoryRepository.kt — Memory access layer
├── service/
│   ├── AutonomousThinkingService.kt — Background proactive suggestions
│   └── BootReceiver.kt     — Auto-start on device boot
├── ui/
│   ├── WaveformView.kt     — Custom animated waveform (idle/listening/thinking/speaking)
│   ├── MessageAdapter.kt   — RecyclerView chat adapter
│   ├── BuddyMessage.kt     — Message data class
│   └── BuddyViewModel.kt   — State management
├── voice/
│   ├── VoiceEngine.kt      — TTS + STT wrapper
│   └── WakeWordService.kt  — Foreground service for always-on listening
├── BuddyApplication.kt     — App singleton
├── MainActivity.kt         — Main UI + orchestration
├── SettingsActivity.kt     — API key + user profile setup
└── SplashActivity.kt       — Boot screen
```

---

## 🔒 Privacy

- All conversation memory is stored **only on your device** in an encrypted database
- No data is collected by this app
- Your messages go to **Anthropic's API** for AI processing (Anthropic's privacy policy applies)
- Nothing else leaves your device

---

## 💡 Tips

- **Long-press the waveform** to clear the chat history
- **Tap the camera icon** to activate vision mode, then "Capture & Ask Buddy"
- Buddy gets smarter about you as you use it more — the memory system builds your profile over time
- Proactive suggestions appear as notifications every ~45 minutes when relevant

---

*Built with Kotlin, CameraX, Room, OkHttp, and the Anthropic Claude API.*
