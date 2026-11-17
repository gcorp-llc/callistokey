package com.example.ime

/**
 * Constructs rows of keys for each supported CallistoKey layout mode and state.
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
            KeyboardMode.SYMBOLS_EXTENDED -> buildExtendedSymbolsLayout(enterLabel, lastAlphaMode)
        }
    }

    private fun buildEnglishLayout(shift: ShiftState, enterLabel: String): List<List<KeyModel>> {
        val isUpper = shift != ShiftState.OFF
        val row1Chars = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val row1Digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val row2Chars = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val row3Chars = listOf("z", "x", "c", "v", "b", "n", "m")

        val row1 = row1Chars.mapIndexed { idx, ch ->
            val text = if (isUpper) ch.uppercase() else ch
            val digit = row1Digits[idx]
            val alts = buildList {
                add(digit)
                when (ch) {
                    "e" -> addAll(listOf("é", "è", "ê", "ë"))
                    "u" -> addAll(listOf("ü", "û", "ù", "ú"))
                    "i" -> addAll(listOf("î", "ï", "í", "ì"))
                    "o" -> addAll(listOf("ö", "ô", "ò", "ó"))
                }
            }
            KeyModel(KeyType.CHARACTER, text, text, hintLabel = digit, altOutputs = alts)
        }

        val row2 = row2Chars.map { ch ->
            val text = if (isUpper) ch.uppercase() else ch
            val alts = when (ch) {
                "a" -> listOf("@", "á", "à", "â", "ä")
                "s" -> listOf("$", "ß", "ś", "š")
                else -> emptyList()
            }
            KeyModel(KeyType.CHARACTER, text, text, altOutputs = alts)
        }

        val row3 = mutableListOf<KeyModel>()
        val shiftLabel = when (shift) {
            ShiftState.OFF -> "SHIFT_OFF"
            ShiftState.ON -> "SHIFT_ON"
            ShiftState.CAPS_LOCK -> "SHIFT_LOCK"
        }
        row3.add(KeyModel(KeyType.SHIFT, shiftLabel, isSpecial = true, weight = 1.45f))
        for (ch in row3Chars) {
            val text = if (isUpper) ch.uppercase() else ch
            row3.add(KeyModel(KeyType.CHARACTER, text, text))
        }
        row3.add(KeyModel(KeyType.BACKSPACE, "BACKSPACE", isSpecial = true, weight = 1.45f))

        // Bottom control row with direct 123, Symbols (!#?), comma, Swipeable Spacebar, period, Enter
        val row4 = listOf(
            KeyModel(KeyType.MODE_SWITCH, "123", output = "NUMERIC", isSpecial = true, weight = 1.25f),
            KeyModel(KeyType.MODE_SWITCH, "!#?", output = "SYMBOLS", isSpecial = true, weight = 1.2f),
            KeyModel(KeyType.CHARACTER, ",", ",", altOutputs = listOf(";", ":", "\"", "'"), weight = 0.95f),
            KeyModel(KeyType.SPACE, "CallistoKey • EN", " ", weight = 3.9f),
            KeyModel(KeyType.CHARACTER, ".", ".", hintLabel = "!", altOutputs = listOf("!", "?", ",", ";", ":", "…"), weight = 0.95f),
            KeyModel(KeyType.ENTER, enterLabel, "\n", isSpecial = true, weight = 1.55f)
        )

        return listOf(row1, row2, row3, row4)
    }

    private fun buildPersianLayout(enterLabel: String): List<List<KeyModel>> {
        val row1Chars = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ")
        val row1Digits = listOf("۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹", "۰", null, null)
        val row2Chars = listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ")
        val row3Chars = listOf("ظ", "ط", "ز", "ر", "ذ", "د", "ئ", "و", "پ", "ژ")

        val row1 = row1Chars.mapIndexed { idx, ch ->
            val digit = row1Digits[idx]
            val alts = buildList {
                if (digit != null) add(digit)
                when (ch) {
                    "ه" -> addAll(listOf("ة", "ۀ"))
                }
            }
            KeyModel(KeyType.CHARACTER, ch, ch, hintLabel = digit, altOutputs = alts)
        }

        val row2 = row2Chars.map { ch ->
            val alts = when (ch) {
                "ا" -> listOf("آ", "أ", "إ", "ء")
                "ی" -> listOf("ي", "ئ")
                "ک" -> listOf("ك")
                else -> emptyList()
            }
            KeyModel(KeyType.CHARACTER, ch, ch, hintLabel = if (ch == "ا") "آ" else null, altOutputs = alts)
        }

        val row3 = mutableListOf<KeyModel>()
        row3.add(KeyModel(KeyType.ZWNJ, "ZWNJ", "\u200C", isSpecial = true, weight = 1.25f))
        for (ch in row3Chars) {
            val alts = when (ch) {
                "و" -> listOf("ؤ")
                else -> emptyList()
            }
            row3.add(KeyModel(KeyType.CHARACTER, ch, ch, altOutputs = alts))
        }
        row3.add(KeyModel(KeyType.BACKSPACE, "BACKSPACE", isSpecial = true, weight = 1.35f))

        // Bottom control row with direct ۱۲۳, Symbols (؟#؛), comma, Swipeable Spacebar, period, Enter
        val row4 = listOf(
            KeyModel(KeyType.MODE_SWITCH, "۱۲۳", output = "NUMERIC", isSpecial = true, weight = 1.25f),
            KeyModel(KeyType.MODE_SWITCH, "؟#؛", output = "SYMBOLS", isSpecial = true, weight = 1.2f),
            KeyModel(KeyType.CHARACTER, "،", "،", altOutputs = listOf("؛", "«", "»", ":"), weight = 0.95f),
            KeyModel(KeyType.SPACE, "CallistoKey • فارسی", " ", weight = 3.9f),
            KeyModel(KeyType.CHARACTER, ".", ".", hintLabel = "؟", altOutputs = listOf("؟", "!", "،", "؛", "…"), weight = 0.95f),
            KeyModel(KeyType.ENTER, enterLabel, "\n", isSpecial = true, weight = 1.55f)
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

        // Classic 3x4 Telephone Keypad flanked by utility/math keys
        val row1 = listOf(
            KeyModel(KeyType.CHARACTER, "+", "+", weight = 1.1f, isSpecial = true),
            KeyModel(KeyType.CHARACTER, d1, d1, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d2, d2, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d3, d3, weight = 2.0f),
            KeyModel(KeyType.BACKSPACE, "BACKSPACE", isSpecial = true, weight = 1.3f)
        )

        val row2 = listOf(
            KeyModel(KeyType.CHARACTER, "-", "-", weight = 1.1f, isSpecial = true),
            KeyModel(KeyType.CHARACTER, d4, d4, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d5, d5, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d6, d6, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, "/", "/", weight = 1.3f, isSpecial = true)
        )

        val row3 = listOf(
            KeyModel(KeyType.NUMERIC_SCRIPT_TOGGLE, toggleLabel, isSpecial = true, weight = 1.1f),
            KeyModel(KeyType.CHARACTER, d7, d7, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d8, d8, weight = 2.0f),
            KeyModel(KeyType.CHARACTER, d9, d9, weight = 2.0f),
            KeyModel(KeyType.MODE_SWITCH, "!#?", output = "SYMBOLS", isSpecial = true, weight = 1.3f)
        )

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
        val isPersian = lastAlphaMode == KeyboardMode.FA_ALPHA

        val row1 = listOf(
            KeyModel(KeyType.CHARACTER, "@", "@"),
            KeyModel(KeyType.CHARACTER, "#", "#"),
            KeyModel(KeyType.CHARACTER, "$", "$", altOutputs = listOf("﷼", "€", "£")),
            KeyModel(KeyType.CHARACTER, "%", "%", altOutputs = listOf("٪")),
            KeyModel(KeyType.CHARACTER, "&", "&"),
            KeyModel(KeyType.CHARACTER, "*", "*"),
            KeyModel(KeyType.CHARACTER, "-", "-", altOutputs = listOf("—", "–")),
            KeyModel(KeyType.CHARACTER, "+", "+"),
            KeyModel(KeyType.CHARACTER, "=", "="),
            KeyModel(KeyType.CHARACTER, "/", "/")
        )

        val row2 = listOf(
            KeyModel(KeyType.CHARACTER, ":", ":"),
            KeyModel(KeyType.CHARACTER, if (isPersian) "؛" else ";", if (isPersian) "؛" else ";", altOutputs = listOf(";", "؛")),
            KeyModel(KeyType.CHARACTER, "!", "!"),
            KeyModel(KeyType.CHARACTER, if (isPersian) "؟" else "?", if (isPersian) "؟" else "?", altOutputs = listOf("?", "؟")),
            KeyModel(KeyType.CHARACTER, "\"", "\""),
            KeyModel(KeyType.CHARACTER, "'", "'"),
            KeyModel(KeyType.CHARACTER, "(", "("),
            KeyModel(KeyType.CHARACTER, ")", ")"),
            KeyModel(KeyType.CHARACTER, "[", "["),
            KeyModel(KeyType.CHARACTER, "]", "]")
        )

        val row3 = listOf(
            KeyModel(KeyType.MODE_SWITCH, "1/2", output = "SYMBOLS_EXT", isSpecial = true, weight = 1.3f),
            KeyModel(KeyType.CHARACTER, "{", "{"),
            KeyModel(KeyType.CHARACTER, "}", "}"),
            KeyModel(KeyType.CHARACTER, "<", "<"),
            KeyModel(KeyType.CHARACTER, ">", ">"),
            KeyModel(KeyType.CHARACTER, "_", "_"),
            KeyModel(KeyType.CHARACTER, "\\", "\\"),
            KeyModel(KeyType.CHARACTER, "|", "|"),
            KeyModel(KeyType.CHARACTER, "~", "~"),
            KeyModel(KeyType.BACKSPACE, "BACKSPACE", isSpecial = true, weight = 1.3f)
        )

        val row4 = listOf(
            KeyModel(KeyType.MODE_SWITCH, alphaLabel, output = "ALPHA", isSpecial = true, weight = 1.3f),
            KeyModel(KeyType.MODE_SWITCH, if (isPersian) "۱۲۳" else "123", output = "NUMERIC", isSpecial = true, weight = 1.2f),
            KeyModel(KeyType.CHARACTER, if (isPersian) "،" else ",", if (isPersian) "،" else ","),
            KeyModel(KeyType.SPACE, if (isPersian) "فاصله" else "Space", " ", weight = 3.9f),
            KeyModel(KeyType.CHARACTER, ".", "."),
            KeyModel(KeyType.ENTER, enterLabel, "\n", isSpecial = true, weight = 1.5f)
        )

        return listOf(row1, row2, row3, row4)
    }

    private fun buildExtendedSymbolsLayout(
        enterLabel: String,
        lastAlphaMode: KeyboardMode
    ): List<List<KeyModel>> {
        val alphaLabel = if (lastAlphaMode == KeyboardMode.FA_ALPHA) "فارسی" else "ABC"
        val isPersian = lastAlphaMode == KeyboardMode.FA_ALPHA

        val row1 = listOf(
            KeyModel(KeyType.CHARACTER, "«", "«"),
            KeyModel(KeyType.CHARACTER, "»", "»"),
            KeyModel(KeyType.CHARACTER, "،", "،"),
            KeyModel(KeyType.CHARACTER, "؛", "؛"),
            KeyModel(KeyType.CHARACTER, "؟", "؟"),
            KeyModel(KeyType.CHARACTER, "…", "…"),
            KeyModel(KeyType.CHARACTER, "﷼", "﷼"),
            KeyModel(KeyType.CHARACTER, "€", "€"),
            KeyModel(KeyType.CHARACTER, "£", "£"),
            KeyModel(KeyType.CHARACTER, "°", "°")
        )

        val row2 = listOf(
            KeyModel(KeyType.CHARACTER, "•", "•"),
            KeyModel(KeyType.CHARACTER, "^", "^"),
            KeyModel(KeyType.CHARACTER, "`", "`"),
            KeyModel(KeyType.CHARACTER, "±", "±"),
            KeyModel(KeyType.CHARACTER, "×", "×"),
            KeyModel(KeyType.CHARACTER, "÷", "÷"),
            KeyModel(KeyType.CHARACTER, "≠", "≠"),
            KeyModel(KeyType.CHARACTER, "≈", "≈"),
            KeyModel(KeyType.CHARACTER, "≤", "≤"),
            KeyModel(KeyType.CHARACTER, "≥", "≥")
        )

        val row3 = listOf(
            KeyModel(KeyType.MODE_SWITCH, "2/2", output = "SYMBOLS", isSpecial = true, weight = 1.3f),
            KeyModel(KeyType.CHARACTER, "©", "©"),
            KeyModel(KeyType.CHARACTER, "®", "®"),
            KeyModel(KeyType.CHARACTER, "™", "™"),
            KeyModel(KeyType.CHARACTER, "§", "§"),
            KeyModel(KeyType.CHARACTER, "¶", "¶"),
            KeyModel(KeyType.CHARACTER, "✓", "✓"),
            KeyModel(KeyType.CHARACTER, "→", "→"),
            KeyModel(KeyType.CHARACTER, "←", "←"),
            KeyModel(KeyType.BACKSPACE, "BACKSPACE", isSpecial = true, weight = 1.3f)
        )

        val row4 = listOf(
            KeyModel(KeyType.MODE_SWITCH, alphaLabel, output = "ALPHA", isSpecial = true, weight = 1.3f),
            KeyModel(KeyType.MODE_SWITCH, if (isPersian) "۱۲۳" else "123", output = "NUMERIC", isSpecial = true, weight = 1.2f),
            KeyModel(KeyType.CHARACTER, ",", ","),
            KeyModel(KeyType.SPACE, if (isPersian) "فاصله" else "Space", " ", weight = 3.9f),
            KeyModel(KeyType.CHARACTER, ".", "."),
            KeyModel(KeyType.ENTER, enterLabel, "\n", isSpecial = true, weight = 1.5f)
        )

        return listOf(row1, row2, row3, row4)
    }
}
