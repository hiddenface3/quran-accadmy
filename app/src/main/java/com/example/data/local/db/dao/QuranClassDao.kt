package com.example.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.db.entity.QuranClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranClassDao {

    @Query("SELECT * FROM classes ORDER BY date ASC, startTime ASC")
    fun getAllClassesFlow(): Flow<List<QuranClassEntity>>

    @Query("SELECT * FROM classes ORDER BY date ASC, startTime ASC")
    suspend fun getAllClasses(): List<QuranClassEntity>

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    suspend fun getClassById(id: String): QuranClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<QuranClassEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(quranClass: QuranClassEntity)

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteClassById(id: String)

    @Query("DELETE FROM classes")
    suspend fun clearAllClasses()
}
