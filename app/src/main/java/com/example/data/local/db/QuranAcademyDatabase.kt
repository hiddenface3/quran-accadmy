package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.db.dao.MessageDao
import com.example.data.local.db.dao.QuranClassDao
import com.example.data.local.db.entity.MessageEntity
import com.example.data.local.db.entity.QuranClassEntity

@Database(
    entities = [QuranClassEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QuranAcademyDatabase : RoomDatabase() {

    abstract fun quranClassDao(): QuranClassDao
    abstract fun messageDao(): MessageDao

    companion object {
        private const val DATABASE_NAME = "quran_academy_db"

        @Volatile
        private var instance: QuranAcademyDatabase? = null

        fun getInstance(context: Context): QuranAcademyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    QuranAcademyDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
