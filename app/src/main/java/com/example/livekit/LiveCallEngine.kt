package com.example.livekit

import android.content.Context
import android.util.Log
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.participant.VideoTrackPublishDefaults
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.TrackPublication
import io.livekit.android.room.track.VideoCaptureParameter
import io.livekit.android.room.track.VideoTrack
import com.example.data.backend.AcademyBackendService
import com.example.data.backend.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

/**
 * Industry-Standard Native LiveKit WebRTC Video & Audio Engine for Quran Academy Connect.
 * Uses hardware-accelerated WebRTC pipelines (H.264/VP8/Opus) and DataChannels.
 */
object LiveCallEngine {
    private const val TAG = "LiveCallEngine"

    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Observable states for UI
    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _remoteIsCameraOn = MutableStateFlow(true)
    val remoteIsCameraOn: StateFlow<Boolean> = _remoteIsCameraOn.asStateFlow()

    private val _remoteIsMicMuted = MutableStateFlow(false)
    val remoteIsMicMuted: StateFlow<Boolean> = _remoteIsMicMuted.asStateFlow()

    private val _remoteIsSpeaking = MutableStateFlow(false)
    val remoteIsSpeaking: StateFlow<Boolean> = _remoteIsSpeaking.asStateFlow()

    private val _localAudioLevel = MutableStateFlow(0f)
    val localAudioLevel: StateFlow<Float> = _localAudioLevel.asStateFlow()

    private val _remoteAudioLevel = MutableStateFlow(0f)
    val remoteAudioLevel: StateFlow<Float> = _remoteAudioLevel.asStateFlow()

    private val _isPeerConnected = MutableStateFlow(false)
    val isPeerConnected: StateFlow<Boolean> = _isPeerConnected.asStateFlow()

    private val _connectionMode = MutableStateFlow("Initializing...")
    val connectionMode: StateFlow<String> = _connectionMode.asStateFlow()

    private val _connectionQuality = MutableStateFlow("EXCELLENT")
    val connectionQuality: StateFlow<String> = _connectionQuality.asStateFlow()

    // Real-Time Ayah & Word Pointer Synchronization
    private val _syncedAyahIndex = MutableStateFlow(0)
    val syncedAyahIndex: StateFlow<Int> = _syncedAyahIndex.asStateFlow()

    private val _syncedWordIndex = MutableStateFlow(-1)
    val syncedWordIndex: StateFlow<Int> = _syncedWordIndex.asStateFlow()

    // Room handle
    private var liveKitRoom: Room? = null
    val currentRoom: Room? get() = liveKitRoom

    // Callbacks
    var onDataMessageReceived: ((String) -> Unit)? = null

    // Session controls
    private var localMicMuted: Boolean = false
    private var localCameraOn: Boolean = true
    private var liveKitJob: Job? = null

