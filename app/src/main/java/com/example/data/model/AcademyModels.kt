package com.example.data.model

data class Message(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val receiverId: String,
    val text: String,
    val timestamp: String,
    val isRead: Boolean = true,
    val isFromMe: Boolean = false,
    val receiverName: String = ""
)

data class AttendanceRecord(
    val id: String,
    val className: String,
    val teacherName: String,
    val date: String,
    val durationMinutes: Int,
    val grade: String = "Mumtaz (Excellent)",
    val notes: String = "Excellent Tajweed pronunciation on Noon Sakinah rules."
)

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: String,
    val type: NotificationType = NotificationType.CLASS_REMINDER,
    val isRead: Boolean = false,
    val relatedClassId: String? = null
)

enum class NotificationType {
    CLASS_REMINDER,
    NEW_MESSAGE,
    SCHEDULE_CHANGE,
    ACADEMY_ANNOUNCEMENT
}

data class StudentInfo(
    val id: String,
    val name: String,
    val email: String,
    val assignedTeacherName: String,
    val tajweedLevel: String,
    val currentSurah: String,
    val attendanceRate: String = "98%",
    val phone: String = "+1 (555) 234-5678",
    val fcmToken: String = ""
)

data class TeacherInfo(
    val id: String,
    val name: String,
    val email: String,
    val title: String,
    val tajweedIjazah: String,
    val assignedStudentCount: Int,
    val availability: String = "Daily 4 PM - 9 PM",
    val phone: String = "+1 (555) 876-5432",
    val fcmToken: String = ""
)

data class QuranVerse(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val arabicText: String,
    val transliteration: String,
    val translation: String,
    val tajweedNote: String = ""
)
