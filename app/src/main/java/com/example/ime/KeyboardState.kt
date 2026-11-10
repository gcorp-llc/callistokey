package com.example.ime

import android.graphics.RectF

/**
 * Active layout modes supported by the Minimal Keyboard.
 */
enum class KeyboardMode {
    EN_ALPHA,   // English QWERTY
    FA_ALPHA,   // Persian standard PC layout
    NUMERIC,    // Classic 3x4 telephone keypad
    SYMBOLS     // Punctuation and symbols layout
}

/**
 * Shift states for the English keyboard.
 */
enum class ShiftState {
    OFF,        // Lowercase
    ON,         // Single-shot uppercase (reverts after one letter)
    CAPS_LOCK   // Locked uppercase
}

/**
 * Functional classification of keys.
 */
enum class KeyType {
    CHARACTER,
    SPACE,
    BACKSPACE,
    ENTER,
    SHIFT,
    MODE_SWITCH,         // Switch to 123, SYM, or ABC
    LANGUAGE_SWITCH,     // Switch between Persian and English
    ZWNJ,                // Persian zero-width non-joiner (\u200C)
    NUMERIC_SCRIPT_TOGGLE // Toggle between Western 123 and Persian ۱۲۳
}

/**
 * Data model for a single key on the keyboard.
 */
data class KeyModel(
    val type: KeyType,
    var label: String,
    val output: String = label,
    val subLabel: String? = null,
    val weight: Float = 1.0f,
    val isSpecial: Boolean = false,
    val bounds: RectF = RectF(),
    var isPressed: Boolean = false
)
