package com.jarvis.ai

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * On-device store for information the user explicitly asks JARVIS to remember
 * and tasks the user explicitly creates. No cloud sync or hidden collection.
 */
class LocalBrainStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "jarvis_personal_brain.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                content TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                done INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )""".trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Keep user data. Future schema migrations must be added explicitly.
    }

    @Synchronized
    fun addMemory(value: String): Long {
        val clean = value.trim().take(2000)
        require(clean.isNotBlank())
        return writableDatabase.insertOrThrow(
            "memories", null,
            ContentValues().apply {
                put("content", clean)
                put("created_at", System.currentTimeMillis())
            }
        )
    }

    @Synchronized
    fun searchMemories(query: String = ""): List<String> {
        val db = readableDatabase
        val rows = if (query.isBlank()) {
            db.rawQuery("SELECT content FROM memories ORDER BY id DESC LIMIT 30", null)
        } else {
            db.rawQuery(
                "SELECT content FROM memories WHERE content LIKE ? ORDER BY id DESC LIMIT 30",
                arrayOf("%${query.take(100)}%")
            )
        }
        return rows.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
    }

    @Synchronized
    fun deleteMemory(exactText: String): Boolean {
        return writableDatabase.delete("memories", "content = ?", arrayOf(exactText.trim())) > 0
    }

    @Synchronized
    fun addTask(title: String): Long {
        val clean = title.trim().take(500)
        require(clean.isNotBlank())
        return writableDatabase.insertOrThrow(
            "tasks", null,
            ContentValues().apply {
                put("title", clean)
                put("done", 0)
                put("created_at", System.currentTimeMillis())
            }
        )
    }

    @Synchronized
    fun pendingTasks(): List<String> {
        val rows = readableDatabase.rawQuery(
            "SELECT title FROM tasks WHERE done = 0 ORDER BY id DESC LIMIT 30", null
        )
        return rows.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
    }

    @Synchronized
    fun completeTask(exactTitle: String): Boolean {
        val values = ContentValues().apply { put("done", 1) }
        return writableDatabase.update(
            "tasks", values, "title = ? AND done = 0", arrayOf(exactTitle.trim())
        ) > 0
    }

    @Synchronized
    fun deleteTask(exactTitle: String): Boolean {
        return writableDatabase.delete(
            "tasks", "title = ?", arrayOf(exactTitle.trim())
        ) > 0
    }
}
