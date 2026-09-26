package com.example.livekit

enum class LiveKitConnectionState {
    IDLE,
    AUTHORIZING_STUDENT,
    ACQUIRING_TOKEN,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED
}

data class LiveKitParticipant(
    val id: String,
    val name: String,
    val role: String, // "Teacher" or "Student"
    val isSpeaking: Boolean = false,
    val isMicMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val isHandRaised: Boolean = false,
    val audioLevel: Float = 0.0f
)

data class LiveKitRoomInfo(
    val classId: String,
    val roomName: String,
    val serverUrl: String,
    val accessToken: String,
    val connectionState: LiveKitConnectionState = LiveKitConnectionState.IDLE,
    val teacherParticipant: LiveKitParticipant? = null,
    val studentParticipant: LiveKitParticipant? = null,
    val pingMs: Int = 28,
    val networkQuality: String = "Excellent (HD)",
    val durationSeconds: Long = 0L
)
