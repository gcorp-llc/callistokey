package com.example.ui

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DictionaryRepository
import com.example.data.KeyboardPreferences
import com.example.data.LearnedPhraseEntry
import com.example.data.LearnedWordEntry
import com.example.data.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CallistoUiState(
    val isEnabled: Boolean = false,
    val isSelected: Boolean = false,
    val spellCheckEnabled: Boolean = true,
    val autoCorrectEnabled: Boolean = true,
    val sentenceSuggestionsEnabled: Boolean = true,
    val learningEnabled: Boolean = true,
    val keyPreviewEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val usePersianDigits: Boolean = false,
    val keyboardHeightMode: String = KeyboardPreferences.HEIGHT_NORMAL,
    val longPressDelayMs: Int = 310,
    val searchQuery: String = "",
    val languageFilter: String = "all",
    val learnedWords: List<LearnedWordEntry> = emptyList(),
    val learnedPhrases: List<LearnedPhraseEntry> = emptyList(),
    val storageStats: StorageStats = StorageStats(0, 0, 0L),
    val statusBannerMessage: String? = null
)

class CallistoViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = KeyboardPreferences(application)
    private val repository = DictionaryRepository.getInstance(application)

    private val _uiState = MutableStateFlow(CallistoUiState())
    val uiState: StateFlow<CallistoUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        val context = getApplication<Application>()
        val enabled = checkIsKeyboardEnabled(context)
        val selected = checkIsKeyboardSelected(context)

        _uiState.update {
            it.copy(
                isEnabled = enabled,
                isSelected = selected,
                spellCheckEnabled = prefs.spellCheckEnabled,
                autoCorrectEnabled = prefs.autoCorrectEnabled,
                sentenceSuggestionsEnabled = prefs.sentenceSuggestionsEnabled,
                learningEnabled = prefs.learningEnabled,
                keyPreviewEnabled = prefs.keyPreviewEnabled,
                hapticEnabled = prefs.hapticEnabled,
                soundEnabled = prefs.soundEnabled,
                usePersianDigits = prefs.usePersianDigits,
                keyboardHeightMode = prefs.keyboardHeightMode,
                longPressDelayMs = prefs.longPressDelayMs
            )
        }
        loadDictionaryData()
    }

    fun loadDictionaryData() {
        val currentFilter = _uiState.value.languageFilter
        val currentQuery = _uiState.value.searchQuery

        viewModelScope.launch(Dispatchers.IO) {
            val words = repository.getLearnedWords(currentFilter, currentQuery)
            val phrases = repository.getLearnedPhrases(currentFilter)
            val stats = repository.getStorageStats()
            _uiState.update {
                it.copy(
                    learnedWords = words,
                    learnedPhrases = phrases,
                    storageStats = stats
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadDictionaryData()
    }

    fun updateLanguageFilter(filter: String) {
        _uiState.update { it.copy(languageFilter = filter) }
        loadDictionaryData()
    }

    fun setSpellCheck(enabled: Boolean) {
        prefs.spellCheckEnabled = enabled
        _uiState.update { it.copy(spellCheckEnabled = enabled) }
    }

    fun setAutoCorrect(enabled: Boolean) {
        prefs.autoCorrectEnabled = enabled
        _uiState.update { it.copy(autoCorrectEnabled = enabled) }
    }

    fun setSentenceSuggestions(enabled: Boolean) {
        prefs.sentenceSuggestionsEnabled = enabled
        _uiState.update { it.copy(sentenceSuggestionsEnabled = enabled) }
    }

    fun setLearningEnabled(enabled: Boolean) {
        prefs.learningEnabled = enabled
        _uiState.update { it.copy(learningEnabled = enabled) }
    }

    fun setKeyPreview(enabled: Boolean) {
        prefs.keyPreviewEnabled = enabled
        _uiState.update { it.copy(keyPreviewEnabled = enabled) }
    }

    fun setHaptic(enabled: Boolean) {
        prefs.hapticEnabled = enabled
        _uiState.update { it.copy(hapticEnabled = enabled) }
    }

    fun setSound(enabled: Boolean) {
        prefs.soundEnabled = enabled
        _uiState.update { it.copy(soundEnabled = enabled) }
    }

    fun setUsePersianDigits(enabled: Boolean) {
        prefs.usePersianDigits = enabled
        _uiState.update { it.copy(usePersianDigits = enabled) }
    }

    fun setKeyboardHeightMode(mode: String) {
        prefs.keyboardHeightMode = mode
        _uiState.update { it.copy(keyboardHeightMode = mode) }
    }

    fun setLongPressDelayMs(delayMs: Int) {
        prefs.longPressDelayMs = delayMs
        _uiState.update { it.copy(longPressDelayMs = delayMs) }
    }

    fun addCustomWord(word: String, lang: String) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCustomWord(clean, lang)
            loadDictionaryData()
            _uiState.update { it.copy(statusBannerMessage = "کلمه «$clean» به دانشنامه اضافه شد.") }
        }
    }

    fun deleteWord(word: String, lang: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWord(word, lang)
            loadDictionaryData()
        }
    }

    fun deletePhrase(prefix: String, nextWord: String, lang: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePhrase(prefix, nextWord, lang)
            loadDictionaryData()
        }
    }

    fun clearTypingHistoryOnly() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearTypingHistoryOnly()
            loadDictionaryData()
            _uiState.update { it.copy(statusBannerMessage = "تاریخچه جملات و الگوهای تایپ پاک شد.") }
        }
    }

    fun clearAllLearnedData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllLearnedData()
            loadDictionaryData()
            _uiState.update { it.copy(statusBannerMessage = "تمامی کلمات ذخیره شده و تاریخچه تایپ به طور کامل حذف شد.") }
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    private fun checkIsKeyboardEnabled(context: Context): Boolean {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
            imm.enabledInputMethodList.any { it.packageName == context.packageName }
        } catch (_: Exception) {
            false
        }
    }

    private fun checkIsKeyboardSelected(context: Context): Boolean {
        return try {
            val currentIme = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            ) ?: return false
            currentIme.contains(context.packageName)
        } catch (_: Exception) {
            false
        }
    }
}
