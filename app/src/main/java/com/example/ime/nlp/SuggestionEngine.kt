package com.example.ime.nlp

import com.example.data.DictionaryRepository
import com.example.data.KeyboardPreferences

data class SuggestionChip(
    val text: String,
    val isCorrection: Boolean = false,
    val isPhrase: Boolean = false
)

data class SuggestionResult(
    val chips: List<SuggestionChip>,
    val autoCorrectReplacement: String? = null
)

/**
 * Coordinates spell checking, auto-correction, next-word prediction, sentence/phrase
 * completions, and learned frequent-word ranking for CallistoKey.
 */
class SuggestionEngine(
    private val repository: DictionaryRepository,
    private val prefs: KeyboardPreferences
) {

    /**
     * Computes suggestions given the text before cursor and current keyboard language.
     * Always returns an empty result if disabled or inside sensitive/password fields.
     */
    fun getSuggestions(
        textBeforeCursor: String,
        isPersianMode: Boolean,
        isSensitiveField: Boolean
    ): SuggestionResult {
        if (isSensitiveField) {
            return SuggestionResult(emptyList(), null)
        }

        repository.ensureFreshCache()

        val spellEnabled = prefs.spellCheckEnabled
        val sentenceEnabled = prefs.sentenceSuggestionsEnabled
        if (!spellEnabled && !sentenceEnabled) {
            return SuggestionResult(emptyList(), null)
        }

        val lang = if (isPersianMode) KeyboardPreferences.LANG_FA else KeyboardPreferences.LANG_EN
        val learnedMap = repository.getLearnedWordMap(lang)

        // Check whether user is mid-word or right after whitespace/start
        val composingWord = extractCurrentWord(textBeforeCursor)
        val priorText = if (composingWord.isNotEmpty()) {
            textBeforeCursor.dropLast(composingWord.length)
        } else {
            textBeforeCursor
        }
        val (prev2, prev1) = extractPreviousTwoWords(priorText)

        // Case 1: User is currently typing a word
        if (composingWord.isNotEmpty()) {
            val chips = ArrayList<SuggestionChip>(4)
            val seen = HashSet<String>(8)
            var autoCorrectTarget: String? = null

            val isKnown = SpellChecker.isKnownWord(composingWord, isPersianMode, learnedMap)
            val completions = SpellChecker.findPrefixCompletions(composingWord, isPersianMode, learnedMap, 3)

            if (!isKnown && spellEnabled && composingWord.length >= 2) {
                val corrections = SpellChecker.findCorrections(composingWord, isPersianMode, learnedMap, 3)
                if (corrections.isNotEmpty()) {
                    val top = corrections.first()
                    if (top.distance <= 1.15f && prefs.autoCorrectEnabled) {
                        autoCorrectTarget = top.word
                    }
                    // Show user's literal word on left, and top correction highlighted
                    chips.add(SuggestionChip("\"$composingWord\"", isCorrection = false))
                    seen.add(composingWord.lowercase())

                    for (c in corrections) {
                        if (seen.add(c.word.lowercase()) && chips.size < 4) {
                            chips.add(SuggestionChip(c.word, isCorrection = (c == top)))
                        }
                    }
                }
            }

            if (chips.isEmpty()) {
                // Known word or single letter: put completions & spell variants
                if (isKnown && seen.add(composingWord.lowercase())) {
                    chips.add(SuggestionChip(composingWord, isCorrection = false))
                }
                for ((comp, _) in completions) {
                    if (seen.add(comp.lowercase()) && chips.size < 4) {
                        chips.add(SuggestionChip(comp, isCorrection = false))
                    }
                }
                if (chips.size < 3 && spellEnabled && composingWord.length >= 2) {
                    val corrections = SpellChecker.findCorrections(composingWord, isPersianMode, learnedMap, 2)
                    for (c in corrections) {
                        if (seen.add(c.word.lowercase()) && chips.size < 4) {
                            chips.add(SuggestionChip(c.word, isCorrection = !isKnown))
                        }
                    }
                }
            } else if (chips.size < 4) {
                for ((comp, _) in completions) {
                    if (seen.add(comp.lowercase()) && chips.size < 4) {
                        chips.add(SuggestionChip(comp, isCorrection = false))
                    }
                }
            }

            return SuggestionResult(chips.take(4), autoCorrectTarget)
        }

        // Case 2: At start of input or after space/punctuation -> Sentence & Next-Word Predictions
        if (!sentenceEnabled) {
            return SuggestionResult(emptyList(), null)
        }

        val chips = ArrayList<SuggestionChip>(4)
        val seen = HashSet<String>(8)

        // 2a. Learned user n-grams (highest personal priority)
        if (!prev1.isNullOrEmpty()) {
            val learnedNext = repository.getNextWordCandidates(prev2, prev1, lang, 3)
            for ((nextWord, _) in learnedNext) {
                if (seen.add(nextWord.lowercase()) && chips.size < 4) {
                    chips.add(SuggestionChip(nextWord, isPhrase = true))
                }
            }

            // 2b. Curated phrase / sentence completions
            val normalizedPrev1 = if (isPersianMode) {
                CoreLexicon.normalizePersian(prev1.lowercase())
            } else {
                prev1.lowercase()
            }
            val phraseMap = if (isPersianMode) CoreLexicon.persianPhrases else CoreLexicon.englishPhrases
            phraseMap[normalizedPrev1]?.forEach { phrase ->
                if (seen.add(phrase.lowercase()) && chips.size < 4) {
                    chips.add(SuggestionChip(phrase, isPhrase = true))
                }
            }
        }

        // 2c. Frequent learned words / sentence starters when field is empty or few candidates
        if (chips.size < 4) {
            val starters = if (isPersianMode) {
                CoreLexicon.persianSentenceStarters
            } else {
                CoreLexicon.englishSentenceStarters
            }
            if (prev1.isNullOrEmpty()) {
                // Empty field: top learned words first, then starters
                learnedMap.entries
                    .sortedByDescending { it.value }
                    .take(2)
                    .forEach { (w, _) ->
                        if (seen.add(w.lowercase()) && chips.size < 4) {
                            chips.add(SuggestionChip(w, isPhrase = true))
                        }
                    }
                for (s in starters) {
                    if (seen.add(s.lowercase()) && chips.size < 4) {
                        chips.add(SuggestionChip(s, isPhrase = true))
                    }
                }
            } else {
                // Fallback frequent next words
                val fallback = if (isPersianMode) {
                    listOf("است", "ممنون", "لطفاً", "خوب")
                } else {
                    listOf("the", "to", "and", "is")
                }
                for (f in fallback) {
                    if (seen.add(f.lowercase()) && chips.size < 4) {
                        chips.add(SuggestionChip(f, isPhrase = false))
                    }
                }
            }
        }

        return SuggestionResult(chips.take(4), null)
    }

    /**
     * Records committed words and phrase transitions if learning is active and input is not sensitive.
     */
    fun learnCommittedWord(
        textBeforeWord: String,
        committedWord: String,
        isPersianMode: Boolean,
        isSensitiveField: Boolean
    ) {
        if (isSensitiveField || !prefs.learningEnabled) return
        val clean = committedWord.trim()
        if (clean.length < 2) return
        val (prev2, prev1) = extractPreviousTwoWords(textBeforeWord)
        val lang = if (isPersianMode) KeyboardPreferences.LANG_FA else KeyboardPreferences.LANG_EN
        repository.recordUsage(prev2, prev1, clean, lang)
    }

    companion object {
        fun extractCurrentWord(textBeforeCursor: String): String {
            if (textBeforeCursor.isEmpty()) return ""
            var idx = textBeforeCursor.length - 1
            while (idx >= 0) {
                val c = textBeforeCursor[idx]
                if (!isWordChar(c)) break
                idx--
            }
            return textBeforeCursor.substring(idx + 1)
        }

        fun extractPreviousTwoWords(text: String): Pair<String?, String?> {
            if (text.isBlank()) return null to null
            val tokens = text.trim()
                .split(Regex("[\\s.,!?;:،؛؟«»()\\[\\]{}\"']+"))
                .filter { it.isNotBlank() }
            return when {
                tokens.size >= 2 -> tokens[tokens.size - 2] to tokens[tokens.size - 1]
                tokens.size == 1 -> null to tokens[0]
                else -> null to null
            }
        }

        fun isWordChar(c: Char): Boolean {
            return c.isLetter() || c == '\'' || c == '\u200C' || c == '‌'
        }
    }
}
