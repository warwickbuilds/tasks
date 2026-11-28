package com.warwickbuilds.tasks.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.warwickbuilds.tasks.core.StarterTags

/**
 * The one local database. Add a Migration (and bump [version]) for any schema change;
 * never use fallbackToDestructiveMigration, which would delete people's tasks.
 */
@Database(
    entities = [TaskEntity::class, TagEntity::class, TaskTagEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TasksDatabase : RoomDatabase() {
    abstract fun dao(): TaskDao

    companion object {
        const val NAME = "tasks.db"

        fun build(context: Context, inMemory: Boolean = false): TasksDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, TasksDatabase::class.java)
            } else {
                Room.databaseBuilder(context, TasksDatabase::class.java, NAME)
            }
            return builder.addCallback(StarterTagsCallback).build()
        }
    }
}

/** Adds the starter tags (Today, Movies to watch, Books to read, TV shows, Projects) to a new database. */
private object StarterTagsCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        StarterTags.all.forEach { tag ->
            db.execSQL(
                "INSERT OR IGNORE INTO tag (id, name, color, sortOrder) VALUES (?, ?, ?, ?)",
                arrayOf<Any>(tag.id, tag.name, tag.color, tag.sortOrder),
            )
        }
    }
}
