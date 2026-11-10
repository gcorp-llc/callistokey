package com.example.ime

/**
 * Constructs rows of keys for each supported keyboard mode and state.
 */
object KeyboardLayout {

    fun buildLayout(
        mode: KeyboardMode,
        shiftState: ShiftState,
        usePersianDigits: Boolean,
        enterLabel: String,
        lastAlphaMode: KeyboardMode = KeyboardMode.EN_ALPHA
    ): List<List<KeyModel>> {
        return when (mode) {
            KeyboardMode.EN_ALPHA -> buildEnglishLayout(shiftState, enterLabel)
            KeyboardMode.FA_ALPHA -> buildPersianLayout(enterLabel)
            KeyboardMode.NUMERIC -> buildNumericLayout(usePersianDigits, enterLabel, lastAlphaMode)
            KeyboardMode.SYMBOLS -> buildSymbolsLayout(enterLabel, lastAlphaMode)
        }
    }

    private fun buildEnglishLayout(shift: ShiftState, enterLabel: String): List<List<KeyModel>> {
        val isUpper = shift != ShiftState.OFF
        val row1Chars = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row2Chars = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row3Chars = listOf("z", "x", "c", "v", "b", "n", "m")

        val row1 = row1Chars.map { ch ->
            val text = if (isUpper) ch.uppercase() else ch
            KeyModel(KeyType.CHARACTER, text, text)
        }

        val row2 = row2Chars.map { ch ->
            val text = if (isUpper) ch.uppercase() else ch
            KeyModel(KeyType.CHARACTER, text, text)
        }

        val row3 = mutableListOf<KeyModel>()
        val shiftLabel = when (shift) {
            ShiftState.OFF -> "⇧"
            ShiftState.ON -> "⬆"
            ShiftState.CAPS_LOCK -> "⇪"
        }
        row3.add(KeyModel(KeyType.SHIFT, shiftLabel, isSpecial = true, weight = 1.4f))
        for (ch in row3Chars) {
            val text = if (isUpper) ch.uppercase() else ch
            row3.add(KeyModel(KeyType.CHARACTER, text, text))
        }
        row3.add(KeyModel(KeyType.BACKSPACE, "⌫", isSpecial = true, weight = 1.4f))

        val row4 = listOf(
            KeyModel(KeyType.MODE_SWITCH, "123", output = "NUMERIC", isSpecial = true, weight = 1.3f),
            KeyModel(KeyType.LANGUAGE_SWITCH, "FA", output = "FA", isSpecial = true, weight = 1.2f),
            KeyModel(KeyType.CHARACTER, ",", ",", weight = 1.0f),
            KeyModel(KeyType.SPACE, "Space", " ", weight = 4.2f),
            KeyModel(KeyType.CHARACTER, ".", ".", weight = 1.0f),
            KeyModel(KeyType.ENTER, enterLabel, "\n", isSpecial = true, weight = 1.5f)
        )

        return listOf(row1, row2, row3, row4)
    }

    private fun buildPersianLayout(enterLabel: String): List<List<KeyModel>> {
        // Standard Persian PC layout row mappings
        val row1Chars = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ")
        val row2Chars = listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ")
        val row3Chars = listOf("ظ", "ط", "ز", "ر", "ذ", "د", "ئ", "و", "پ", "ژ")

        val row1 = row1Chars.map { ch -> KeyModel(KeyType.CHARACTER, ch, ch) }
        val row2 = row2Chars.map { ch -> KeyModel(KeyType.CHARACTER, ch, ch) }

        val row3 = mutableListOf<KeyModel>()
        // Half-space / Zero-width non-joiner key (essential for proper Persian typing)
        row3.add(KeyModel(KeyType.ZWNJ, "نیم‌فاصله", "\u200C", isSpecial = true, weight = 1.3f))
        for (ch in row3Chars) {
            row3.add(KeyModel(KeyType.CHARACTER, ch, ch))
        }
        row3.add(KeyModel(KeyType.BACKSPACE, "⌫", isSpecial = true, weight = 1.3f))

        val row4 = listOf(
            KeyModel(KeyType.MODE_SWITCH, "۱۲۳", output = "NUMERIC", isSpecial = true, weight = 1.3f),
            KeyModel(KeyType.LANGUAGE_SWITCH, "EN", output = "EN", isSpecial = true, weight = 1.2f),
            KeyModel(KeyType.CHARACTER, "،", "،", weight = 1.0f),
            KeyModel(KeyType.SPACE, "فاصله", " ", weight = 4.2f),
            KeyModel(KeyType.CHARACTER, ".", ".", weight = 1.0f),
            KeyModel(KeyType.ENTER, enterLabel, "\n", isSpecial = true, weight = 1.5f)
        )

        return listOf(row1, row2, row3, row4)
    }

