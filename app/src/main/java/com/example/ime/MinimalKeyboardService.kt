package com.example.ime

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import com.example.ime.views.CustomKeyboardView

/**
 * Production Android Input Method Service implementing the Minimal Persian & English Keyboard.
 * Strictly offline, zero analytics, zero word predictions, GitHub Dark styled.
 */
class MinimalKeyboardService : InputMethodService(), CustomKeyboardView.OnKeyboardActionListener {

    private var keyboardView: CustomKeyboardView? = null
    private val inputHandler = InputConnectionHandler()
    private var audioManager: AudioManager? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as? AudioManager
    }

    override fun onCreateInputView(): View {
        val view = CustomKeyboardView(this)
        view.listener = this
        keyboardView = view
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        keyboardView?.let { view ->
            // Update Enter key label to match Editor action (Done, Send, Search, Next, Go)
            val enterLabel = inputHandler.getEnterLabel(info)
            view.setEnterLabel(enterLabel)

            // Auto-switch to Numeric keypad for phone, numbers, and dates
            val inputClass = info?.inputType?.and(InputType.TYPE_MASK_CLASS)
            if (inputClass == InputType.TYPE_CLASS_NUMBER ||
                inputClass == InputType.TYPE_CLASS_PHONE ||
                inputClass == InputType.TYPE_CLASS_DATETIME
            ) {
                view.setMode(KeyboardMode.NUMERIC)
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        keyboardView?.setShift(ShiftState.OFF)
    }

    override fun onText(text: String) {
        inputHandler.commitText(currentInputConnection, text)
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
    }

    override fun onBackspace() {
        inputHandler.handleDelete(currentInputConnection)
        playClickSound(AudioManager.FX_KEYPRESS_DELETE)
    }

    override fun onEnter() {
        inputHandler.handleEnter(currentInputConnection, currentInputEditorInfo)
        playClickSound(AudioManager.FX_KEYPRESS_RETURN)
    }

    override fun onShiftToggle() {
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
    }

    override fun onShiftDoubleTap() {
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
    }

    override fun onLanguageSwitch() {
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
    }

    override fun onModeSwitch(target: String) {
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
    }

    override fun onNumericScriptToggle() {
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
    }

    override fun onZwnj() {
        inputHandler.commitText(currentInputConnection, "\u200C")
        playClickSound(AudioManager.FX_KEYPRESS_STANDARD)
    }

    private fun playClickSound(effect: Int) {
        try {
            audioManager?.playSoundEffect(effect)
        } catch (_: Exception) {
            // Ignore if audio manager is unavailable
        }
    }
}
