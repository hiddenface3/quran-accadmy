package com.example.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ClassStatus
import com.example.data.model.QuranClass

@Entity(tableName = "classes")
data class QuranClassEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val teacherName: String,
    val teacherTitle: String,
    val studentName: String,
    val date: String,
    val startTime: String,
    val durationMinutes: Int,
    val status: String,
    val description: String,
    val liveKitRoomName: String,
    val surahTopic: String,
    val syllabusNotes: String,
    val isNextClass: Boolean
) {
    fun toDomain(): QuranClass {
        val classStatus = try {
            ClassStatus.valueOf(status)
        } catch (_: Exception) {
            ClassStatus.SCHEDULED
        }
        return QuranClass(
            id = id,
            title = title,
            teacherName = teacherName,
            teacherTitle = teacherTitle,
            studentName = studentName,
            date = date,
            startTime = startTime,
            durationMinutes = durationMinutes,
            status = classStatus,
            description = description,
            liveKitRoomName = liveKitRoomName,
            surahTopic = surahTopic,
            syllabusNotes = syllabusNotes,
            isNextClass = isNextClass
        )
    }

    companion object {
        fun fromDomain(c: QuranClass): QuranClassEntity {
            return QuranClassEntity(
                id = c.id,
                title = c.title,
                teacherName = c.teacherName,
                teacherTitle = c.teacherTitle,
                studentName = c.studentName,
                date = c.date,
                startTime = c.startTime,
                durationMinutes = c.durationMinutes,
                status = c.status.name,
                description = c.description,
                liveKitRoomName = c.liveKitRoomName,
                surahTopic = c.surahTopic,
                syllabusNotes = c.syllabusNotes,
                isNextClass = c.isNextClass
            )
        }
    }
}
