package com.example.ime

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/**
 * Handles communication with the active editor's InputConnection.
 * Strictly stateless and memory-leak free: no typed text is ever retained or logged.
 */
class InputConnectionHandler {

    fun commitText(ic: InputConnection?, text: String) {
        if (ic == null || text.isEmpty()) return
        ic.commitText(text, 1)
    }

    fun handleDelete(ic: InputConnection?) {
        if (ic == null) return

        // Check if there is selected text to delete
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }

        // Check for Unicode surrogate pairs (e.g., emojis or extended characters)
        val before = ic.getTextBeforeCursor(2, 0)
        if (!before.isNullOrEmpty()) {
            val lastChar = before.last()
            if (Character.isLowSurrogate(lastChar) && before.length >= 2 && Character.isHighSurrogate(before[before.length - 2])) {
                ic.deleteSurroundingText(2, 0)
                return
            }
        }

        // Standard single character deletion
        val deleted = ic.deleteSurroundingText(1, 0)
        if (!deleted) {
            // Fallback via KeyEvent for webviews or custom editors
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

        if (!isMultiline && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
    }

    fun getEnterLabel(editorInfo: EditorInfo?): String {
        if (editorInfo == null) return "⏎"

        val isMultiline = (editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        if (isMultiline) return "⏎"

        return when (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "Search"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_DONE -> "Done"
            else -> "⏎"
        }
    }
}