    private fun buildNumericLayout(
        usePersianDigits: Boolean,
        enterLabel: String,
        lastAlphaMode: KeyboardMode
    ): List<List<KeyModel>> {
        val d0 = if (usePersianDigits) "۰" else "0"
        val d1 = if (usePersianDigits) "۱" else "1"
        val d2 = if (usePersianDigits) "۲" else "2"
        val d3 = if (usePersianDigits) "۳" else "3"
        val d4 = if (usePersianDigits) "۴" else "4"
        val d5 = if (usePersianDigits) "۵" else "5"
        val d6 = if (usePersianDigits) "۶" else "6"
        val d7 = if (usePersianDigits) "۷" else "7"
        val d8 = if (usePersianDigits) "۸" else "8"
        val d9 = if (usePersianDigits) "۹" else "9"

        val toggleLabel = if (usePersianDigits) "123" else "۱۲۳"
        val alphaLabel = if (lastAlphaMode == KeyboardMode.FA_ALPHA) "فارسی" else "ABC"

        // Center 3 columns x 4 rows classic telephone layout flanked by navigation & math keys
        // Row 1: [+] | [1] [2] [3] | [Backspace]
        val row1 = listOf(
            KeyModel(KeyType.CHARACTER, "+", "+", weight = 1.1f, isSpecial = true),
            KeyModel(KeyType.CHARACTER, d1, d1, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d2, d2, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d3, d3, weight = 2.0f),
            KeyModel(KeyType.BACKSPACE, "⌫", isSpecial = true, weight = 1.3f)
        )

        // Row 2: [-] | [4] [5] [6] | [/]
        val row2 = listOf(
            KeyModel(KeyType.CHARACTER, "-", "-", weight = 1.1f, isSpecial = true),
            KeyModel(KeyType.CHARACTER, d4, d4, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d5, d5, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d6, d6, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, "/", "/", weight = 1.3f, isSpecial = true)
        )

        // Row 3: [Script Toggle] | [7] [8] [9] | [SYM]
        val row3 = listOf(
            KeyModel(KeyType.NUMERIC_SCRIPT_TOGGLE, toggleLabel, isSpecial = true, weight = 1.1f),
            KeyModel(KeyType.CHARACTER, d7, d7, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d8, d8, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d9, d9, weight = 2.0f),
            KeyModel(KeyType.MODE_SWITCH, "SYM", output = "SYMBOLS", isSpecial = true, weight = 1.3f)
        )

        // Row 4: [ABC / return] | [*] [0] [#] | [Enter]
        val row4 = listOf(
            KeyModel(KeyType.MODE_SWITCH, alphaLabel, output = "ALPHA", isSpecial = true, weight = 1.2f),
            KeyModel(KeyType.CHARACTER, "*", "*", weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d0, d0, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, "#", "#", weight = 2.0f),
            KeyModel(KeyType.ENTER, enterLabel, "\n", isSpecial = true, weight = 1.3f)
        )

        return listOf(row1, row2, row3, row4)
    }

    private fun buildSymbolsLayout(
        enterLabel: String,
        lastAlphaMode: KeyboardMode
    ): List<List<KeyModel>> {
        val alphaLabel = if (lastAlphaMode == KeyboardMode.FA_ALPHA) "فارسی" else "ABC"

        val row1 = listOf(
            KeyModel(KeyType.CHARACTER, "@", "@"),
            KeyModel(KeyType.CHARACTER, "#", "#"),
            KeyModel(KeyType.CHARACTER, "$", "$"),
            KeyModel(KeyType.CHARACTER, "%", "%"),
            KeyModel(KeyType.CHARACTER, "&", "&"),
            KeyModel(KeyType.CHARACTER, "*", "*"),
            KeyModel(KeyType.CHARACTER, "-", "-"),
            KeyModel(KeyType.CHARACTER, "+", "+"),
            KeyModel(KeyType.CHARACTER, "=", "="),
            KeyModel(KeyType.CHARACTER, "/", "/")
        )

        val row2 = listOf(
            KeyModel(KeyType.CHARACTER, ":", ":"),
            KeyModel(KeyType.CHARACTER, ";", ";"),
            KeyModel(KeyType.CHARACTER, "!", "!"),
            KeyModel(KeyType.CHARACTER, "?", "?"),
            KeyModel(KeyType.CHARACTER, "\"", "\""),
            KeyModel(KeyType.CHARACTER, "'", "'"),
            KeyModel(KeyType.CHARACTER, "(", "("),
            KeyModel(KeyType.CHARACTER, ")", ")"),
            KeyModel(KeyType.CHARACTER, "[", "["),
            KeyModel(KeyType.CHARACTER, "]", "]")
        )

        val row3 = listOf(
            KeyModel(KeyType.CHARACTER, "{", "{"),
            KeyModel(KeyType.CHARACTER, "}", "}"),
            KeyModel(KeyType.CHARACTER, "<", "<"),
            KeyModel(KeyType.CHARACTER, ">", ">"),
            KeyModel(KeyType.CHARACTER, "_", "_"),
            KeyModel(KeyType.CHARACTER, "\\", "\\"),
            KeyModel(KeyType.CHARACTER, "|", "|"),
            KeyModel(KeyType.CHARACTER, "~", "~"),
            KeyModel(KeyType.CHARACTER, "«", "«"),
            KeyModel(KeyType.BACKSPACE, "⌫", isSpecial = true, weight = 1.2f)
        )

        val row4 = listOf(
            KeyModel(KeyType.MODE_SWITCH, alphaLabel, output = "ALPHA", isSpecial = true, weight = 1.3f),
            KeyModel(KeyType.MODE_SWITCH, "123", output = "NUMERIC", isSpecial = true, weight = 1.2f),
            KeyModel(KeyType.CHARACTER, ",", ",", weight = 1.0f),
            KeyModel(KeyType.SPACE, "Space", " ", weight = 4.2f),
            KeyModel(KeyType.CHARACTER, ".", ".", weight = 1.0f),
            KeyModel(KeyType.ENTER, enterLabel, "\n", isSpecial = true, weight = 1.5f)
        )

        return listOf(row1, row2, row3, row4)
    }
}
