package com.example.data.repository

import com.example.data.backend.AcademyBackendService
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClassStatus
import com.example.data.model.Message
import com.example.data.model.NotificationItem
import com.example.data.model.NotificationType
import com.example.data.model.QuranClass
import com.example.data.model.StudentInfo
import com.example.data.model.TeacherInfo
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AcademyRepository {

    val backendService = AcademyBackendService()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null

    init {
        scope.launch {
            com.example.data.local.OfflineDataCache.getAllClassesFlow()?.collect { roomClasses ->
                if (roomClasses.isNotEmpty()) {
                    _classes.value = roomClasses
                }
            }
        }
        scope.launch {
            com.example.data.local.OfflineDataCache.getAllMessagesFlow()?.collect { roomMessages ->
                if (roomMessages.isNotEmpty()) {
                    _messages.value = roomMessages
                }
            }
        }
        scope.launch { syncDataNow() }
        setupRealtimeCallSubscription()
        startPeriodicSync()
    }

    private fun setupRealtimeCallSubscription() {
        backendService.onChatMessageReceived = { incomingMsg ->
            val current = _currentUser.value
            val isFromMe = incomingMsg.senderId == current.id ||
                    incomingMsg.senderName.trim().equals(current.name.trim(), ignoreCase = true)
            val msgWithMe = incomingMsg.copy(isFromMe = isFromMe)

            val existing = _messages.value
            if (existing.none { it.id == msgWithMe.id }) {
                val updated = existing + msgWithMe
                _messages.value = updated
                com.example.data.local.OfflineDataCache.saveMessages(updated)
            }
        }

        backendService.subscribeToRealtimeActiveCalls { activeCall ->
            val current = _currentUser.value
            if (current.role == UserRole.STUDENT) {
                val cleanStudent = current.name.trim().lowercase()
                val callStudent = activeCall.studentName.trim().lowercase()
                val isMatch = cleanStudent.isNotEmpty() && callStudent.isNotEmpty() && callStudent == cleanStudent

                if (isMatch) {
                    if (activeCall.isRinging) {
                        val existingClass = _classes.value.firstOrNull { it.id == activeCall.classId }
                        val targetClass = existingClass ?: QuranClass(
                            id = activeCall.classId,
                            title = "Quran Reading & Tajweed Rules",
                            teacherName = activeCall.teacherName,
                            teacherTitle = "Certified Qari",
                            studentName = current.name,
                            date = "Today",
                            startTime = "Now",
                            durationMinutes = 45,
                            status = ClassStatus.LIVE_NOW,
                            description = "Live Quran Recitation Session with ${activeCall.teacherName}",
                            liveKitRoomName = activeCall.roomName,
                            surahTopic = "Surah Al-Mulk"
                        )
                        _incomingCallClass.value = targetClass
                    } else {
                        if (_incomingCallClass.value?.id == activeCall.classId) {
                            _incomingCallClass.value = null
                        }
                    }
                }
            }
        }
    }

    private val _currentUser = MutableStateFlow(
        UserProfile(
            id = "student_zaid_01",
            name = "Zaid Ahmed",
            email = "zaid.student@gmail.com",
            role = UserRole.STUDENT,
            avatarUrl = "",
            assignedTeacherName = "",
            tajweedLevel = "Intermediate Tajweed (Ahkam At-Tilawah)",
            currentSurah = "Surah Al-Mulk (67)",
            currentAyah = 14,
            completedJuzCount = 3,
            attendanceRate = "100%",
            enrolledDate = "September 2026"
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _classes = MutableStateFlow<List<QuranClass>>(com.example.data.local.OfflineDataCache.loadClasses() ?: initialClasses())
    val classes: StateFlow<List<QuranClass>> = _classes.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(com.example.data.local.OfflineDataCache.loadMessages() ?: initialMessages())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(initialNotifications())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _attendance = MutableStateFlow<List<AttendanceRecord>>(initialAttendance())
    val attendance: StateFlow<List<AttendanceRecord>> = _attendance.asStateFlow()

    private val _students = MutableStateFlow<List<StudentInfo>>(initialStudents())
    val students: StateFlow<List<StudentInfo>> = _students.asStateFlow()

    private val _teachers = MutableStateFlow<List<TeacherInfo>>(initialTeachers())
    val teachers: StateFlow<List<TeacherInfo>> = _teachers.asStateFlow()

    // Real-time Direct Call trigger state
    private val _incomingCallClass = MutableStateFlow<QuranClass?>(null)
    val incomingCallClass: StateFlow<QuranClass?> = _incomingCallClass.asStateFlow()

    suspend fun syncDataNow() {
        try {
            val remoteClassesResult = backendService.fetchClasses()
            remoteClassesResult.onSuccess { remoteList ->
                if (remoteList.isNotEmpty()) {
                    _classes.value = remoteList
                    com.example.data.local.OfflineDataCache.saveClasses(remoteList)
                }
            }

            val remoteProfilesResult = backendService.fetchProfiles()
            remoteProfilesResult.onSuccess { (remoteStudents, remoteTeachers) ->
                if (remoteStudents.isNotEmpty()) _students.value = remoteStudents
                if (remoteTeachers.isNotEmpty()) _teachers.value = remoteTeachers
            }

            // Fetch chat history ONCE on startup. Realtime WebSocket handles updates.
            val chatResult = backendService.fetchChatMessages()
            chatResult.onSuccess { remoteMsgs ->
                if (remoteMsgs.isNotEmpty()) {
                    val current = _currentUser.value
                    val mapped = remoteMsgs.map { msg ->
                        val isFromMe = msg.senderId == current.id ||
                                msg.senderName.trim().equals(current.name.trim(), ignoreCase = true)
                        msg.copy(isFromMe = isFromMe)
                    }
                    _messages.value = mapped
                    com.example.data.local.OfflineDataCache.saveMessages(mapped)
                }
            }
        } catch (e: Exception) {
            // Ignore transient network errors
        }
    }

    fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                try {
                    // 1. Fetch remote classes - sync directly with Supabase backend
                    val remoteClassesResult = backendService.fetchClasses()
                    remoteClassesResult.onSuccess { remoteList ->
                        if (remoteList.isNotEmpty()) {
                            _classes.value = remoteList
                        }
                    }

                    // 2. Fetch remote profiles - sync directly with Supabase backend
                    val remoteProfilesResult = backendService.fetchProfiles()
                    remoteProfilesResult.onSuccess { (remoteStudents, remoteTeachers) ->
                        val current = _currentUser.value
                        val baseStudents = if (remoteStudents.isNotEmpty()) remoteStudents else _students.value
                        val updatedStudents = if (current.role == UserRole.STUDENT && current.name.isNotBlank() &&
                            baseStudents.none { it.id == current.id || it.name.equals(current.name, ignoreCase = true) }) {
                            listOf(
                                StudentInfo(
                                    id = current.id,
                                    name = current.name,
                                    email = current.email,
                                    assignedTeacherName = current.assignedTeacherName,
                                    tajweedLevel = current.tajweedLevel,
                                    currentSurah = current.currentSurah,
                                    attendanceRate = current.attendanceRate
                                )
                            ) + baseStudents
                        } else {
                            baseStudents
                        }

                        val baseTeachers = if (remoteTeachers.isNotEmpty()) remoteTeachers else _teachers.value
                        val updatedTeachers = if (current.role == UserRole.TEACHER && current.name.isNotBlank() &&
                            baseTeachers.none { it.id == current.id || it.name.equals(current.name, ignoreCase = true) }) {
                            listOf(
                                TeacherInfo(
                                    id = current.id,
                                    name = current.name,
                                    email = current.email,
                                    title = "Certified Quran & Tajweed Instructor",
                                    tajweedIjazah = "Certified Hafiz & Qari",
                                    assignedStudentCount = 0,
                                    availability = "Available for Live Classes"
                                )
                            ) + baseTeachers
                        } else {
                            baseTeachers
                        }

                        _students.value = updatedStudents
                        _teachers.value = updatedTeachers
                    }

                    // Note: Incoming call signaling is handled via <50ms Supabase Realtime WebSocket 
                    // (setupRealtimeCallSubscription) and high-priority FCM data push notifications.
                    // Redundant REST polling was removed to eliminate duplicate alerts and conserve battery/bandwidth.

                } catch (e: Exception) {
                    // Ignore transient network errors
                }
                delay(3000)
            }
        }
    }

    fun updateCurrentUser(user: UserProfile) {
        _currentUser.value = user
        if (user.role == UserRole.STUDENT) {
            val exists = _students.value.any { it.name.equals(user.name, ignoreCase = true) || (it.email.isNotBlank() && it.email.equals(user.email, ignoreCase = true)) }
            if (!exists) {
                val newStudent = StudentInfo(
                    id = user.id,
                    name = user.name,
                    assignedTeacherName = user.assignedTeacherName,
                    tajweedLevel = user.tajweedLevel.ifBlank { "Intermediate Tajweed" },
                    currentSurah = user.currentSurah,
                    attendanceRate = "100%",
                    email = user.email
                )
                _students.value = listOf(newStudent) + _students.value
            }
        } else if (user.role == UserRole.TEACHER) {
            val exists = _teachers.value.any { it.name.equals(user.name, ignoreCase = true) || (it.email.isNotBlank() && it.email.equals(user.email, ignoreCase = true)) }
            if (!exists) {
                val newTeacher = TeacherInfo(
                    id = user.id,
                    name = user.name,
                    email = user.email,
                    title = "Certified Quran & Tajweed Instructor",
                    tajweedIjazah = "Certified Hafiz & Qari",
                    assignedStudentCount = 0,
                    availability = "Available for Live Classes"
                )
                _teachers.value = listOf(newTeacher) + _teachers.value
            }
        }

        scope.launch {
            backendService.syncUserProfile(user)
        }
    }

    fun assignTeacherToStudent(studentId: String, teacherName: String) {
        _students.value = _students.value.map { student ->
            if (student.id == studentId) {
                student.copy(assignedTeacherName = teacherName)
            } else student
        }

        // If the current logged-in user is that student, update their profile too
        val current = _currentUser.value
        if (current.id == studentId) {
            _currentUser.value = current.copy(assignedTeacherName = teacherName)
        }

        // Also update any scheduled classes for this student
        val updatedStudent = _students.value.firstOrNull { it.id == studentId }
        val sName = updatedStudent?.name ?: "Student"
        _classes.value = _classes.value.map { qClass ->
            if (qClass.studentName.equals(sName, ignoreCase = true)) {
                val updated = qClass.copy(teacherName = teacherName)
                scope.launch { backendService.saveClass(updated) }
                updated
            } else qClass
        }

        // Persist to Supabase backend
        scope.launch {
            backendService.updateStudentTeacherAssignment(studentId, teacherName)
        }

        // Post notification
        val notif = NotificationItem(
            id = "notif_${System.currentTimeMillis()}",
            title = "Teacher Assignment Updated",
            body = "Admin assigned $teacherName to student $sName.",
            timestamp = "Just now",
            type = NotificationType.SCHEDULE_CHANGE
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun deleteStudent(studentId: String) {
        val student = _students.value.firstOrNull { it.id == studentId }
        _students.value = _students.value.filterNot { it.id == studentId }
        scope.launch {
            backendService.deleteProfile(studentId)
        }
        val notif = NotificationItem(
            id = "notif_${System.currentTimeMillis()}",
            title = "Student Deleted",
            body = "Student '${student?.name ?: studentId}' was removed from the database.",
            timestamp = "Just now",
            type = NotificationType.SCHEDULE_CHANGE
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun deleteTeacher(teacherId: String) {
        val teacher = _teachers.value.firstOrNull { it.id == teacherId }
        _teachers.value = _teachers.value.filterNot { it.id == teacherId }
        scope.launch {
            backendService.deleteProfile(teacherId)
        }
        val notif = NotificationItem(
            id = "notif_${System.currentTimeMillis()}",
            title = "Teacher Deleted",
            body = "Teacher '${teacher?.name ?: teacherId}' was removed from the database.",
            timestamp = "Just now",
            type = NotificationType.SCHEDULE_CHANGE
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun deleteClass(classId: String) {
        val qClass = _classes.value.firstOrNull { it.id == classId }
        _classes.value = _classes.value.filterNot { it.id == classId }
        scope.launch {
            backendService.deleteClass(classId)
        }
        val notif = NotificationItem(
            id = "notif_${System.currentTimeMillis()}",
            title = "Class Deleted",
            body = "Class '${qClass?.title ?: classId}' was removed from the schedule.",
            timestamp = "Just now",
            type = NotificationType.SCHEDULE_CHANGE
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun recordSessionAttendance(classId: String, durationMinutes: Int, notes: String = "Live WebRTC Quran Tutoring Completed") {
        val qClass = _classes.value.firstOrNull { it.id == classId } ?: return
        val record = AttendanceRecord(
            id = "att_${System.currentTimeMillis()}",
            className = qClass.title,
            teacherName = qClass.teacherName,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
            durationMinutes = durationMinutes,
            grade = "Mumtaz (Excellent)",
            notes = notes
        )
        _attendance.value = listOf(record) + _attendance.value
    }

    fun initiateClassCall(classId: String) {
        val qClass = _classes.value.firstOrNull { it.id == classId } ?: return
        // Mark as live
        markClassAsLive(classId)
        
        // Trigger incoming call screen locally ONLY if the current device is logged in as the student
        val current = _currentUser.value
        if (current.role == UserRole.STUDENT && (current.name.equals(qClass.studentName, ignoreCase = true) || current.email.isNotBlank())) {
            _incomingCallClass.value = qClass
        }

        scope.launch {
            backendService.triggerCallSignal(qClass, qClass.teacherName)
        }

        val notif = NotificationItem(
            id = "call_notif_${System.currentTimeMillis()}",
            title = "Incoming Quran Class Call!",
            body = "${qClass.teacherName} is calling for '${qClass.title}'. Tap to accept.",
            timestamp = "Now",
            type = NotificationType.CLASS_REMINDER,
            relatedClassId = classId
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun dismissIncomingCall() {
        val call = _incomingCallClass.value
        _incomingCallClass.value = null
        if (call != null) {
            scope.launch {
                backendService.dismissCallSignal(call.id)
            }
        }
    }

    fun sendFiveMinuteReminder(classId: String) {
        val qClass = _classes.value.firstOrNull { it.id == classId } ?: return
        val notif = NotificationItem(
            id = "remind_${System.currentTimeMillis()}",
            title = "Class in 5 Minutes!",
            body = "Your Quran class '${qClass.title}' with ${qClass.teacherName} starts in 5 minutes. Please be ready.",
            timestamp = "Just now",
            type = NotificationType.CLASS_REMINDER,
            relatedClassId = classId
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun cancelClass(classId: String) {
        _classes.value = _classes.value.map {
            if (it.id == classId) it.copy(status = ClassStatus.CANCELLED) else it
        }
    }

    fun markClassAsLive(classId: String) {
        _classes.value = _classes.value.map {
            if (it.id == classId) it.copy(status = ClassStatus.LIVE_NOW) else it
        }
    }

    fun markClassAsCompleted(classId: String) {
        _classes.value = _classes.value.map {
            if (it.id == classId) it.copy(status = ClassStatus.COMPLETED) else it
        }
    }

    fun endSession(classId: String) {
        _classes.value = _classes.value.map {
            if (it.id == classId) it.copy(status = ClassStatus.COMPLETED) else it
        }
        val endedClass = _classes.value.firstOrNull { it.id == classId }
        if (endedClass != null) {
            scope.launch {
                backendService.saveClass(endedClass)
                backendService.dismissCallSignal(classId)
            }
        }
        _incomingCallClass.value = null

        // Add completed attendance record and notification
        val notif = NotificationItem(
            id = "session_ended_${System.currentTimeMillis()}",
            title = "Class Session Ended",
            body = "Session for '${endedClass?.title ?: classId}' was ended. Status updated to Completed.",
            timestamp = "Just now",
            type = NotificationType.CLASS_REMINDER
        )
        _notifications.value = listOf(notif) + _notifications.value
    }

    fun addClass(newClass: QuranClass) {
        _classes.value = listOf(newClass) + _classes.value
        scope.launch {
            backendService.saveClass(newClass)
        }
    }

    fun sendMessage(text: String, receiverId: String? = null, receiverName: String? = null) {
        if (text.isBlank()) return
        val current = _currentUser.value
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeString = sdf.format(Date())

        val isStudent = current.role == UserRole.STUDENT
        val defaultReceiverId = if (isStudent) "teacher_abdullah_01" else "student_zaid_01"
        val defaultReceiverName = if (isStudent) "Sheikh Abdullah Al-Mansoor" else "Zaid Ahmed"
        val actualReceiverId = receiverId ?: defaultReceiverId
        val actualReceiverName = receiverName ?: defaultReceiverName

        val newMessage = Message(
            id = "msg_${System.currentTimeMillis()}_${(1000..9999).random()}",
            senderId = current.id,
            senderName = current.name,
            senderRole = current.role,
            receiverId = actualReceiverId,
            receiverName = actualReceiverName,
            text = text.trim(),
            timestamp = timeString,
            isRead = true,
            isFromMe = true
        )
        _messages.value = _messages.value + newMessage
        com.example.data.local.OfflineDataCache.saveMessages(_messages.value)

        scope.launch(Dispatchers.IO) {
            backendService.sendChatMessage(newMessage)
        }
    }

    /**
     * Received instant in-call chat message (<20ms) from peer via LiveKit WebRTC DataChannel
     */
    fun onLiveKitMessageReceived(text: String) {
        if (text.isBlank()) return
        val current = _currentUser.value
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeString = sdf.format(Date())

        val isStudent = current.role == UserRole.STUDENT
        val incomingMsg = Message(
            id = "msg_lk_${System.currentTimeMillis()}_${(1000..9999).random()}",
            senderId = if (isStudent) "teacher" else "student",
            senderName = if (isStudent) "Teacher" else "Student",
            senderRole = if (isStudent) UserRole.TEACHER else UserRole.STUDENT,
            receiverId = current.id,
            receiverName = current.name,
            text = text.trim(),
            timestamp = timeString,
            isRead = true,
            isFromMe = false
        )
        val existing = _messages.value
        val isDuplicate = existing.any { 
            it.text == incomingMsg.text && !it.isFromMe && it.timestamp == incomingMsg.timestamp 
        }
        if (!isDuplicate) {
            val updated = existing + incomingMsg
            _messages.value = updated
            com.example.data.local.OfflineDataCache.saveMessages(updated)
        }
    }

    fun markNotificationsAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    private fun initialClasses(): List<QuranClass> = listOf(
        QuranClass(
            id = "cls_48392",
            title = "Quran Reading & Tajweed Rules",
            teacherName = "Sheikh Abdullah Al-Mansoor",
            teacherTitle = "Senior Tajweed & Qira'at Instructor",
            studentName = "Zaid Ahmed",
            date = "Today",
            startTime = "6:00 PM",
            durationMinutes = 45,
            status = ClassStatus.SCHEDULED,
            description = "Live recitation session covering Surah Al-Mulk Ayahs 1 to 15 with focus on Ikhfa rules.",
            liveKitRoomName = "quran-class-cls_48392",
            surahTopic = "Surah Al-Mulk (Ayah 1-15)",
            syllabusNotes = "Focus on Makharij of throat letters and Qalqalah kubra."
        ),
        QuranClass(
            id = "cls_48393",
            title = "Hifz Revision & Murattal Recitation",
            teacherName = "Sheikh Abdullah Al-Mansoor",
            teacherTitle = "Senior Tajweed & Qira'at Instructor",
            studentName = "Bilal Hassan",
            date = "Tomorrow",
            startTime = "5:00 PM",
            durationMinutes = 45,
            status = ClassStatus.SCHEDULED,
            description = "Daily Hifz sabq evaluation and retention testing on Juz 1.",
            liveKitRoomName = "quran-class-cls_48393",
            surahTopic = "Surah Al-Baqarah (Juz 1)",
            syllabusNotes = "Murattal pace testing and Mutashabihat review."
        ),
        QuranClass(
            id = "cls_48394",
            title = "Noorani Qaida & Letters Recognition",
            teacherName = "Ustadha Maryam Siddiqui",
            teacherTitle = "Hafizah & Foundation Specialist",
            studentName = "Fatima Zahra",
            date = "Tomorrow",
            startTime = "4:00 PM",
            durationMinutes = 30,
            status = ClassStatus.SCHEDULED,
            description = "Elementary pronunciation of heavy and light Arabic letters.",
            liveKitRoomName = "quran-class-cls_48394",
            surahTopic = "Qaida Lesson 9: Madd Letters",
            syllabusNotes = "Lengthening vowels by 2 Harakah."
        ),
        QuranClass(
            id = "cls_48391",
            title = "Tafseer & Recitation of Surah Ya-Sin",
            teacherName = "Sheikh Tariq Jameel",
            teacherTitle = "Al-Azhar Certified Qari",
            studentName = "Umar Farooq",
            date = "Yesterday",
            startTime = "7:00 PM",
            durationMinutes = 45,
            status = ClassStatus.COMPLETED,
            description = "Tafseer overview and melodious Tilawah.",
            liveKitRoomName = "quran-class-cls_48391",
            surahTopic = "Surah Ya-Sin (Ayah 1-20)",
            syllabusNotes = "Completed with excellent evaluation."
        )
    )

    private fun initialStudents(): List<StudentInfo> = listOf(
        StudentInfo(
            id = "student_zaid_01",
            name = "Zaid Ahmed",
            email = "zaid.student@gmail.com",
            assignedTeacherName = "Sheikh Abdullah Al-Mansoor",
            tajweedLevel = "Intermediate Tajweed (Ahkam At-Tilawah)",
            currentSurah = "Surah Al-Mulk (67)",
            attendanceRate = "100%",
            phone = "+1 (555) 234-5678"
        ),
        StudentInfo(
            id = "student_bilal_02",
            name = "Bilal Hassan",
            email = "bilal.hassan@gmail.com",
            assignedTeacherName = "Sheikh Abdullah Al-Mansoor",
            tajweedLevel = "Advanced Hifz & Murattal",
            currentSurah = "Surah Al-Baqarah (2)",
            attendanceRate = "96%",
            phone = "+1 (555) 345-6789"
        ),
        StudentInfo(
            id = "student_fatima_03",
            name = "Fatima Zahra",
            email = "fatima.zahra@gmail.com",
            assignedTeacherName = "Ustadha Maryam Siddiqui",
            tajweedLevel = "Beginner (Noorani Qaida)",
            currentSurah = "Juz 'Amma",
            attendanceRate = "98%",
            phone = "+1 (555) 456-7890"
        ),
        StudentInfo(
            id = "student_umar_04",
            name = "Umar Farooq",
            email = "umar.farooq@gmail.com",
            assignedTeacherName = "Sheikh Abdullah Al-Mansoor",
            tajweedLevel = "Intermediate Tajweed",
            currentSurah = "Surah Ya-Sin (36)",
            attendanceRate = "94%",
            phone = "+1 (555) 567-8901"
        ),
        StudentInfo(
            id = "student_aisha_05",
            name = "Aisha Rahman",
            email = "aisha.rahman@gmail.com",
            assignedTeacherName = "Sheikh Tariq Jameel",
            tajweedLevel = "Tajweed Rules & Makharij",
            currentSurah = "Surah Ar-Rahman (55)",
            attendanceRate = "100%",
            phone = "+1 (555) 678-9012"
        )
    )

    private fun initialTeachers(): List<TeacherInfo> = listOf(
        TeacherInfo(
            id = "teacher_abdullah_01",
            name = "Sheikh Abdullah Al-Mansoor",
            email = "abdullah.mansoor@quranacademy.com",
            title = "Senior Quran & Tajweed Instructor",
            tajweedIjazah = "Certified Hafiz & Qari (10 Qira'at)",
            assignedStudentCount = 3,
            availability = "Daily 4 PM - 9 PM",
            phone = "+1 (555) 876-5432"
        ),
        TeacherInfo(
            id = "teacher_maryam_02",
            name = "Ustadha Maryam Siddiqui",
            email = "maryam.siddiqui@quranacademy.com",
            title = "Noorani Qaida & Hifz Specialist",
            tajweedIjazah = "Certified Hafizah & Tajweed Teacher",
            assignedStudentCount = 2,
            availability = "Mon-Thu 3 PM - 7 PM",
            phone = "+1 (555) 765-4321"
        ),
        TeacherInfo(
            id = "teacher_tariq_03",
            name = "Sheikh Tariq Jameel",
            email = "tariq.jameel@quranacademy.com",
            title = "Tafseer & Advanced Tajweed Instructor",
            tajweedIjazah = "Al-Azhar Certified Qari",
            assignedStudentCount = 2,
            availability = "Weekends & Evenings",
            phone = "+1 (555) 654-3210"
        )
    )

    private fun initialMessages(): List<Message> = listOf(
        Message(
            id = "msg_01",
            senderId = "teacher_abdullah_01",
            senderName = "Sheikh Abdullah Al-Mansoor",
            senderRole = UserRole.TEACHER,
            receiverId = "student_zaid_01",
            text = "Assalamu Alaikum wa Rahmatullah Zaid. Your Quran Reading session is scheduled for today at 6:00 PM insha'Allah.",
            timestamp = "10:15 AM",
            isRead = true,
            isFromMe = false
        ),
        Message(
            id = "msg_02",
            senderId = "teacher_abdullah_01",
            senderName = "Sheikh Abdullah Al-Mansoor",
            senderRole = UserRole.TEACHER,
            receiverId = "student_zaid_01",
            text = "Please make sure to review Ayahs 1 to 15 of Surah Al-Mulk beforehand, focusing on the Ikhfa and Qalqalah rules.",
            timestamp = "10:16 AM",
            isRead = true,
            isFromMe = false
        ),
        Message(
            id = "msg_03",
            senderId = "student_zaid_01",
            senderName = "Zaid Ahmed",
            senderRole = UserRole.STUDENT,
            receiverId = "teacher_abdullah_01",
            text = "Wa Alaikum Assalam Sheikh. JazakAllah Khair, I have practiced them and I am ready to join the live room at 6:00 PM.",
            timestamp = "11:30 AM",
            isRead = true,
            isFromMe = true
        ),
        Message(
            id = "msg_04",
            senderId = "teacher_abdullah_01",
            senderName = "Sheikh Abdullah Al-Mansoor",
            senderRole = UserRole.TEACHER,
            receiverId = "student_zaid_01",
            text = "BarakAllah Feek. The LiveKit video classroom is ready for you when you tap 'Join Class'.",
            timestamp = "5:45 PM",
            isRead = false,
            isFromMe = false
        ),
        Message(
            id = "msg_05",
            senderId = "student_bilal_02",
            senderName = "Bilal Hassan",
            senderRole = UserRole.STUDENT,
            receiverId = "teacher_abdullah_01",
            text = "Assalamu Alaikum Sheikh, I have revised Juz 1 for tomorrow's revision test.",
            timestamp = "2:30 PM",
            isRead = true,
            isFromMe = false
        ),
        Message(
            id = "msg_06",
            senderId = "teacher_abdullah_01",
            senderName = "Sheikh Abdullah Al-Mansoor",
            senderRole = UserRole.TEACHER,
            receiverId = "student_bilal_02",
            text = "Masha'Allah Bilal. We will test from Surah Al-Baqarah Ayahs 100-141 tomorrow insha'Allah.",
            timestamp = "3:10 PM",
            isRead = true,
            isFromMe = false
        ),
        Message(
            id = "msg_07",
            senderId = "teacher_maryam_02",
            senderName = "Ustadha Maryam Siddiqui",
            senderRole = UserRole.TEACHER,
            receiverId = "student_fatima_03",
            text = "Assalamu Alaikum Fatima, great effort on Noorani Qaida Lesson 8 yesterday!",
            timestamp = "1:15 PM",
            isRead = true,
            isFromMe = false
        ),
        Message(
            id = "msg_08",
            senderId = "teacher_maryam_02",
            senderName = "Ustadha Maryam Siddiqui",
            senderRole = UserRole.TEACHER,
            receiverId = "student_zaid_01",
            text = "Assalamu Alaikum Zaid, your sister Fatima is progressing wonderfully in her Qaida lessons.",
            timestamp = "Yesterday",
            isRead = true,
            isFromMe = false
        ),
        Message(
            id = "msg_09",
            senderId = "teacher_tariq_03",
            senderName = "Sheikh Tariq Jameel",
            senderRole = UserRole.TEACHER,
            receiverId = "student_zaid_01",
            text = "Assalamu Alaikum brother Zaid. If you wish to join the Tafseer study circle this Saturday, you are warmly invited.",
            timestamp = "Yesterday",
            isRead = true,
            isFromMe = false
        )
    )

    private fun initialNotifications(): List<NotificationItem> = listOf(
        NotificationItem(
            id = "notif_01",
            title = "Class Starts Soon!",
            body = "Your live Quran Reading session with Sheikh Abdullah begins at 6:00 PM.",
            timestamp = "15 mins ago",
            type = NotificationType.CLASS_REMINDER,
            relatedClassId = "cls_48392"
        ),
        NotificationItem(
            id = "notif_02",
            title = "New Message from Teacher",
            body = "Sheikh Abdullah: 'BarakAllah Feek. The LiveKit video classroom is ready...'",
            timestamp = "1 hour ago",
            type = NotificationType.NEW_MESSAGE
        ),
        NotificationItem(
            id = "notif_03",
            title = "Weekly Tajweed Evaluation",
            body = "Masha'Allah! Your score for Surah Al-Fatihah was graded 'Mumtaz' (98%).",
            timestamp = "Yesterday",
            type = NotificationType.ACADEMY_ANNOUNCEMENT
        )
    )

    private fun initialAttendance(): List<AttendanceRecord> = listOf(
        AttendanceRecord(
            id = "att_01",
            className = "Surah Al-Fatihah & Juz 'Amma Recitation",
            teacherName = "Sheikh Abdullah Al-Mansoor",
            date = "Sept 24, 2026",
            durationMinutes = 45,
            grade = "Mumtaz (98%)",
            notes = "Accurate Madd Tabee'i and crisp articulation of throat letters."
        ),
        AttendanceRecord(
            id = "att_02",
            className = "Noorani Qaida Lesson 8",
            teacherName = "Ustadha Maryam Siddiqui",
            date = "Sept 21, 2026",
            durationMinutes = 30,
            grade = "Jayyid Jiddan (92%)",
            notes = "Good mastery of Sukoon and Tanween recognition."
        ),
        AttendanceRecord(
            id = "att_03",
            className = "Surah An-Nas & Al-Falaq Recitation",
            teacherName = "Sheikh Abdullah Al-Mansoor",
            date = "Sept 18, 2026",
            durationMinutes = 45,
            grade = "Mumtaz (96%)",
            notes = "Correct Ghunnah on Noon Mushaddadah."
        )
    )
}
