package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Lightweight, zero-overhead SQLite database for CallistoKey's local learned dictionary
 * and n-gram phrase predictions.
 */
class CallistoDbHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_WORDS (
                word TEXT NOT NULL,
                lang TEXT NOT NULL,
                freq INTEGER NOT NULL DEFAULT 1,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (word, lang)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_words_lang_freq ON $TABLE_WORDS (lang, freq DESC)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_BIGRAMS (
                prev_word TEXT NOT NULL,
                next_word TEXT NOT NULL,
                lang TEXT NOT NULL,
                freq INTEGER NOT NULL DEFAULT 1,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (prev_word, next_word, lang)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_bigrams_lookup ON $TABLE_BIGRAMS (lang, prev_word, freq DESC)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TRIGRAMS (
                w1 TEXT NOT NULL,
                w2 TEXT NOT NULL,
                next_word TEXT NOT NULL,
                lang TEXT NOT NULL,
                freq INTEGER NOT NULL DEFAULT 1,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (w1, w2, next_word, lang)
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_trigrams_lookup ON $TABLE_TRIGRAMS (lang, w1, w2, freq DESC)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WORDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BIGRAMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRIGRAMS")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "callistokey_lexicon.db"
        const val DATABASE_VERSION = 1

        const val TABLE_WORDS = "learned_words"
        const val TABLE_BIGRAMS = "learned_bigrams"
        const val TABLE_TRIGRAMS = "learned_trigrams"
    }
}
