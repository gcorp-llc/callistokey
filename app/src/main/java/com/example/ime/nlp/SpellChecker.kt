package com.example.ime.nlp

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Fast, zero-bloat Damerau-Levenshtein spell checker with physical keyboard proximity scoring
 * for both English (QWERTY) and Persian (Standard PC) layouts.
 */
object SpellChecker {

    private val enCoords: Map<Char, Pair<Float, Float>> = buildMap {
        val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        val offsets = listOf(0f, 0.5f, 1.5f)
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                put(ch, Pair(c + offsets[r], r.toFloat()))
            }
        }
    }

    private val faCoords: Map<Char, Pair<Float, Float>> = buildMap {
        val rows = listOf("ضصثقفغعهخحجچ", "شسیبلاتنمکگ", "ظطزرذدئو‌پژ")
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                put(ch, Pair(c.toFloat(), r.toFloat()))
            }
        }
    }

    data class SpellCandidate(
        val word: String,
        val distance: Float,
        val score: Double
    )

    fun isKnownWord(word: String, isPersian: Boolean, learnedWords: Map<String, Int>): Boolean {
        val normalized = if (isPersian) CoreLexicon.normalizePersian(word) else word.lowercase()
        if (learnedWords.containsKey(word) || learnedWords.containsKey(normalized)) return true
        val core = if (isPersian) CoreLexicon.persianWords else CoreLexicon.englishWords
        return core.containsKey(word) || core.containsKey(normalized)
    }

    fun findCorrections(
        input: String,
        isPersian: Boolean,
        learnedWords: Map<String, Int>,
        maxResults: Int = 4
    ): List<SpellCandidate> {
        if (input.length < 2 || input.length > 24) return emptyList()

        val query = if (isPersian) CoreLexicon.normalizePersian(input) else input.lowercase()
        val core = if (isPersian) CoreLexicon.persianWords else CoreLexicon.englishWords
        val coords = if (isPersian) faCoords else enCoords
        val maxDist = if (query.length <= 4) 1 else 2

        val candidates = ArrayList<SpellCandidate>(16)
        val seen = HashSet<String>(32)

        fun evaluate(word: String, freq: Int, learnedBonus: Double) {
            val target = if (isPersian) CoreLexicon.normalizePersian(word) else word.lowercase()
            if (target == query) return
            if (abs(target.length - query.length) > maxDist) return

            // Quick character filter for distance <= 1/2 when length >= 4
            val dist = weightedDamerauLevenshtein(query, target, coords, maxDist)
            if (dist <= maxDist + 0.35f) {
                if (seen.add(target)) {
                    val baseScore = (freq.toDouble() + 10.0) * learnedBonus
                    val prefixMatchBonus = if (target.firstOrNull() == query.firstOrNull()) 1.5 else 1.0
                    val finalScore = (baseScore * prefixMatchBonus) / ((dist + 0.5) * (dist + 0.5))
                    val casedWord = preserveInputCase(input, word, isPersian)
                    candidates.add(SpellCandidate(casedWord, dist, finalScore))
                }
            }
        }

        // Prioritize user's learned frequent words first
        for ((word, freq) in learnedWords) {
            evaluate(word, freq * 25, 2.5)
        }
        // Then evaluate core lexicon
        for ((word, freq) in core) {
            evaluate(word, freq, 1.0)
        }

        candidates.sortByDescending { it.score }
        return if (candidates.size > maxResults) candidates.subList(0, maxResults) else candidates
    }

    fun findPrefixCompletions(
        prefix: String,
        isPersian: Boolean,
        learnedWords: Map<String, Int>,
        maxResults: Int = 4
    ): List<Pair<String, Double>> {
        if (prefix.isEmpty()) return emptyList()

        val query = if (isPersian) CoreLexicon.normalizePersian(prefix) else prefix.lowercase()
        val core = if (isPersian) CoreLexicon.persianWords else CoreLexicon.englishWords
        val results = ArrayList<Pair<String, Double>>(12)
        val seen = HashSet<String>(24)

        for ((word, freq) in learnedWords) {
            val norm = if (isPersian) CoreLexicon.normalizePersian(word) else word.lowercase()
            if (norm.startsWith(query) && norm != query && seen.add(norm)) {
                results.add(preserveInputCase(prefix, word, isPersian) to (freq * 40.0 + 1000.0))
            }
        }

        for ((word, freq) in core) {
            val norm = if (isPersian) CoreLexicon.normalizePersian(word) else word.lowercase()
            if (norm.startsWith(query) && norm != query && seen.add(norm)) {
                results.add(preserveInputCase(prefix, word, isPersian) to freq.toDouble())
            }
        }

        results.sortByDescending { it.second }
        return if (results.size > maxResults) results.subList(0, maxResults) else results
    }

    private fun preserveInputCase(input: String, suggestion: String, isPersian: Boolean): String {
        if (isPersian || input.isEmpty() || suggestion.isEmpty()) return suggestion
        return when {
            input.length > 1 && input.all { it.isUpperCase() } -> suggestion.uppercase()
            input[0].isUpperCase() -> suggestion.replaceFirstChar { it.uppercaseChar() }
            else -> suggestion
        }
    }

    /**
     * Weighted Damerau-Levenshtein supporting adjacent transpositions (cost 0.85)
     * and physically adjacent key substitutions (cost 0.55) for smarter typo correction.
     */
    private fun weightedDamerauLevenshtein(
        a: String,
        b: String,
        coords: Map<Char, Pair<Float, Float>>,
        maxDist: Int
    ): Float {
        val m = a.length
        val n = b.length
        if (abs(m - n) > maxDist) return Float.MAX_VALUE

        val dp = Array(m + 1) { FloatArray(n + 1) }
        for (i in 0..m) dp[i][0] = i.toFloat()
        for (j in 0..n) dp[0][j] = j.toFloat()

        for (i in 1..m) {
            var rowMin = Float.MAX_VALUE
            val chA = a[i - 1]
            for (j in 1..n) {
                val chB = b[j - 1]
                val subCost = if (chA == chB) {
                    0f
                } else {
                    val pA = coords[chA]
                    val pB = coords[chB]
                    if (pA != null && pB != null) {
                        val keyDist = hypot((pA.first - pB.first).toDouble(), (pA.second - pB.second).toDouble()).toFloat()
                        if (keyDist <= 1.45f) 0.6f else 1.0f
                    } else {
                        1.0f
                    }
                }

                var best = min(
                    dp[i - 1][j] + 1.0f,
                    min(dp[i][j - 1] + 1.0f, dp[i - 1][j - 1] + subCost)
                )

                if (i > 1 && j > 1 && chA == b[j - 2] && a[i - 2] == chB) {
                    best = min(best, dp[i - 2][j - 2] + 0.8f)
                }

                dp[i][j] = best
                if (best < rowMin) rowMin = best
            }
            if (rowMin > maxDist + 0.5f) return Float.MAX_VALUE
        }

        return dp[m][n]
    }
}
