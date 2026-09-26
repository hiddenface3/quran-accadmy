package com.example.data.model

enum class ClassStatus {
    SCHEDULED,
    LIVE_NOW,
    COMPLETED,
    CANCELLED
}

data class QuranClass(
    val id: String,
    val title: String,
    val teacherName: String,
    val teacherTitle: String = "Certified Qari & Hifz Instructor",
    val studentName: String,
    val date: String,
    val startTime: String,
    val durationMinutes: Int = 45,
    val status: ClassStatus = ClassStatus.SCHEDULED,
    val description: String = "",
    val liveKitRoomName: String = "",
    val surahTopic: String = "Surah Al-Mulk (Ayah 1-15)",
    val syllabusNotes: String = "Makharij Al-Huroof and Ghunnah rules recitation practice.",
    val isNextClass: Boolean = false
) {
    val canJoin: Boolean
        get() = status == ClassStatus.LIVE_NOW || status == ClassStatus.SCHEDULED
}
