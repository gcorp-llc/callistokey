package com.example.ime

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/**
 * Handles communication with the active editor's InputConnection.
 * Uses atomic batch edits for instant spell correction and suggestion insertion.
 */
class InputConnectionHandler {

    fun commitText(ic: InputConnection?, text: String) {
        if (ic == null || text.isEmpty()) return
        ic.commitText(text, 1)
    }

    fun getTextBeforeCursor(ic: InputConnection?, maxChars: Int = 80): String {
        if (ic == null) return ""
        return try {
            ic.getTextBeforeCursor(maxChars, 0)?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun replaceCurrentWord(ic: InputConnection?, currentWordLength: Int, newText: String) {
        if (ic == null) return
        ic.beginBatchEdit()
        try {
            if (currentWordLength > 0) {
                ic.deleteSurroundingText(currentWordLength, 0)
            }
            ic.commitText(newText, 1)
        } finally {
            ic.endBatchEdit()
        }
    }

    fun undoReplacement(ic: InputConnection?, replacementLength: Int, originalText: String): Boolean {
        if (ic == null || replacementLength <= 0) return false
        ic.beginBatchEdit()
        return try {
            ic.deleteSurroundingText(replacementLength, 0)
            ic.commitText(originalText, 1)
            true
        } finally {
            ic.endBatchEdit()
        }
    }

    fun handleDelete(ic: InputConnection?) {
        if (ic == null) return

        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }

        // Handle Unicode surrogate pairs (e.g., emojis or extended glyphs)
        val before = ic.getTextBeforeCursor(2, 0)
        if (!before.isNullOrEmpty()) {
            val lastChar = before.last()
            if (Character.isLowSurrogate(lastChar) &&
                before.length >= 2 &&
                Character.isHighSurrogate(before[before.length - 2])
            ) {
                ic.deleteSurroundingText(2, 0)
                return
            }
        }

        val deleted = ic.deleteSurroundingText(1, 0)
        if (!deleted) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
    }

    fun handleEnter(ic: InputConnection?, editorInfo: EditorInfo?) {
        if (ic == null) return

        if (editorInfo == null) {
            ic.commitText("\n", 1)
            return
        }

        val isMultiline = (editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION

        if (!isMultiline &&
            action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
    }

    fun getEnterLabel(editorInfo: EditorInfo?): String {
        if (editorInfo == null) return "ENTER_ICON"

        val isMultiline = (editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        if (isMultiline) return "ENTER_ICON"

        return when (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "Search"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_DONE -> "Done"
            else -> "ENTER_ICON"
        }
    }

    fun isSensitiveEditor(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false
        val inputType = editorInfo.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val flags = inputType and InputType.TYPE_MASK_FLAGS

        if (inputClass == InputType.TYPE_CLASS_NUMBER &&
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) {
            return true
        }
        if (inputClass == InputType.TYPE_CLASS_TEXT && (
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )
        ) {
            return true
        }
        if ((flags and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
            return true
        }
        return false
    }
}
