package com.notereader.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.notereader.app.data.db.dao.*
import com.notereader.app.data.db.entities.*

@Database(
    entities = [
        NotebookEntity::class,
        SectionEntity::class,
        PageEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        AnnotationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notebookDao(): NotebookDao
    abstract fun sectionDao(): SectionDao
    abstract fun pageDao(): PageDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun annotationDao(): AnnotationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notereader.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
