package com.example

import com.example.ime.KeyType
import com.example.ime.KeyboardLayout
import com.example.ime.KeyboardMode
import com.example.ime.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardFunctionalTest {

    @Test
    fun testEnglishQwertyLayoutStructure() {
        val layout = KeyboardLayout.buildLayout(
            mode = KeyboardMode.EN_ALPHA,
            shiftState = ShiftState.OFF,
            usePersianDigits = false,
            enterLabel = "⏎"
        )
        assertEquals(4, layout.size)

        // Row 1: q w e r t y u i o p (10 keys)
        val row1 = layout[0]
        assertEquals(10, row1.size)
        assertEquals("q", row1[0].output)
        assertEquals("p", row1[9].output)

        // Row 2: a s d f g h j k l (9 keys)
        val row2 = layout[1]
        assertEquals(9, row2.size)
        assertEquals("a", row2[0].output)
        assertEquals("l", row2[8].output)

        // Row 3: Shift, z x c v b n m, Backspace (9 keys)
        val row3 = layout[2]
        assertEquals(9, row3.size)
        assertEquals(KeyType.SHIFT, row3[0].type)
        assertEquals("z", row3[1].output)
        assertEquals("m", row3[7].output)
        assertEquals(KeyType.BACKSPACE, row3[8].type)

        // Row 4: 123, FA, comma, Space, period, Enter (6 keys)
        val row4 = layout[3]
        assertEquals(6, row4.size)
        assertEquals("123", row4[0].label)
        assertEquals("FA", row4[1].label)
        assertEquals(",", row4[2].output)
        assertEquals(" ", row4[3].output)
        assertEquals(".", row4[4].output)
        assertEquals(KeyType.ENTER, row4[5].type)
    }

    @Test
    fun testEnglishShiftUppercaseTransformation() {
        val layoutUpper = KeyboardLayout.buildLayout(
            mode = KeyboardMode.EN_ALPHA,
            shiftState = ShiftState.ON,
            usePersianDigits = false,
            enterLabel = "⏎"
        )
        assertEquals("Q", layoutUpper[0][0].output)
        assertEquals("W", layoutUpper[0][1].output)
        assertEquals("A", layoutUpper[1][0].output)
        assertEquals("Z", layoutUpper[2][1].output)
    }

    @Test
    fun testPersianStandardPcLayoutKeyMapping() {
        val layout = KeyboardLayout.buildLayout(
            mode = KeyboardMode.FA_ALPHA,
            shiftState = ShiftState.OFF,
            usePersianDigits = false,
            enterLabel = "⏎"
        )
        assertEquals(4, layout.size)

        // Row 1: ض ص ث ق ف غ ع ه خ ح ج چ (12 keys)
        val expectedRow1 = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ")
        val row1 = layout[0]
        assertEquals(12, row1.size)
        for (i in expectedRow1.indices) {
            assertEquals(expectedRow1[i], row1[i].output)
        }

        // Row 2: ش س ی ب ل ا ت ن م ک گ (11 keys)
        val expectedRow2 = listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ")
        val row2 = layout[1]
        assertEquals(11, row2.size)
        for (i in expectedRow2.indices) {
            assertEquals(expectedRow2[i], row2[i].output)
        }

        // Row 3: نیم‌فاصله, ظ ط ز ر ذ د ئ و پ ژ, Backspace
        val expectedRow3Letters = listOf("ظ", "ط", "ز", "ر", "ذ", "د", "ئ", "و", "پ", "ژ")
        val row3 = layout[2]
        assertEquals(12, row3.size) // ZWNJ + 10 letters + Backspace
        assertEquals(KeyType.ZWNJ, row3[0].type)
        assertEquals("\u200C", row3[0].output)
        for (i in expectedRow3Letters.indices) {
            assertEquals(expectedRow3Letters[i], row3[i + 1].output)
        }
        assertEquals(KeyType.BACKSPACE, row3[11].type)

        // Row 4: ۱۲۳, EN, ،, فاصله, ., Enter
        val row4 = layout[3]
        assertEquals(6, row4.size)
        assertEquals("۱۲۳", row4[0].label)
        assertEquals("EN", row4[1].label)
        assertEquals("،", row4[2].output)
        assertEquals(" ", row4[3].output)
        assertEquals(".", row4[4].output)
        assertEquals(KeyType.ENTER, row4[5].type)
    }

    @Test
    fun testClassicTelephoneKeypadLayout() {
        val layout = KeyboardLayout.buildLayout(
            mode = KeyboardMode.NUMERIC,
            shiftState = ShiftState.OFF,
            usePersianDigits = false,
            enterLabel = "⏎"
        )
        assertEquals(4, layout.size)

        // Row 1: + | 1 2 3 | ⌫
        assertEquals("+", layout[0][0].output)
        assertEquals("1", layout[0][1].output)
        assertEquals("2", layout[0][2].output)
        assertEquals("3", layout[0][3].output)
        assertEquals(KeyType.BACKSPACE, layout[0][4].type)

        // Row 2: - | 4 5 6 | /
        assertEquals("-", layout[1][0].output)
        assertEquals("4", layout[1][1].output)
        assertEquals("5", layout[1][2].output)
        assertEquals("6", layout[1][3].output)
        assertEquals("/", layout[1][4].output)

        // Row 3: toggle | 7 8 9 | SYM
        assertEquals(KeyType.NUMERIC_SCRIPT_TOGGLE, layout[2][0].type)
        assertEquals("7", layout[2][1].output)
        assertEquals("8", layout[2][2].output)
        assertEquals("9", layout[2][3].output)
        assertEquals("SYM", layout[2][4].label)

        // Row 4: ABC | * 0 # | Enter
        assertEquals("ABC", layout[3][0].label)
        assertEquals("*", layout[3][1].output)
        assertEquals("0", layout[3][2].output)
        assertEquals("#", layout[3][3].output)
        assertEquals(KeyType.ENTER, layout[3][4].type)
    }

    @Test
    fun testPersianDigitsToggleOnNumericKeypad() {
        val layoutFa = KeyboardLayout.buildLayout(
            mode = KeyboardMode.NUMERIC,
            shiftState = ShiftState.OFF,
            usePersianDigits = true,
            enterLabel = "⏎"
        )
        assertEquals("۱", layoutFa[0][1].output)
        assertEquals("۲", layoutFa[0][2].output)
        assertEquals("۳", layoutFa[0][3].output)
        assertEquals("۴", layoutFa[1][1].output)
        assertEquals("۵", layoutFa[1][2].output)
        assertEquals("۶", layoutFa[1][3].output)
        assertEquals("۷", layoutFa[2][1].output)
        assertEquals("۸", layoutFa[2][2].output)
        assertEquals("۹", layoutFa[2][3].output)
        assertEquals("۰", layoutFa[3][2].output)
    }

    @Test
    fun testSymbolsLayoutContainsRequiredSymbols() {
        val layout = KeyboardLayout.buildLayout(
            mode = KeyboardMode.SYMBOLS,
            shiftState = ShiftState.OFF,
            usePersianDigits = false,
            enterLabel = "⏎"
        )
        val allSymbols = layout.flatMap { row -> row.map { it.output } }.toSet()

        val required = listOf("@", "#", "$", "%", "&", "*", "-", "+", "=", "/", ":", ";", "!", "?", "(", ")", "[", "]", "{", "}", "\"", "'")
        for (sym in required) {
            assertTrue("Expected symbol $sym to be in symbols layout", allSymbols.contains(sym))
        }
    }
}
