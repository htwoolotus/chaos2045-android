package com.example.chaos2045.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SharedContentDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "shared_content.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "shared_content"
        const val COLUMN_ID = "id"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_TYPE = "type"
        const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CONTENT TEXT NOT NULL,
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertSharedContent(content: String, type: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CONTENT, content)
            put(COLUMN_TYPE, type)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun getAllSharedContent(): List<SharedContent> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        val contentList = mutableListOf<SharedContent>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
            val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
            val type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE))
            val timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            contentList.add(SharedContent(id, content, type, timestamp))
        }
        cursor.close()
        return contentList
    }

    fun deleteSharedContent(id: Long): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString())) > 0
    }

    fun getSharedContentById(id: Long): SharedContent? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
            val type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE))
            val timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
            SharedContent(id, content, type, timestamp)
        } else {
            null
        }.also { cursor.close() }
    }
}

data class SharedContent(
    val id: Long,
    val content: String,
    val type: String,
    val timestamp: String
) 