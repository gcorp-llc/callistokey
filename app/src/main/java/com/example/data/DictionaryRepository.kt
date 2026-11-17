package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

data class LearnedWordEntry(
    val word: String,
    val lang: String,
    val frequency: Int,
    val updatedAt: Long
)

data class LearnedPhraseEntry(
    val prefix: String,
    val nextWord: String,
    val lang: String,
    val frequency: Int,
    val updatedAt: Long
)

data class StorageStats(
    val learnedWordsCount: Int,
    val learnedPhrasesCount: Int,
    val databaseSizeBytes: Long
)

/**
 * Thread-safe, ultra-fast local repository backing CallistoKey's learned dictionary,
 * frequent word ranking, and n-gram phrase predictions.
 * All reads during typing hit in-memory maps for sub-millisecond latency.
 */
class DictionaryRepository private constructor(
    private val context: Context,
    private val prefs: KeyboardPreferences
) {
    private val dbHelper = CallistoDbHelper(context)
    private val ioExecutor = Executors.newSingleThreadExecutor()

    // In-memory fast lookup structures: (lang -> Map<word, freq>)
    private val wordFreqsEn = ConcurrentHashMap<String, Int>()
    private val wordFreqsFa = ConcurrentHashMap<String, Int>()

    // Bigrams: (lang -> Map<prevWord, MutableMap<nextWord, freq>>)
    private val bigramsEn = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()
    private val bigramsFa = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    // Trigrams: (lang -> Map<"w1 w2", MutableMap<nextWord, freq>>)
    private val trigramsEn = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()
    private val trigramsFa = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    @Volatile
    private var lastLoadedEpoch = -1L

    @Volatile
    var isLoaded = false
        private set

    init {
        preloadAsync()
    }

    fun preloadAsync() {
        ioExecutor.execute {
            reloadFromDatabase()
        }
    }

    fun ensureFreshCache() {
        val currentEpoch = prefs.dictionaryEpoch
        if (currentEpoch != lastLoadedEpoch && isLoaded) {
            preloadAsync()
        }
    }

    @Synchronized
    fun reloadFromDatabase() {
        wordFreqsEn.clear()
        wordFreqsFa.clear()
        bigramsEn.clear()
        bigramsFa.clear()
        trigramsEn.clear()
        trigramsFa.clear()

        val db = dbHelper.readableDatabase

        db.rawQuery(
            "SELECT word, lang, freq FROM ${CallistoDbHelper.TABLE_WORDS} ORDER BY freq DESC LIMIT 5000",
            null
        ).use { cursor ->
            val wordIdx = cursor.getColumnIndexOrThrow("word")
            val langIdx = cursor.getColumnIndexOrThrow("lang")
            val freqIdx = cursor.getColumnIndexOrThrow("freq")
            while (cursor.moveToNext()) {
                val word = cursor.getString(wordIdx)
                val lang = cursor.getString(langIdx)
                val freq = cursor.getInt(freqIdx)
                val map = if (lang == KeyboardPreferences.LANG_FA) wordFreqsFa else wordFreqsEn
                map[word] = freq
            }
        }

        db.rawQuery(
            "SELECT prev_word, next_word, lang, freq FROM ${CallistoDbHelper.TABLE_BIGRAMS} ORDER BY freq DESC LIMIT 5000",
            null
        ).use { cursor ->
            val prevIdx = cursor.getColumnIndexOrThrow("prev_word")
            val nextIdx = cursor.getColumnIndexOrThrow("next_word")
            val langIdx = cursor.getColumnIndexOrThrow("lang")
            val freqIdx = cursor.getColumnIndexOrThrow("freq")
            while (cursor.moveToNext()) {
                val prev = cursor.getString(prevIdx)
                val next = cursor.getString(nextIdx)
                val lang = cursor.getString(langIdx)
                val freq = cursor.getInt(freqIdx)
                val map = if (lang == KeyboardPreferences.LANG_FA) bigramsFa else bigramsEn
                map.getOrPut(prev) { ConcurrentHashMap() }[next] = freq
            }
        }

        db.rawQuery(
            "SELECT w1, w2, next_word, lang, freq FROM ${CallistoDbHelper.TABLE_TRIGRAMS} ORDER BY freq DESC LIMIT 4000",
            null
        ).use { cursor ->
            val w1Idx = cursor.getColumnIndexOrThrow("w1")
            val w2Idx = cursor.getColumnIndexOrThrow("w2")
            val nextIdx = cursor.getColumnIndexOrThrow("next_word")
            val langIdx = cursor.getColumnIndexOrThrow("lang")
            val freqIdx = cursor.getColumnIndexOrThrow("freq")
            while (cursor.moveToNext()) {
                val w1 = cursor.getString(w1Idx)
                val w2 = cursor.getString(w2Idx)
                val next = cursor.getString(nextIdx)
                val lang = cursor.getString(langIdx)
                val freq = cursor.getInt(freqIdx)
                val map = if (lang == KeyboardPreferences.LANG_FA) trigramsFa else trigramsEn
                map.getOrPut("$w1 $w2") { ConcurrentHashMap() }[next] = freq
            }
        }

        lastLoadedEpoch = prefs.dictionaryEpoch
        isLoaded = true
    }

    fun getWordFrequency(word: String, lang: String): Int {
        val map = if (lang == KeyboardPreferences.LANG_FA) wordFreqsFa else wordFreqsEn
        return map[word] ?: 0
    }

    fun getLearnedWordMap(lang: String): Map<String, Int> {
        return if (lang == KeyboardPreferences.LANG_FA) wordFreqsFa else wordFreqsEn
    }

    fun getNextWordCandidates(prev2: String?, prev1: String?, lang: String, limit: Int = 5): List<Pair<String, Int>> {
        val results = HashMap<String, Int>()
        val normalized1 = prev1?.trim()?.lowercase()
        val normalized2 = prev2?.trim()?.lowercase()

        if (!normalized2.isNullOrEmpty() && !normalized1.isNullOrEmpty()) {
            val trigramMap = if (lang == KeyboardPreferences.LANG_FA) trigramsFa else trigramsEn
            trigramMap["$normalized2 $normalized1"]?.forEach { (next, freq) ->
                results[next] = (results[next] ?: 0) + freq * 5 // Trigram boost
            }
        }

        if (!normalized1.isNullOrEmpty()) {
            val bigramMap = if (lang == KeyboardPreferences.LANG_FA) bigramsFa else bigramsEn
            bigramMap[normalized1]?.forEach { (next, freq) ->
                results[next] = (results[next] ?: 0) + freq * 2
            }
        }

        return results.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    fun recordUsage(prev2: String?, prev1: String?, word: String, lang: String) {
        val cleanWord = word.trim()
        if (cleanWord.length < 2 || cleanWord.length > 48) return
        if (cleanWord.all { it.isDigit() }) return

        val keyWord = if (lang == KeyboardPreferences.LANG_EN) cleanWord else cleanWord
        val map = if (lang == KeyboardPreferences.LANG_FA) wordFreqsFa else wordFreqsEn
        map[keyWord] = (map[keyWord] ?: 0) + 1

        val p1 = prev1?.trim()?.lowercase()?.takeIf { it.isNotEmpty() && it.length <= 48 }
        val p2 = prev2?.trim()?.lowercase()?.takeIf { it.isNotEmpty() && it.length <= 48 }

        if (p1 != null) {
            val bMap = if (lang == KeyboardPreferences.LANG_FA) bigramsFa else bigramsEn
            val nextMap = bMap.getOrPut(p1) { ConcurrentHashMap() }
            nextMap[cleanWord] = (nextMap[cleanWord] ?: 0) + 1

            if (p2 != null) {
                val tMap = if (lang == KeyboardPreferences.LANG_FA) trigramsFa else trigramsEn
                val tNextMap = tMap.getOrPut("$p2 $p1") { ConcurrentHashMap() }
                tNextMap[cleanWord] = (tNextMap[cleanWord] ?: 0) + 1
            }
        }

        val now = System.currentTimeMillis()
        ioExecutor.execute {
            try {
                val db = dbHelper.writableDatabase
                db.beginTransaction()
                try {
                    upsertWord(db, cleanWord, lang, now)
                    if (p1 != null) {
                        upsertBigram(db, p1, cleanWord, lang, now)
                        if (p2 != null) {
                            upsertTrigram(db, p2, p1, cleanWord, lang, now)
                        }
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } catch (_: Exception) {
                // Keep typing uninterrupted if storage fails
            }
        }
    }

    private fun upsertWord(db: SQLiteDatabase, word: String, lang: String, now: Long) {
        db.execSQL(
            """
            INSERT INTO ${CallistoDbHelper.TABLE_WORDS} (word, lang, freq, updated_at)
            VALUES (?, ?, 1, ?)
            ON CONFLICT(word, lang) DO UPDATE SET
                freq = freq + 1,
                updated_at = excluded.updated_at
            """.trimIndent(),
            arrayOf(word, lang, now)
        )
    }

    private fun upsertBigram(db: SQLiteDatabase, prev: String, next: String, lang: String, now: Long) {
        db.execSQL(
            """
            INSERT INTO ${CallistoDbHelper.TABLE_BIGRAMS} (prev_word, next_word, lang, freq, updated_at)
            VALUES (?, ?, ?, 1, ?)
            ON CONFLICT(prev_word, next_word, lang) DO UPDATE SET
                freq = freq + 1,
                updated_at = excluded.updated_at
            """.trimIndent(),
            arrayOf(prev, next, lang, now)
        )
    }

    private fun upsertTrigram(db: SQLiteDatabase, w1: String, w2: String, next: String, lang: String, now: Long) {
        db.execSQL(
            """
            INSERT INTO ${CallistoDbHelper.TABLE_TRIGRAMS} (w1, w2, next_word, lang, freq, updated_at)
            VALUES (?, ?, ?, ?, 1, ?)
            ON CONFLICT(w1, w2, next_word, lang) DO UPDATE SET
                freq = freq + 1,
                updated_at = excluded.updated_at
            """.trimIndent(),
            arrayOf(w1, w2, next, lang, now)
        )
    }

    fun getLearnedWords(langFilter: String? = null, query: String = ""): List<LearnedWordEntry> {
        val list = mutableListOf<LearnedWordEntry>()
        val db = dbHelper.readableDatabase
        val trimmedQuery = query.trim()

        val sql = StringBuilder("SELECT word, lang, freq, updated_at FROM ${CallistoDbHelper.TABLE_WORDS} WHERE 1=1")
        val args = mutableListOf<String>()

        if (!langFilter.isNullOrEmpty() && langFilter != "all") {
            sql.append(" AND lang = ?")
            args.add(langFilter)
        }
        if (trimmedQuery.isNotEmpty()) {
            sql.append(" AND word LIKE ?")
            args.add("%$trimmedQuery%")
        }
        sql.append(" ORDER BY freq DESC, updated_at DESC LIMIT 300")

        db.rawQuery(sql.toString(), args.toTypedArray()).use { cursor ->
            val wordIdx = cursor.getColumnIndexOrThrow("word")
            val langIdx = cursor.getColumnIndexOrThrow("lang")
            val freqIdx = cursor.getColumnIndexOrThrow("freq")
            val timeIdx = cursor.getColumnIndexOrThrow("updated_at")
            while (cursor.moveToNext()) {
                list.add(
                    LearnedWordEntry(
                        word = cursor.getString(wordIdx),
                        lang = cursor.getString(langIdx),
                        frequency = cursor.getInt(freqIdx),
                        updatedAt = cursor.getLong(timeIdx)
                    )
                )
            }
        }
        return list
    }

    fun getLearnedPhrases(langFilter: String? = null): List<LearnedPhraseEntry> {
        val list = mutableListOf<LearnedPhraseEntry>()
        val db = dbHelper.readableDatabase
        val sql = StringBuilder("SELECT prev_word, next_word, lang, freq, updated_at FROM ${CallistoDbHelper.TABLE_BIGRAMS} WHERE 1=1")
        val args = mutableListOf<String>()

        if (!langFilter.isNullOrEmpty() && langFilter != "all") {
            sql.append(" AND lang = ?")
            args.add(langFilter)
        }
        sql.append(" ORDER BY freq DESC, updated_at DESC LIMIT 200")

        db.rawQuery(sql.toString(), args.toTypedArray()).use { cursor ->
            val prevIdx = cursor.getColumnIndexOrThrow("prev_word")
            val nextIdx = cursor.getColumnIndexOrThrow("next_word")
            val langIdx = cursor.getColumnIndexOrThrow("lang")
            val freqIdx = cursor.getColumnIndexOrThrow("freq")
            val timeIdx = cursor.getColumnIndexOrThrow("updated_at")
            while (cursor.moveToNext()) {
                list.add(
                    LearnedPhraseEntry(
                        prefix = cursor.getString(prevIdx),
                        nextWord = cursor.getString(nextIdx),
                        lang = cursor.getString(langIdx),
                        frequency = cursor.getInt(freqIdx),
                        updatedAt = cursor.getLong(timeIdx)
                    )
                )
            }
        }
        return list
    }

    fun addCustomWord(word: String, lang: String) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        val map = if (lang == KeyboardPreferences.LANG_FA) wordFreqsFa else wordFreqsEn
        map[clean] = (map[clean] ?: 0) + 5

        val db = dbHelper.writableDatabase
        db.execSQL(
            """
            INSERT INTO ${CallistoDbHelper.TABLE_WORDS} (word, lang, freq, updated_at)
            VALUES (?, ?, 5, ?)
            ON CONFLICT(word, lang) DO UPDATE SET
                freq = freq + 5,
                updated_at = excluded.updated_at
            """.trimIndent(),
            arrayOf(clean, lang, System.currentTimeMillis())
        )
        prefs.bumpDictionaryEpoch()
    }

    fun deleteWord(word: String, lang: String) {
        val map = if (lang == KeyboardPreferences.LANG_FA) wordFreqsFa else wordFreqsEn
        map.remove(word)

        val db = dbHelper.writableDatabase
        db.delete(
            CallistoDbHelper.TABLE_WORDS,
            "word = ? AND lang = ?",
            arrayOf(word, lang)
        )
        prefs.bumpDictionaryEpoch()
    }

    fun deletePhrase(prefix: String, nextWord: String, lang: String) {
        val bMap = if (lang == KeyboardPreferences.LANG_FA) bigramsFa else bigramsEn
        bMap[prefix]?.remove(nextWord)

        val db = dbHelper.writableDatabase
        db.delete(
            CallistoDbHelper.TABLE_BIGRAMS,
            "prev_word = ? AND next_word = ? AND lang = ?",
            arrayOf(prefix, nextWord, lang)
        )
        prefs.bumpDictionaryEpoch()
    }

    /**
     * Deletes typing context / sentence history (bigrams and trigrams) while keeping saved frequent words.
     */
    fun clearTypingHistoryOnly() {
        bigramsEn.clear()
        bigramsFa.clear()
        trigramsEn.clear()
        trigramsFa.clear()

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(CallistoDbHelper.TABLE_BIGRAMS, null, null)
            db.delete(CallistoDbHelper.TABLE_TRIGRAMS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        prefs.bumpDictionaryEpoch()
    }

    /**
     * Wipes all learned frequent words, phrase patterns, and storage completely.
     */
    fun clearAllLearnedData() {
        wordFreqsEn.clear()
        wordFreqsFa.clear()
        bigramsEn.clear()
        bigramsFa.clear()
        trigramsEn.clear()
        trigramsFa.clear()

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(CallistoDbHelper.TABLE_WORDS, null, null)
            db.delete(CallistoDbHelper.TABLE_BIGRAMS, null, null)
            db.delete(CallistoDbHelper.TABLE_TRIGRAMS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        try {
            db.execSQL("VACUUM")
        } catch (_: Exception) {
            // Safe fallback
        }
        prefs.bumpDictionaryEpoch()
    }

    fun getStorageStats(): StorageStats {
        val db = dbHelper.readableDatabase
        var wordsCount = 0
        var phrasesCount = 0

        db.rawQuery("SELECT COUNT(*) FROM ${CallistoDbHelper.TABLE_WORDS}", null).use { c ->
            if (c.moveToFirst()) wordsCount = c.getInt(0)
        }
        db.rawQuery("SELECT COUNT(*) FROM ${CallistoDbHelper.TABLE_BIGRAMS}", null).use { c ->
            if (c.moveToFirst()) phrasesCount += c.getInt(0)
        }
        db.rawQuery("SELECT COUNT(*) FROM ${CallistoDbHelper.TABLE_TRIGRAMS}", null).use { c ->
            if (c.moveToFirst()) phrasesCount += c.getInt(0)
        }

        val dbFile: File = context.getDatabasePath(CallistoDbHelper.DATABASE_NAME)
        val sizeBytes = if (dbFile.exists()) dbFile.length() else 0L

        return StorageStats(
            learnedWordsCount = wordsCount,
            learnedPhrasesCount = phrasesCount,
            databaseSizeBytes = sizeBytes
        )
    }

    companion object {
        @Volatile
        private var instance: DictionaryRepository? = null

        fun getInstance(context: Context): DictionaryRepository {
            return instance ?: synchronized(this) {
                instance ?: DictionaryRepository(
                    context.applicationContext,
                    KeyboardPreferences(context.applicationContext)
                ).also { instance = it }
            }
        }
    }
}
