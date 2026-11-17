# CallistoKey (فارسی & English)

**CallistoKey** is a modern, ultra-fast, privacy-first Android Input Method Editor (IME) designed for seamless Persian (فارسی) and English (US) typing with GitHub Dark aesthetics and bundled Vazirmatn (وزیرمتن) typography.

---

## ✨ Key Features

- **Modern GitHub Dark UI**: Sleek tactile keycaps, crisp vector-drawn icons, floating key preview bubbles, and zero-lag Canvas rendering.
- **Ultra-Fast Typing Engine**: Multi-touch key rollover (never drops rapid two-thumb keystrokes), zero per-frame allocations, and background-threaded suggestion evaluation.
- **Swipe Spacebar Language Switch**: Slide your finger left or right across the Spacebar to switch instantly between **English** and **Persian**.
- **Dedicated Symbols & Punctuation Access**: Direct `!#?` / `؟#؛` Symbols key on the main layout (plus 2nd page with `« » ، ؛ ؟ … ﷼ € ± × ÷`) and long-press alternate characters (`1..0` / `۱..۰` on the top row, `آ أ إ ء` on `ا`, `! ? ؛ …` on `.`).
- **Classic 3×4 Telephone Numeric Keypad**: Large centered digits with instant `123` ⇄ `۱۲۳` script switching.
- **On-Device Spell Check & Auto-Correction**: Weighted Damerau-Levenshtein + physical keyboard proximity scoring for both English and Persian, with instant Backspace undo.
- **Sentence & Next-Word Prediction**: Built-in bilingual phrase starters combined with a local n-gram (bigram/trigram) learner.
- **Learned Frequent-Words Dictionary ("دانشنامه")**: Browse, search, add custom words, or delete individual learned words and phrase patterns from the companion app.
- **Full History & Storage Deletion Controls**: Toggle learning on/off anytime, clear phrase history, or wipe all learned storage with one tap.
- **Strict Offline Privacy**: Zero `INTERNET` permission, zero telemetry/analytics, and automatic pause of suggestions and learning in password/sensitive fields.

---

## 🚀 Build & Test

```bash
# Run unit tests
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📜 Font Attribution

- **Font**: Vazirmatn (وزیرمتن) by Saber Rastikerdar
- **License**: SIL Open Font License 1.1 (`VAZIRMATN_LICENSE.txt`)
