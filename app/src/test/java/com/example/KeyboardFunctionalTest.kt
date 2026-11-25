package com.example

import com.example.ime.KeyType
import com.example.ime.KeyboardLayout
import com.example.ime.KeyboardMode
import com.example.ime.ShiftState
import com.example.ime.nlp.CoreLexicon
import com.example.ime.nlp.SpellChecker
import com.example.ime.nlp.SuggestionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardFunctionalTest {

    @Test
    fun testEnglishQwertyLayoutStructureAndSymbolsButton() {
        val layout = KeyboardLayout.buildLayout(
            mode = KeyboardMode.EN_ALPHA,
            shiftState = ShiftState.OFF,
            usePersianDigits = false,
            enterLabel = "ENTER_ICON"
        )
        assertEquals(4, layout.size)

        // Row 1: q..p with digit hints 1..0
        val rowLayout = KeyboardLayout.buildRowLayout(
            mode = KeyboardMode.EN_ALPHA,
            shiftState = ShiftState.OFF,
            usePersianDigits = false,
            enterLabel = "ENTER_ICON"
        )
        assertEquals(0f, rowLayout[0].horizontalInsetWeight)
        assertEquals(0.45f, rowLayout[1].horizontalInsetWeight)

        val row1 = layout[0]
        assertEquals(10, row1.size)
        assertEquals("q", row1[0].output)
        assertEquals("1", row1[0].hintLabel)
        assertEquals("p", row1[9].output)
        assertEquals("0", row1[9].hintLabel)

        // Row 2: a..l
        val row2 = layout[1]
        assertEquals(9, row2.size)
        assertEquals("a", row2[0].output)
        assertEquals("l", row2[8].output)

        // Row 3: Shift, z..m, Backspace
        val row3 = layout[2]
        assertEquals(9, row3.size)
        assertEquals(KeyType.SHIFT, row3[0].type)
        assertEquals("z", row3[1].output)
        assertEquals("m", row3[7].output)
        assertEquals(KeyType.BACKSPACE, row3[8].type)

        // Row 4: 123, !#? (Symbols), comma, Space, period, Enter
        val row4 = layout[3]
        assertEquals(6, row4.size)
        assertEquals("123", row4[0].label)
        assertEquals("!#?", row4[1].label)
        assertEquals("SYMBOLS", row4[1].output)
        assertEquals(",", row4[2].output)
        assertEquals(" ", row4[3].output)
        assertTrue(row4[3].label.contains("CallistoKey"))
        assertEquals(".", row4[4].output)
        assertEquals(KeyType.ENTER, row4[5].type)
    }

    @Test
    fun testEnglishShiftUppercaseTransformation() {
        val layoutUpper = KeyboardLayout.buildLayout(
            mode = KeyboardMode.EN_ALPHA,
            shiftState = ShiftState.ON,
            usePersianDigits = false,
            enterLabel = "ENTER_ICON"
        )
        assertEquals("Q", layoutUpper[0][0].output)
        assertEquals("W", layoutUpper[0][1].output)
        assertEquals("A", layoutUpper[1][0].output)
        assertEquals("Z", layoutUpper[2][1].output)
    }

    @Test
    fun testPersianStandardPcLayoutAndSymbolsButton() {
        val layout = KeyboardLayout.buildLayout(
            mode = KeyboardMode.FA_ALPHA,
            shiftState = ShiftState.OFF,
            usePersianDigits = false,
            enterLabel = "ENTER_ICON"
        )
        assertEquals(4, layout.size)

        val expectedRow1 = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ")
        val row1 = layout[0]
        assertEquals(12, row1.size)
        for (i in expectedRow1.indices) {
            assertEquals(expectedRow1[i], row1[i].output)
        }
        assertEquals("۱", row1[0].hintLabel)
        assertEquals("۰", row1[9].hintLabel)

        val expectedRow2 = listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ")
        val row2 = layout[1]
        assertEquals(11, row2.size)
        for (i in expectedRow2.indices) {
            assertEquals(expectedRow2[i], row2[i].output)
        }

        val expectedRow3Letters = listOf("ظ", "ط", "ز", "ر", "ذ", "د", "ئ", "و", "پ", "ژ")
        val row3 = layout[2]
        assertEquals(12, row3.size)
        assertEquals(KeyType.ZWNJ, row3[0].type)
        assertEquals("\u200C", row3[0].output)
        for (i in expectedRow3Letters.indices) {
            assertEquals(expectedRow3Letters[i], row3[i + 1].output)
        }
        assertEquals(KeyType.BACKSPACE, row3[11].type)

        // Row 4: ۱۲۳, ؟#؛ (Symbols), ،, Space, ., Enter
        val row4 = layout[3]
        assertEquals(6, row4.size)
        assertEquals("۱۲۳", row4[0].label)
        assertEquals("؟#؛", row4[1].label)
        assertEquals("SYMBOLS", row4[1].output)
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
            enterLabel = "ENTER_ICON"
        )
        assertEquals(4, layout.size)
        assertEquals("1", layout[0][1].output)
        assertEquals("2", layout[0][2].output)
        assertEquals("3", layout[0][3].output)
        assertEquals("4", layout[1][1].output)
        assertEquals("5", layout[1][2].output)
        assertEquals("6", layout[1][3].output)
        assertEquals("7", layout[2][1].output)
        assertEquals("8", layout[2][2].output)
        assertEquals("9", layout[2][3].output)
        assertEquals("*", layout[3][1].output)
        assertEquals("0", layout[3][2].output)
        assertEquals("#", layout[3][3].output)
    }

    @Test
    fun testSymbolsAndExtendedSymbolsCoverage() {
        val page1 = KeyboardLayout.buildLayout(
            mode = KeyboardMode.SYMBOLS,
            shiftState = ShiftState.OFF,
            usePersianDigits = false,
            enterLabel = "ENTER_ICON"
        )
        val page2 = KeyboardLayout.buildLayout(
            mode = KeyboardMode.SYMBOLS_EXTENDED,
            shiftState = ShiftState.OFF,
            usePersianDigits = false,
            enterLabel = "ENTER_ICON"
        )
        val combined = (page1 + page2).flatMap { row -> row.map { it.output } }.toSet()
        val required = listOf("@", "#", "$", "%", "&", "*", "-", "+", "=", "/", ":", ";", "!", "?", "(", ")", "[", "]", "{", "}", "\"", "'", "«", "»", "،", "؛", "؟", "…")
        for (sym in required) {
            assertTrue("Expected symbol $sym in symbols layouts", combined.contains(sym))
        }
    }

    @Test
    fun testEnglishAndPersianSpellChecking() {
        val enCorrections = SpellChecker.findCorrections("helo", isPersian = false, learnedWords = emptyMap())
        assertTrue(enCorrections.any { it.word == "hello" })

        val enTranspose = SpellChecker.findCorrections("teh", isPersian = false, learnedWords = emptyMap())
        assertTrue(enTranspose.any { it.word == "the" })

        val faCorrections = SpellChecker.findCorrections("صلام", isPersian = true, learnedWords = emptyMap())
        assertTrue(faCorrections.any { it.word == "سلام" })

        val faLearnedBonus = SpellChecker.findCorrections("سلم", isPersian = true, learnedWords = mapOf("سلام" to 50))
        assertEquals("سلام", faLearnedBonus.firstOrNull()?.word)
    }

    @Test
    fun testPersianNormalizationAndWordExtraction() {
        assertEquals("کتاب", CoreLexicon.normalizePersian("كتاب"))
        assertEquals("سلام", SuggestionEngine.extractCurrentWord("وقت بخیر سلام"))
        val (p2, p1) = SuggestionEngine.extractPreviousTwoWords("سلام خوبی ")
        assertEquals("سلام", p2)
        assertEquals("خوبی", p1)
        assertFalse(SuggestionEngine.isWordChar(' '))
        assertTrue(SuggestionEngine.isWordChar('\u200C'))
    }
}