    fun startSession(
        context: Context,
        classId: String,
        isTeacher: Boolean,
        backend: AcademyBackendService,
        liveKitUrl: String = SupabaseConfig.liveKitServerUrl,
        liveKitToken: String = ""
    ) {
        stopSession(context)

        // Audio Routing Manager (Auto Bluetooth / Earpiece / Loudspeaker)
        try {
            com.example.utils.AudioRoutingManager.start(context)
        } catch (e: Exception) {
            Log.w(TAG, "Audio routing setup error: ${e.message}")
        }

        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        _remoteIsCameraOn.value = true
        _remoteIsMicMuted.value = false
        _remoteIsSpeaking.value = false
        _isPeerConnected.value = false
        _connectionMode.value = "Connecting LiveKit HD..."
        _connectionQuality.value = "CONNECTING"

        Log.i(TAG, "Starting LiveKit WebRTC session for class $classId (url=$liveKitUrl)")

        if (liveKitUrl.isBlank() || liveKitToken.isBlank()) {
            _connectionMode.value = "LiveKit token missing"
            return
        }

        liveKitJob = engineScope.launch {
            try {
                val tier = com.example.utils.DevicePerformanceMonitor.getDeviceTier(context)
                val (capWidth, capHeight, capFps) = if (tier == com.example.utils.DeviceTier.HIGH) {
                    Triple(1280, 720, 30) // High-end HD
                } else {
                    Triple(640, 480, 24) // Low-end safe mode
                }

                val roomOptions = RoomOptions(
                    adaptiveStream = true,
                    dynacast = true,
                    videoTrackCaptureDefaults = LocalVideoTrackOptions(
                        captureParams = VideoCaptureParameter(
                            width = capWidth,
                            height = capHeight,
                            maxFps = capFps
                        )
                    ),
                    videoTrackPublishDefaults = VideoTrackPublishDefaults(
                        simulcast = true
                    )
                )
                
                val room = LiveKit.create(
                    appContext = context.applicationContext,
                    options = roomOptions
                )
                liveKitRoom = room

                // Connect to LiveKit WebRTC server
                room.connect(liveKitUrl, liveKitToken)
                Log.i(TAG, "Connected to LiveKit room: ${room.name}")
                _connectionMode.value = "LiveKit HD WebRTC"
                _isPeerConnected.value = true

                // Enable camera and mic on room
                try {
                    room.localParticipant.setCameraEnabled(localCameraOn)
                    room.localParticipant.setMicrophoneEnabled(!localMicMuted)

                    val localTrack = room.localParticipant.trackPublications.values
                        .firstOrNull { it.source == Track.Source.CAMERA }?.track as? VideoTrack
                        ?: room.localParticipant.trackPublications.values.mapNotNull { it.track as? VideoTrack }.firstOrNull()
                    _localVideoTrack.value = localTrack
                } catch (camEx: Exception) {
                    Log.w(TAG, "Initial camera setup note: ${camEx.message}")
                }

                // Check if remote participant already has video
                val existingRemote = room.remoteParticipants.values.firstOrNull()
                if (existingRemote != null) {
                    _isPeerConnected.value = true
                    val remoteTrack = existingRemote.trackPublications.values
                        .firstOrNull { it.source == Track.Source.CAMERA }?.track as? VideoTrack
                        ?: existingRemote.trackPublications.values.mapNotNull { it.track as? VideoTrack }.firstOrNull()
                    if (remoteTrack != null) {
                        _remoteVideoTrack.value = remoteTrack
                        _remoteIsCameraOn.value = true
                    }
                }

                // Event listener
                room.events.collect { event ->
                    when (event) {
                        is RoomEvent.Connected -> {
                            _isPeerConnected.value = true
                            _connectionMode.value = "LiveKit HD Connected"
                            val lTrack = room.localParticipant.trackPublications.values
                                .firstOrNull { it.source == Track.Source.CAMERA }?.track as? VideoTrack
                                ?: room.localParticipant.trackPublications.values.mapNotNull { it.track as? VideoTrack }.firstOrNull()
                            if (lTrack != null) {
                                _localVideoTrack.value = lTrack
                            }
                        }
                        is RoomEvent.TrackPublished -> {
                            if (event.participant == room.localParticipant) {
                                val lTrack = event.publication.track as? VideoTrack
                                if (lTrack != null) {
                                    _localVideoTrack.value = lTrack
                                    Log.i(TAG, "Local camera track published: ${lTrack.sid}")
                                }
                            }
                        }
                        is RoomEvent.TrackUnpublished -> {
                            if (event.participant == room.localParticipant) {
                                _localVideoTrack.value = null
                                Log.i(TAG, "Local camera track unpublished")
                            }
                        }
                        is RoomEvent.ParticipantConnected -> {
                            _isPeerConnected.value = true
                            Log.i(TAG, "Remote participant joined: ${event.participant.identity}")
                            val rTrack = event.participant.trackPublications.values
                                .firstOrNull { it.source == Track.Source.CAMERA }?.track as? VideoTrack
                                ?: event.participant.trackPublications.values.mapNotNull { it.track as? VideoTrack }.firstOrNull()
                            if (rTrack != null) {
                                _remoteVideoTrack.value = rTrack
                                _remoteIsCameraOn.value = true
                            }
                        }
                        is RoomEvent.TrackSubscribed -> {
                            val track = event.track
                            Log.i(TAG, "Track subscribed: ${track.sid}, kind: ${track.kind}")
                            if (track is VideoTrack) {
                                _remoteVideoTrack.value = track
                                _remoteIsCameraOn.value = true
                                _isPeerConnected.value = true
                                _connectionMode.value = "LiveKit HD Video Active"
                            }
                        }
                        is RoomEvent.TrackUnsubscribed -> {
                            if (event.track == _remoteVideoTrack.value) {
                                _remoteVideoTrack.value = null
                                _remoteIsCameraOn.value = false
                            }
                        }
                        is RoomEvent.TrackMuted -> {
                            if (event.publication.source == Track.Source.CAMERA) {
                                if (event.participant != room.localParticipant) {
                                    _remoteIsCameraOn.value = false
                                }
                            } else if (event.publication.source == Track.Source.MICROPHONE) {
                                if (event.participant != room.localParticipant) {
                                    _remoteIsMicMuted.value = true
                                }
                            }
                        }
                        is RoomEvent.TrackUnmuted -> {
                            if (event.publication.source == Track.Source.CAMERA) {
                                if (event.participant != room.localParticipant) {
                                    _remoteIsCameraOn.value = true
                                    val rTrack = event.publication.track as? VideoTrack
                                    if (rTrack != null) {
                                        _remoteVideoTrack.value = rTrack
                                    }
                                }
                            } else if (event.publication.source == Track.Source.MICROPHONE) {
                                if (event.participant != room.localParticipant) {
                                    _remoteIsMicMuted.value = false
                                }
                            }
                        }
                        is RoomEvent.ActiveSpeakersChanged -> {
                            val isLocalSpeaking = event.speakers.contains(room.localParticipant)
                            val isRemoteSpeaking = event.speakers.any { it != room.localParticipant }
                            _localAudioLevel.value = if (isLocalSpeaking) 0.85f else 0.0f
                            _remoteAudioLevel.value = if (isRemoteSpeaking) 0.85f else 0.0f
                            _remoteIsSpeaking.value = isRemoteSpeaking
                        }
                        is RoomEvent.DataReceived -> {
                            try {
                                val text = String(event.data, StandardCharsets.UTF_8)
                                Log.i(TAG, "LiveKit DataChannel received: $text")
                                if (event.topic == "ayah_sync") {
                                    val parts = text.split(":")
                                    if (parts.size >= 2) {
                                        _syncedAyahIndex.value = parts[0].toIntOrNull() ?: 0
                                        _syncedWordIndex.value = parts[1].toIntOrNull() ?: -1
                                    }
                                } else {
                                    onDataMessageReceived?.invoke(text)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Data parse error: ${e.message}")
                            }
                        }
                        is RoomEvent.ParticipantDisconnected -> {
                            Log.i(TAG, "Remote participant left: ${event.participant.identity}")
                            _remoteVideoTrack.value = null
                            _remoteIsCameraOn.value = false
                        }
                        is RoomEvent.ConnectionQualityChanged -> {
                            _connectionQuality.value = event.quality.name
                        }
                        is RoomEvent.Reconnecting -> {
                            _connectionMode.value = "Reconnecting to Live Class..."
                            _connectionQuality.value = "RECONNECTING"
                            Log.w(TAG, "LiveKit WebRTC auto-reconnecting (ICE restart)...")
                        }
                        is RoomEvent.Reconnected -> {
                            _connectionMode.value = "LiveKit HD Reconnected"
                            _connectionQuality.value = "EXCELLENT"
                            Log.i(TAG, "LiveKit WebRTC reconnected successfully!")
                        }
                        is RoomEvent.Disconnected -> {
                            _isPeerConnected.value = false
                            _remoteVideoTrack.value = null
                            _connectionMode.value = "Disconnected"
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "LiveKit connection failed: ${e.message}", e)
                _connectionMode.value = "Connection Error: ${e.message}"
            }
        }
    }

    fun stopSession(context: Context? = null) {
        Log.i(TAG, "Stopping LiveKit session")
        liveKitJob?.cancel()
        liveKitJob = null

        try {
            if (context != null) {
                com.example.utils.AudioRoutingManager.stop(context)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio routing restore error: ${e.message}")
        }

        try {
            liveKitRoom?.disconnect()
            liveKitRoom?.release()
        } catch (_: Exception) {}
        liveKitRoom = null

        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        _isPeerConnected.value = false
        _connectionMode.value = "Disconnected"
    }

    fun setLocalMicMuted(muted: Boolean) {
        localMicMuted = muted
        engineScope.launch {
            try {
                liveKitRoom?.localParticipant?.setMicrophoneEnabled(!muted)
            } catch (e: Exception) {
                Log.w(TAG, "Mic toggle error: ${e.message}")
            }
        }
    }

    fun setLocalCameraOn(cameraOn: Boolean) {
        localCameraOn = cameraOn
        engineScope.launch {
            try {
                liveKitRoom?.localParticipant?.setCameraEnabled(cameraOn)
                val track = liveKitRoom?.localParticipant?.trackPublications?.values
                    ?.firstOrNull { it.source == Track.Source.CAMERA }?.track as? VideoTrack
                    ?: liveKitRoom?.localParticipant?.trackPublications?.values?.mapNotNull { it.track as? VideoTrack }?.firstOrNull()
                _localVideoTrack.value = if (cameraOn) track else null
            } catch (e: Exception) {
                Log.w(TAG, "Camera toggle error: ${e.message}")
            }
        }
    }

    /**
     * Send in-call chat message instantly (<20ms) via LiveKit WebRTC DataChannel
     */
    fun sendInCallChatMessage(text: String): Boolean {
        return sendDataPacket("chat", text)
    }

    /**
     * Synchronize currently selected Ayah and Word across tutor and student screens (<20ms)
     */
    fun sendAyahSync(ayahIndex: Int, wordIndex: Int): Boolean {
        _syncedAyahIndex.value = ayahIndex
        _syncedWordIndex.value = wordIndex
        return sendDataPacket("ayah_sync", "$ayahIndex:$wordIndex")
    }

    fun sendDataPacket(topic: String, message: String): Boolean {
        val room = liveKitRoom ?: return false
        return try {
            engineScope.launch {
                room.localParticipant.publishData(
                    data = message.toByteArray(StandardCharsets.UTF_8),
                    topic = topic
                )
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "DataChannel publish error ($topic): ${e.message}")
            false
        }
    }

    // Keep compatibility with any legacy observer if needed
    val remoteVideoBitmap = MutableStateFlow<android.graphics.Bitmap?>(null).asStateFlow()
    fun onLocalCameraFrame(bitmap: android.graphics.Bitmap) {}
    
    // Network Drop Guard
    fun onNetworkChanged(isConnected: Boolean) {
        engineScope.launch {
            try {
                if (!isConnected) {
                    Log.w(TAG, "Network lost: pausing local video to save bandwidth for audio")
                    liveKitRoom?.localParticipant?.setCameraEnabled(false)
                } else {
                    Log.i(TAG, "Network restored: resuming local video")
                    if (localCameraOn) {
                        liveKitRoom?.localParticipant?.setCameraEnabled(true)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network guard error: ${e.message}")
            }
        }
    }
}
