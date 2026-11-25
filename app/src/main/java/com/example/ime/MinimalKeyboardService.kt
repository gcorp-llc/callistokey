package com.example.ime

import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import com.example.MainActivity
import com.example.data.DictionaryRepository
import com.example.data.KeyboardPreferences
import com.example.ime.nlp.SuggestionChip
import com.example.ime.nlp.SuggestionEngine
import com.example.ime.views.CustomKeyboardView
import com.example.ime.views.KeyboardContainerView
import com.example.ime.views.SuggestionBarView
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Production Android Input Method Service for CallistoKey.
 * Features ultra-fast native Canvas rendering, Spacebar swipe language switching,
 * local spell checking, sentence/phrase prediction, and user-controlled frequent-word learning.
 */
class MinimalKeyboardService : InputMethodService(),
    CustomKeyboardView.OnKeyboardActionListener,
    SuggestionBarView.OnSuggestionListener {

    private var containerView: KeyboardContainerView? = null
    private lateinit var prefs: KeyboardPreferences
    private lateinit var repository: DictionaryRepository
    private lateinit var suggestionEngine: SuggestionEngine
    private val inputHandler = InputConnectionHandler()
    private var audioManager: AudioManager? = null

    private val nlpExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val suggestionSeq = AtomicLong(0L)

    @Volatile
    private var currentAutoCorrectCandidate: String? = null
    private var lastAutoCorrectedOriginal: String? = null
    private var lastAutoCorrectedReplacedWithSpace: String? = null
    private var isSensitiveField: Boolean = false

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        applyPreferencesToViews()
        refreshSuggestions()
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
        prefs = KeyboardPreferences(this)
        repository = DictionaryRepository.getInstance(this)
        suggestionEngine = SuggestionEngine(repository, prefs)
        prefs.registerListener(prefListener)
    }

    override fun onDestroy() {
        prefs.unregisterListener(prefListener)
        nlpExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val container = KeyboardContainerView(this)
        container.keyboardView.listener = this
        container.suggestionBar.listener = this
        containerView = container

        // Restore last preferred language
        val initialMode = if (prefs.lastLanguage == KeyboardPreferences.LANG_FA) {
            KeyboardMode.FA_ALPHA
        } else {
            KeyboardMode.EN_ALPHA
        }
        container.keyboardView.setMode(initialMode)
        applyPreferencesToViews()
        return container
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        clearAutoCorrectUndoState()
        isSensitiveField = inputHandler.isSensitiveEditor(info)
        applyPreferencesToViews()

        containerView?.keyboardView?.let { kb ->
            val enterLabel = inputHandler.getEnterLabel(info)
            kb.setEnterLabel(enterLabel)

            val inputClass = info?.inputType?.and(InputType.TYPE_MASK_CLASS)
            if (inputClass == InputType.TYPE_CLASS_NUMBER ||
                inputClass == InputType.TYPE_CLASS_PHONE ||
                inputClass == InputType.TYPE_CLASS_DATETIME
            ) {
                kb.setMode(KeyboardMode.NUMERIC)
            } else if (kb.currentMode == KeyboardMode.NUMERIC) {
                kb.setMode(kb.lastAlphaMode)
            }
        }
        refreshSuggestions()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        refreshSuggestions()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        clearAutoCorrectUndoState()
        containerView?.keyboardView?.setShift(ShiftState.OFF)
    }

    private fun applyPreferencesToViews() {
        val container = containerView ?: return
        container.keyboardView.hapticEnabled = prefs.hapticEnabled
        container.keyboardView.keyPreviewEnabled = prefs.keyPreviewEnabled
        container.keyboardView.longPressDelayMs = prefs.longPressDelayMs
        container.keyboardView.heightMode = prefs.keyboardHeightMode
        container.keyboardView.setUsePersianDigits(prefs.usePersianDigits)
        container.suggestionBar.hapticEnabled = prefs.hapticEnabled
    }

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        val kb = containerView?.keyboardView
        val isPersian = (kb?.lastAlphaMode == KeyboardMode.FA_ALPHA)

        if (text == " ") {
            val textBefore = inputHandler.getTextBeforeCursor(ic, 80)
            val composing = SuggestionEngine.extractCurrentWord(textBefore)
            val priorText = if (composing.isNotEmpty()) textBefore.dropLast(composing.length) else textBefore

            val autoTarget = currentAutoCorrectCandidate
            if (!isSensitiveField &&
                prefs.spellCheckEnabled &&
                prefs.autoCorrectEnabled &&
                composing.length >= 2 &&
                !autoTarget.isNullOrEmpty() &&
                autoTarget != composing
            ) {
                val replaced = "$autoTarget "
                inputHandler.replaceCurrentWord(ic, composing.length, replaced)
                lastAutoCorrectedOriginal = composing
                lastAutoCorrectedReplacedWithSpace = replaced
                suggestionEngine.learnCommittedWord(priorText, autoTarget, isPersian, isSensitiveField)
            } else {
                clearAutoCorrectUndoState()
                inputHandler.commitText(ic, " ")
                if (composing.isNotEmpty()) {
                    suggestionEngine.learnCommittedWord(priorText, composing, isPersian, isSensitiveField)
                }
            }
        } else {
            clearAutoCorrectUndoState()
            // Learn word when user types sentence-ending punctuation
            if (text.length == 1 && !SuggestionEngine.isWordChar(text[0])) {
                val textBefore = inputHandler.getTextBeforeCursor(ic, 80)
                val composing = SuggestionEngine.extractCurrentWord(textBefore)
                if (composing.isNotEmpty()) {
                    val priorText = textBefore.dropLast(composing.length)
                    suggestionEngine.learnCommittedWord(priorText, composing, isPersian, isSensitiveField)
                }
            }
            inputHandler.commitText(ic, text)
        }

        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
        refreshSuggestions()
    }

    override fun onBackspace() {
        val ic = currentInputConnection ?: return

        // Support instant undo if Backspace is pressed immediately after an auto-correction
        val orig = lastAutoCorrectedOriginal
        val replaced = lastAutoCorrectedReplacedWithSpace
        if (orig != null && replaced != null) {
            val before = inputHandler.getTextBeforeCursor(ic, replaced.length + 2)
            if (before.endsWith(replaced)) {
                inputHandler.undoReplacement(ic, replaced.length, orig)
                clearAutoCorrectUndoState()
                playClickSound(AudioManager.FX_KEYPRESS_DELETE)
                refreshSuggestions()
                return
            }
            clearAutoCorrectUndoState()
        }

        inputHandler.handleDelete(ic)
        playClickSound(AudioManager.FX_KEYPRESS_DELETE)
        refreshSuggestions()
    }

    override fun onEnter() {
        clearAutoCorrectUndoState()
        val ic = currentInputConnection
        val kb = containerView?.keyboardView
        val isPersian = (kb?.lastAlphaMode == KeyboardMode.FA_ALPHA)

        if (ic != null) {
            val textBefore = inputHandler.getTextBeforeCursor(ic, 80)
            val composing = SuggestionEngine.extractCurrentWord(textBefore)
            if (composing.isNotEmpty()) {
                val priorText = textBefore.dropLast(composing.length)
                suggestionEngine.learnCommittedWord(priorText, composing, isPersian, isSensitiveField)
            }
        }

        inputHandler.handleEnter(ic, currentInputEditorInfo)
        playClickSound(AudioManager.FX_KEYPRESS_RETURN)
        refreshSuggestions()
    }

    override fun onShiftToggle() {
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
    }

    override fun onShiftDoubleTap() {
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
    }

    override fun onLanguageSwitch() {
        clearAutoCorrectUndoState()
        val kb = containerView?.keyboardView
        if (kb != null) {
            prefs.lastLanguage = if (kb.lastAlphaMode == KeyboardMode.FA_ALPHA) {
                KeyboardPreferences.LANG_FA
            } else {
                KeyboardPreferences.LANG_EN
            }
        }
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
        refreshSuggestions()
    }

    override fun onModeSwitch(target: String) {
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
        refreshSuggestions()
    }

    override fun onNumericScriptToggle() {
        val kb = containerView?.keyboardView
        if (kb != null) {
            prefs.usePersianDigits = kb.usePersianDigits
        }
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
    }

    override fun onZwnj() {
        clearAutoCorrectUndoState()
        inputHandler.commitText(currentInputConnection, "\u200C")
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
        refreshSuggestions()
    }

    override fun onSuggestionClicked(chip: SuggestionChip) {
        val ic = currentInputConnection ?: return
        val kb = containerView?.keyboardView
        val isPersian = (kb?.lastAlphaMode == KeyboardMode.FA_ALPHA)
        clearAutoCorrectUndoState()

        val rawText = chip.text
        val cleanText = if (rawText.startsWith("\"") && rawText.endsWith("\"") && rawText.length > 2) {
            rawText.substring(1, rawText.length - 1)
        } else {
            rawText
        }

        val textBefore = inputHandler.getTextBeforeCursor(ic, 80)
        val composing = SuggestionEngine.extractCurrentWord(textBefore)
        val priorText = if (composing.isNotEmpty()) textBefore.dropLast(composing.length) else textBefore

        if (composing.isNotEmpty()) {
            inputHandler.replaceCurrentWord(ic, composing.length, "$cleanText ")
        } else {
            inputHandler.commitText(ic, "$cleanText ")
        }

        // Learn words/phrases selected by user
        val wordsInChip = cleanText.split(Regex("\\s+")).filter { it.isNotBlank() }
        var runningContext = priorText
        for (w in wordsInChip) {
            val trimmedWord = w.trimEnd('.', '?', '!', '،', '؛', '؟')
            if (trimmedWord.length >= 2) {
                suggestionEngine.learnCommittedWord(runningContext, trimmedWord, isPersian, isSensitiveField)
                runningContext = "$runningContext $trimmedWord"
            }
        }

        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
        refreshSuggestions()
    }

    override fun onLanguageBadgeClicked() {
        containerView?.keyboardView?.switchLanguage()
        onLanguageSwitch()
    }

    override fun onSettingsIconClicked() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("open_tab", 2)
        }
        startActivity(intent)
    }

    private fun refreshSuggestions() {
        val container = containerView ?: return
        val ic = currentInputConnection
        val kb = container.keyboardView
        val isPersian = (kb.lastAlphaMode == KeyboardMode.FA_ALPHA)
        val langTag = if (isPersian) "FA" else "EN"
        val sensitive = isSensitiveField

        if (sensitive || ic == null) {
            currentAutoCorrectCandidate = null
            container.suggestionBar.updateState(emptyList(), sensitive, langTag)
            return
        }

        val seq = suggestionSeq.incrementAndGet()
        val textBefore = inputHandler.getTextBeforeCursor(ic, 80)

        nlpExecutor.execute {
            if (suggestionSeq.get() != seq) return@execute
            val result = suggestionEngine.getSuggestions(textBefore, isPersian, sensitive)
            mainHandler.post {
                if (suggestionSeq.get() == seq) {
                    currentAutoCorrectCandidate = result.autoCorrectReplacement
                    container.suggestionBar.updateState(result.chips, sensitive, langTag)
                }
            }
        }
    }

    private fun clearAutoCorrectUndoState() {
        lastAutoCorrectedOriginal = null
        lastAutoCorrectedReplacedWithSpace = null
    }

    private fun playClickSound(effect: Int) {
        if (!prefs.soundEnabled) return
        try {
            audioManager?.playSoundEffect(effect)
        } catch (_: Exception) {
            // Ignore if audio service is unavailable
        }
    }
}
