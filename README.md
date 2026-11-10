# Minimal Keyboard (فارسی & English)

A lightweight, lightning-fast, privacy-first Android Input Method Editor (IME) designed with minimalism, native ergonomics, and GitHub Dark aesthetics.

---

## 🌟 Philosophy & Core Principles

- **Minimalism**: Exactly two languages (Persian and English). Zero unnecessary UI clutters, zero suggestion bars, zero popups.
- **Speed**: Built on native Android Views using high-performance Canvas rendering. Sub-millisecond touch dispatch, zero GC thrashing during typing, fluid 120 FPS frame rate.
- **Privacy First**: 100% offline. **Zero `INTERNET` permission** declared. Zero analytics, zero telemetry, zero keystroke logging, zero cloud sync.
- **Typography**: Powered natively by the **Vazirmatn (وزیرمتن)** font (SIL Open Font License 1.1) bundled directly within the app.
- **Native IME**: A real Android `InputMethodService` that can replace the system keyboard everywhere.

---

## 🎨 GitHub Dark Design Palette

| UI Component | Hex Code | Purpose |
|---|---|---|
| Main Background | `#0D1117` | Root background for companion app |
| Keyboard Surface | `#161B22` | Keyboard container and surface backdrop |
| Normal Keys | `#21262D` | Letter and symbol keys |
| Pressed Keys | `#30363D` | Keypress feedback state |
| Special / Action Keys | `#30363D` | Shift, Backspace, 123, ABC, Enter |
| Borders | `#30363D` | 1dp clean boundary stroke |
| Primary Text | `#E6EDF3` | Crisp white-gray glyphs and labels |
| Secondary Text | `#8B949E` | Functional hints, sub-labels, icons |
| Accent Color | `#58A6FF` | Active Enter action, Caps Lock indicator |

---

## ⌨️ Layout Architecture

### 1. English Layout (QWERTY)
- **Top Row**: `Q W E R T Y U I O P`
- **Middle Row**: `A S D F G H J K L`
- **Bottom Letter Row**: `Shift (⇧/⬆/⇪)` | `Z X C V B N M` | `Backspace (⌫)`
- **Control Row**: `123` | `FA` (instant switch) | `,` | `Space` | `.` | `Enter`

### 2. Persian Layout (Standard PC Layout)
- **First Row**: `ض ص ث ق ف غ ع ه خ ح ج چ` (12 characters)
- **Second Row**: `ش س ی ب ل ا ت ن م ک گ` (11 characters)
- **Third Row**: `نیم‌فاصله (\u200C)` | `ظ ط ز ر ذ د ئ و پ ژ` (10 characters) | `Backspace (⌫)`
- **Control Row**: `۱۲۳` | `EN` (instant switch) | `،` (Persian comma) | `فاصله (Space)` | `.` | `Enter`

### 3. Classic Numeric Keypad (Telephone 3×4 Layout)
Inspired by classic mobile phone dials, featuring large, centered, highly readable keys:
```
+     [ 1 ] [ 2 ] [ 3 ]     ⌫
-     [ 4 ] [ 5 ] [ 6 ]     /
۱۲۳   [ 7 ] [ 8 ] [ 9 ]    SYM
ABC   [ * ] [ 0 ] [ # ]   Enter
```
- Includes a dedicated **Script Toggle (`123` ⇄ `۱۲۳`)** to effortlessly switch between Western and Persian digits without ambiguity.
- Flanked with instant navigation (`ABC`, `SYM`), arithmetic symbols (`+`, `-`, `/`), `Backspace`, and `Enter`.

### 4. Symbols Keyboard
- Comprehensive symbol inventory: `@ # $ % & * - + = / : ; ! ? ( ) [ ] { } " ' < > _ \ | ~ « »`
- Seamless one-tap return to alphabetic or numeric modes.

---

## 🔒 Security & Privacy Guarantees

1. **No Network Access**: The Android manifest contains **no `android.permission.INTERNET`**. The app cannot send or receive data over any network.
2. **Zero Storage of Typed Content**: Keystrokes are directly committed to the active `InputConnection` and immediately discarded from memory.
3. **No Logging**: No `Log.d` / `Log.i` calls capture user input.
4. **No Predictions or Suggestions**: Suggestion bars, spell checkers, and user habit trackers are strictly omitted by design.
5. **No Telemetry / SDKs**: Zero Google Analytics, Firebase Analytics, Crashlytics, or third-party tracking libraries.
6. **Sensitive Input Protection**: Password and PIN fields are treated with zero retention.

---

## 🚀 Build & Installation Instructions

### Prerequisites
- JDK 11 or higher
- Android SDK (API 36 / minSdk 24)
- Gradle

### Building the APK
```bash
# Build Debug APK (signed with debug keystore)
gradle :app:assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Enabling on Device
1. Install `app-debug.apk` via `adb install` or the device package installer.
2. Open **Minimal Keyboard** from the app launcher.
3. Tap **1. Enable Keyboard** and toggle Minimal Keyboard ON in Android System Settings.
4. Tap **2. Switch to Minimal Keyboard** and select Minimal Keyboard as your default input method.
5. Test typing in the built-in **Live Typing Playground** (text, numeric phone dialer, and password fields).

---

## 📜 Font Attribution & License

- **Font Family**: Vazirmatn (وزیرمتن)
- **Author**: Saber Rastikerdar (https://github.com/rastikerdar/vazirmatn)
- **License**: SIL Open Font License 1.1 (included in `VAZIRMATN_LICENSE.txt`)

---

## 📦 Technical Specifications

- **Package Name**: `com.example`
- **Application ID**: `com.aistudio.minimalkeyboard.pvzqxk`
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 36 (Android 16)
- **Architecture**: Native `InputMethodService` + Direct Canvas Custom View
- **Companion App**: Jetpack Compose (Material 3 Dark Theme)
