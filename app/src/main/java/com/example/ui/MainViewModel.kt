package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.auth.AuthState
import com.example.data.backend.SupabaseConfig
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClassStatus
import com.example.data.model.Message
import com.example.data.model.NotificationItem
import com.example.data.model.QuranClass
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.AcademyRepository
import com.example.livekit.LiveCallEngine
import com.example.livekit.LiveKitConnectionState
import com.example.livekit.LiveKitParticipant
import com.example.livekit.LiveKitRoomInfo
import com.example.livekit.LiveKitTokenService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AcademyRepository()
    val authManager = AuthManager(application.applicationContext)
    private val liveKitTokenService = LiveKitTokenService()

    val currentUser: StateFlow<UserProfile> = repository.currentUser
    val classes: StateFlow<List<QuranClass>> = repository.classes
    val messages: StateFlow<List<Message>> = repository.messages
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications
    val attendance: StateFlow<List<AttendanceRecord>> = repository.attendance
    val authState: StateFlow<AuthState> = authManager.authState
    val students = repository.students
    val teachers = repository.teachers
    val incomingCallClass: StateFlow<QuranClass?> = repository.incomingCallClass

    val nextClass: StateFlow<QuranClass?> = classes.map { list ->
        list.firstOrNull { it.status == ClassStatus.LIVE_NOW || it.status == ClassStatus.SCHEDULED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val upcomingClasses: StateFlow<List<QuranClass>> = classes.map { list ->
        list.filter { it.status == ClassStatus.SCHEDULED || it.status == ClassStatus.LIVE_NOW }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previousClasses: StateFlow<List<QuranClass>> = classes.map { list ->
        list.filter { it.status == ClassStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Classroom State
    private val _activeLiveClass = MutableStateFlow<QuranClass?>(null)
    val activeLiveClass: StateFlow<QuranClass?> = _activeLiveClass.asStateFlow()

    private val _liveKitRoomInfo = MutableStateFlow<LiveKitRoomInfo?>(null)
    val liveKitRoomInfo: StateFlow<LiveKitRoomInfo?> = _liveKitRoomInfo.asStateFlow()

    private val _isJoiningClass = MutableStateFlow(false)
    val isJoiningClass: StateFlow<Boolean> = _isJoiningClass.asStateFlow()

    private val _joinErrorMessage = MutableStateFlow<String?>(null)
    val joinErrorMessage: StateFlow<String?> = _joinErrorMessage.asStateFlow()

    // Controls in live classroom
    val isMicMuted = MutableStateFlow(false)
    val isCameraOn = MutableStateFlow(true)
    val isSpeakerOn = MutableStateFlow(true)
    val isHandRaised = MutableStateFlow(false)
    val isQuranOverlayVisible = MutableStateFlow(false)
    val selectedQuranVerseIndex = MutableStateFlow(0)
    val isInClassChatOpen = MutableStateFlow(false)

    // Real-Time Video & VoIP Media States from LiveCallEngine
    val localVideoTrack: StateFlow<io.livekit.android.room.track.VideoTrack?> = LiveCallEngine.localVideoTrack
    val remoteVideoTrack: StateFlow<io.livekit.android.room.track.VideoTrack?> = LiveCallEngine.remoteVideoTrack
    val currentLiveKitRoom: io.livekit.android.room.Room? get() = LiveCallEngine.currentRoom

    val remoteVideoBitmap: StateFlow<Bitmap?> = LiveCallEngine.remoteVideoBitmap
    val remoteIsCameraOn: StateFlow<Boolean> = LiveCallEngine.remoteIsCameraOn
    val remoteIsMicMuted: StateFlow<Boolean> = LiveCallEngine.remoteIsMicMuted
    val remoteIsSpeaking: StateFlow<Boolean> = LiveCallEngine.remoteIsSpeaking
    val localAudioLevel: StateFlow<Float> = LiveCallEngine.localAudioLevel
    val remoteAudioLevel: StateFlow<Float> = LiveCallEngine.remoteAudioLevel
    val isPeerConnected: StateFlow<Boolean> = LiveCallEngine.isPeerConnected
    val connectionMode: StateFlow<String> = LiveCallEngine.connectionMode
    val connectionQuality: StateFlow<String> = LiveCallEngine.connectionQuality

    fun onLocalCameraFrame(bitmap: Bitmap) {
        LiveCallEngine.onLocalCameraFrame(bitmap)
    }

    // Call duration timer job
    private var callTimerJob: Job? = null
    val callDurationSeconds = MutableStateFlow(0L)

    init {
        authManager.prefs.loadConfigIntoMemory()
        val saved = authManager.prefs.getSavedUser()
        if (saved != null) {
            repository.updateCurrentUser(saved)
        }
        com.example.service.AcademyFirebaseMessagingService.initializeTopics(authManager.prefs)

        // Route real-time LiveKit DataChannel messages (<20ms) into repository chat flow
        LiveCallEngine.onDataMessageReceived = { incomingText ->
            repository.onLiveKitMessageReceived(incomingText)
        }

        viewModelScope.launch {
            incomingCallClass.collect { incoming ->
                if (incoming != null) {
                    com.example.service.CallRingtoneService.startRinging(
                        context = getApplication(),
                        teacherName = incoming.teacherName,
                        studentName = incoming.studentName,
                        classId = incoming.id,
                        roomName = incoming.liveKitRoomName
                    )
                } else {
                    com.example.service.CallRingtoneService.stopRinging(getApplication())
                }
            }
        }

        // Crash Recovery State Guard
        viewModelScope.launch {
            repository.classes.collect { classList ->
                val activeCallClassId = authManager.prefs.prefs.getString("active_call_class_id", null)
                if (activeCallClassId != null && _activeLiveClass.value == null && !_isJoiningClass.value) {
                    val activeClass = classList.find { it.id == activeCallClassId }
                    if (activeClass != null && activeClass.status == ClassStatus.LIVE_NOW) {
                        joinClass(activeClass)
                    }
                }
            }
        }
    }

    fun signInWithGoogle(
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle()
            result.onSuccess { user ->
                repository.updateCurrentUser(user)
                com.example.service.AcademyFirebaseMessagingService.initializeTopics(authManager.prefs)
                onSuccess()
            }.onFailure { e ->
                onFailure(e.localizedMessage ?: "Google sign-in canceled or not configured on device")
            }
        }
    }

    fun isEmailRegistered(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return false
        if (authManager.prefs.isEmailRegistered(cleanEmail)) return true
        if (students.value.any { it.email.trim().lowercase() == cleanEmail }) return true
        if (teachers.value.any { it.email.trim().lowercase() == cleanEmail }) return true
        if (com.example.auth.AuthManager.authorizedAdminEmails.contains(cleanEmail) || cleanEmail == "swabi5072@gmail.com") return true
        return false
    }

    fun registerNewUser(name: String, email: String, role: UserRole) {
        val user = authManager.signInWithEmailAndRole(name, email, role)
        authManager.prefs.addRegisteredEmail(email)
        repository.updateCurrentUser(user)
        com.example.service.AcademyFirebaseMessagingService.initializeTopics(authManager.prefs)
        syncDeviceTokenToBackend(user.id)
    }

    fun signInWithEmailAndRole(name: String, email: String, role: UserRole) {
        val user = authManager.signInWithEmailAndRole(name, email, role)
        repository.updateCurrentUser(user)
        com.example.service.AcademyFirebaseMessagingService.initializeTopics(authManager.prefs)
        syncDeviceTokenToBackend(user.id)
    }

    private fun syncDeviceTokenToBackend(userId: String) {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        authManager.prefs.saveFcmToken(token)
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            repository.backendService.updateUserFcmToken(userId, token)
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    fun signInAsRole(role: UserRole) {
        authManager.signInAsRole(role)
        val saved = authManager.prefs.getSavedUser()
        if (saved != null) {
            repository.updateCurrentUser(saved)
        } else {
            repository.switchRole(role)
        }
        com.example.service.AcademyFirebaseMessagingService.initializeTopics(authManager.prefs)
    }

    fun saveLiveKitCredentials(serverUrl: String, apiKey: String, apiSecret: String) {
        authManager.prefs.saveLiveKitConfig(serverUrl, apiKey, apiSecret)
    }

    fun saveSupabaseCredentials(projectUrl: String, anonKey: String) {
        authManager.prefs.saveSupabaseConfig(projectUrl, anonKey)
    }

    fun signOut() {
        authManager.signOut()
        leaveClass()
    }

    fun switchRole(role: UserRole) {
        signInAsRole(role)
    }

    fun joinClass(quranClass: QuranClass) {
        _isJoiningClass.value = true
        _joinErrorMessage.value = null
        _activeLiveClass.value = quranClass
        isCameraOn.value = true

        viewModelScope.launch {
            val user = currentUser.value
            val result = liveKitTokenService.requestClassAccessToken(quranClass, user)

            result.fold(
                onSuccess = { tokenResp ->
                    val isStudent = user.role == UserRole.STUDENT
                    val teacher = LiveKitParticipant(
                        id = "teacher_participant",
                        name = if (isStudent) quranClass.teacherName else user.name,
                        role = "Teacher",
                        isSpeaking = true,
                        isMicMuted = false,
                        isCameraOn = true,
                        audioLevel = 0.75f
                    )
                    val student = LiveKitParticipant(
                        id = "student_participant",
                        name = if (isStudent) user.name else quranClass.studentName,
                        role = "Student",
                        isSpeaking = false,
                        isMicMuted = isMicMuted.value,
                        isCameraOn = isCameraOn.value,
                        audioLevel = 0.2f
                    )

                    _liveKitRoomInfo.value = LiveKitRoomInfo(
                        classId = quranClass.id,
                        roomName = tokenResp.roomName,
                        serverUrl = tokenResp.serverUrl,
                        accessToken = tokenResp.token,
                        connectionState = LiveKitConnectionState.CONNECTED,
                        teacherParticipant = teacher,
                        studentParticipant = student,
                        pingMs = 24,
                        networkQuality = "Excellent (720p HD)"
                    )
                    _isJoiningClass.value = false
                    startCallTimer()

                    val isTeacher = (user.role == UserRole.TEACHER)
                    LiveCallEngine.setLocalMicMuted(isMicMuted.value)
                    LiveCallEngine.setLocalCameraOn(isCameraOn.value)
                    
                    // Crash Recovery State
                    authManager.prefs.prefs.edit().putString("active_call_class_id", quranClass.id).apply()
                    
                    // Foreground Service Guard
                    com.example.service.LiveKitCallService.startService(getApplication(), quranClass.title)

                    LiveCallEngine.startSession(
                        context = getApplication(),
                        classId = quranClass.id,
                        isTeacher = isTeacher,
                        backend = repository.backendService,
                        liveKitUrl = tokenResp.serverUrl,
                        liveKitToken = tokenResp.token
                    )
                },
                onFailure = { error ->
                    _isJoiningClass.value = false
                    _joinErrorMessage.value = error.localizedMessage ?: "Failed to connect to LiveKit classroom"
                }
            )
        }
    }

    private fun startCallTimer() {
        callDurationSeconds.value = 0L
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                callDurationSeconds.value += 1
            }
        }
    }

    fun endSession(classId: String) {
        LiveCallEngine.stopSession(getApplication())
        repository.endSession(classId)
        leaveClass(markCompleted = true)
    }

    fun leaveClass(markCompleted: Boolean = false) {
        val currentClass = _activeLiveClass.value
        LiveCallEngine.stopSession(getApplication())
        
        // Clear Crash Recovery State
        authManager.prefs.prefs.edit().remove("active_call_class_id").apply()
        
        // Stop Foreground Service
        com.example.service.LiveKitCallService.stopService(getApplication())

        callTimerJob?.cancel()
        callTimerJob = null
        callDurationSeconds.value = 0L
        _liveKitRoomInfo.value = null
        _activeLiveClass.value = null
        isHandRaised.value = false
        isQuranOverlayVisible.value = false
        isInClassChatOpen.value = false
        isCameraOn.value = false

        if (markCompleted && currentClass != null) {
            repository.endSession(currentClass.id)
        }
    }

    fun toggleMic() {
        isMicMuted.value = !isMicMuted.value
        LiveCallEngine.setLocalMicMuted(isMicMuted.value)
    }

    fun toggleCamera() {
        isCameraOn.value = !isCameraOn.value
        LiveCallEngine.setLocalCameraOn(isCameraOn.value)
    }

    fun toggleSpeaker() {
        isSpeakerOn.value = !isSpeakerOn.value
    }

    fun toggleHandRaise() {
        isHandRaised.value = !isHandRaised.value
    }

    fun toggleQuranOverlay() {
        isQuranOverlayVisible.value = !isQuranOverlayVisible.value
    }

    fun toggleInClassChat() {
        isInClassChatOpen.value = !isInClassChatOpen.value
    }

    fun selectVerse(index: Int) {
        selectedQuranVerseIndex.value = index
    }

    fun sendMessage(text: String, receiverId: String? = null, receiverName: String? = null) {
        if (_activeLiveClass.value != null) {
            // Instant sub-20ms delivery to classroom participants via WebRTC DataChannel
            LiveCallEngine.sendInCallChatMessage(text)
        }
        repository.sendMessage(text, receiverId, receiverName)
    }

    fun markNotificationsRead() {
        repository.markNotificationsAsRead()
    }

    fun createClass(
        title: String,
        teacher: String,
        student: String,
        date: String,
        time: String,
        duration: Int,
        topic: String
    ) {
        val newId = "cls_${System.currentTimeMillis() % 100000}"
        val newClass = QuranClass(
            id = newId,
            title = title,
            teacherName = teacher,
            studentName = student,
            date = date,
            startTime = time,
            durationMinutes = duration,
            status = ClassStatus.SCHEDULED,
            description = "Live recitation session covering $topic",
            liveKitRoomName = "quran-class-${newId}",
            surahTopic = topic
        )
        repository.addClass(newClass)
    }

    fun assignTeacherToStudent(studentId: String, teacherName: String) {
        repository.assignTeacherToStudent(studentId, teacherName)
    }

    fun deleteStudent(studentId: String) {
        repository.deleteStudent(studentId)
    }

    fun deleteTeacher(teacherId: String) {
        repository.deleteTeacher(teacherId)
    }

    fun deleteClass(classId: String) {
        repository.deleteClass(classId)
    }

    fun initiateClassCall(classId: String) {
        repository.initiateClassCall(classId)
    }

    fun acceptIncomingCall(qClass: QuranClass) {
        com.example.service.CallRingtoneService.stopRinging(getApplication())
        repository.dismissIncomingCall()
        joinClass(qClass)
    }

    fun dismissIncomingCall() {
        com.example.service.CallRingtoneService.stopRinging(getApplication())
        repository.dismissIncomingCall()
    }

    fun sendFiveMinuteReminder(classId: String) {
        repository.sendFiveMinuteReminder(classId)
    }

    fun cancelClass(classId: String) {
        repository.cancelClass(classId)
    }
}
