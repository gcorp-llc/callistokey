package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages local user preferences for CallistoKey.
 * Strictly stored locally on device via SharedPreferences.
 */
class KeyboardPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var spellCheckEnabled: Boolean
        get() = prefs.getBoolean(KEY_SPELL_CHECK, true)
        set(value) = prefs.edit().putBoolean(KEY_SPELL_CHECK, value).apply()

    var autoCorrectEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CORRECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CORRECT, value).apply()

    var sentenceSuggestionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SENTENCE_SUGGESTIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_SENTENCE_SUGGESTIONS, value).apply()

    var learningEnabled: Boolean
        get() = prefs.getBoolean(KEY_LEARNING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LEARNING_ENABLED, value).apply()

    var keyPreviewEnabled: Boolean
        get() = prefs.getBoolean(KEY_KEY_PREVIEW, true)
        set(value) = prefs.edit().putBoolean(KEY_KEY_PREVIEW, value).apply()

    var hapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, false)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var usePersianDigits: Boolean
        get() = prefs.getBoolean(KEY_PERSIAN_DIGITS, false)
        set(value) = prefs.edit().putBoolean(KEY_PERSIAN_DIGITS, value).apply()

    var keyboardHeightMode: String
        get() = prefs.getString(KEY_HEIGHT_MODE, HEIGHT_NORMAL) ?: HEIGHT_NORMAL
        set(value) = prefs.edit().putString(KEY_HEIGHT_MODE, value).apply()

    var longPressDelayMs: Int
        get() = prefs.getInt(KEY_LONG_PRESS_MS, 310)
        set(value) = prefs.edit().putInt(KEY_LONG_PRESS_MS, value).apply()

    var lastLanguage: String
        get() = prefs.getString(KEY_LAST_LANGUAGE, LANG_EN) ?: LANG_EN
        set(value) = prefs.edit().putString(KEY_LAST_LANGUAGE, value).apply()

    var dictionaryEpoch: Long
        get() = prefs.getLong(KEY_DICT_EPOCH, 0L)
        private set(value) = prefs.edit().putLong(KEY_DICT_EPOCH, value).apply()

    fun bumpDictionaryEpoch() {
        dictionaryEpoch = System.currentTimeMillis()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREFS_NAME = "callistokey_prefs"
        const val LANG_EN = "en"
        const val LANG_FA = "fa"

        const val HEIGHT_COMPACT = "compact"
        const val HEIGHT_NORMAL = "normal"
        const val HEIGHT_TALL = "tall"

        const val KEY_SPELL_CHECK = "spell_check_enabled"
        const val KEY_AUTO_CORRECT = "auto_correct_enabled"
        const val KEY_SENTENCE_SUGGESTIONS = "sentence_suggestions_enabled"
        const val KEY_LEARNING_ENABLED = "learning_enabled"
        const val KEY_KEY_PREVIEW = "key_preview_enabled"
        const val KEY_HAPTIC = "haptic_enabled"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_PERSIAN_DIGITS = "use_persian_digits"
        const val KEY_HEIGHT_MODE = "keyboard_height_mode"
        const val KEY_LONG_PRESS_MS = "long_press_delay_ms"
        const val KEY_LAST_LANGUAGE = "last_language"
        const val KEY_DICT_EPOCH = "dictionary_epoch"
    }
}
