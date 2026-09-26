package com.example.data.model

enum class UserRole {
    STUDENT,
    TEACHER,
    ADMIN
}

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole = UserRole.STUDENT,
    val avatarUrl: String = "",
    val assignedTeacherName: String = "",
    val tajweedLevel: String = "Intermediate (Ahkam At-Tajweed)",
    val currentSurah: String = "Surah Al-Mulk (67)",
    val currentAyah: Int = 14,
    val completedJuzCount: Int = 3,
    val attendanceRate: String = "100%",
    val enrolledDate: String = "September 2026"
)
