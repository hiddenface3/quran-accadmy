package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClassStatus
import com.example.data.model.QuranClass
import com.example.data.model.QuranCurriculumData
import com.example.data.model.UserRole
import com.example.livekit.LiveKitConnectionState
import com.example.ui.MainViewModel
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandDarkEmerald
import com.example.ui.theme.BrandDarkGold
import com.example.ui.theme.BrandDarkVideo
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandMint
import com.example.ui.theme.BrandPrimaryEmerald
import com.example.ui.theme.BrandSuccess
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

@Composable
fun LiveClassroomScreen(
    quranClass: QuranClass,
    viewModel: MainViewModel,
    onLeaveClass: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val roomInfo by viewModel.liveKitRoomInfo.collectAsStateWithLifecycle()
    val isJoining by viewModel.isJoiningClass.collectAsStateWithLifecycle()
    val joinError by viewModel.joinErrorMessage.collectAsStateWithLifecycle()
    val callDuration by viewModel.callDurationSeconds.collectAsStateWithLifecycle()

    val isMicMuted by viewModel.isMicMuted.collectAsStateWithLifecycle()
    val isCameraOn by viewModel.isCameraOn.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val isHandRaised by viewModel.isHandRaised.collectAsStateWithLifecycle()
    val isQuranOverlayVisible by viewModel.isQuranOverlayVisible.collectAsStateWithLifecycle()
    val selectedVerseIndex by viewModel.selectedQuranVerseIndex.collectAsStateWithLifecycle()
    val isInClassChatOpen by viewModel.isInClassChatOpen.collectAsStateWithLifecycle()

    val localVideoTrack by viewModel.localVideoTrack.collectAsStateWithLifecycle()
    val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsStateWithLifecycle()
    val liveKitRoom = viewModel.currentLiveKitRoom

    val remoteIsCameraOn by viewModel.remoteIsCameraOn.collectAsStateWithLifecycle()
    val remoteIsMicMuted by viewModel.remoteIsMicMuted.collectAsStateWithLifecycle()
    val remoteIsSpeaking by viewModel.remoteIsSpeaking.collectAsStateWithLifecycle()
    val localAudioLevel by viewModel.localAudioLevel.collectAsStateWithLifecycle()
    val remoteAudioLevel by viewModel.remoteAudioLevel.collectAsStateWithLifecycle()
    val isPeerConnected by viewModel.isPeerConnected.collectAsStateWithLifecycle()
    val connectionMode by viewModel.connectionMode.collectAsStateWithLifecycle()
    val connectionQuality by viewModel.connectionQuality.collectAsStateWithLifecycle()
    val allClasses by viewModel.classes.collectAsStateWithLifecycle()
    val classLiveState = allClasses.firstOrNull { it.id == quranClass.id }

    // Auto-shutdown session for student when teacher ends class
    LaunchedEffect(classLiveState?.status) {
        if (classLiveState != null && classLiveState.status == ClassStatus.COMPLETED && currentUser.role == UserRole.STUDENT) {
            android.widget.Toast.makeText(context, "Teacher has ended the live Quran class session.", android.widget.Toast.LENGTH_LONG).show()
            onLeaveClass()
        }
    }

    var showEndCallDialog by remember { mutableStateOf(false) }
    var isViewSwapped by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasMicPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
        
        if (hasCameraPermission) {
            viewModel.isCameraOn.value = true
            com.example.livekit.LiveCallEngine.setLocalCameraOn(true)
        }
        if (hasMicPermission) {
            viewModel.isMicMuted.value = false
            com.example.livekit.LiveCallEngine.setLocalMicMuted(false)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    val minutes = callDuration / 60
    val seconds = callDuration % 60
    val durationText = String.format("%02d:%02d", minutes, seconds)

    val verses = QuranCurriculumData.sampleVerses
    val currentVerse = verses.getOrElse(selectedVerseIndex) { verses.first() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1513))
            .testTag("live_classroom_screen")
    ) {
        if (isJoining) {
            // Authorizing & Connecting State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = GoldSecondary,
                        modifier = Modifier.size(54.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Securing LiveKit Classroom Room...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Verifying student enrollment & generating temporary token",
                        fontSize = 13.sp,
                        color = Color(0xFFC5EEDB),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (joinError != null) {
            // Connection Error
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Unable to Join Room",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF5350)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = joinError ?: "Unknown error occurred",
                        fontSize = 13.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.joinClass(quranClass) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Retry Connection")
                    }
                }
            }
        } else {
            // Main Live Video Classroom Stage
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Live Bar
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showEndCallDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Leave",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = quranClass.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FiberManualRecord,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE • $durationText",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF8A80),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "LiveKit HD",
                                            fontSize = 10.sp,
                                            color = Color(0xFF81C784)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        ConnectionQualityBars(connectionQuality)
                                    }
                                }
                            }
                        }

                        // Right actions: End Session button, Swap Views, Quran toggle & Hand raise indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // End Session Red Pill Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFD32F2F),
                                modifier = Modifier
                                    .clickable { showEndCallDialog = true }
                                    .testTag("top_bar_end_session_button")
                            ) {
                                Text(
                                    text = "End Session",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Swap Views Button
                            IconButton(
                                onClick = { isViewSwapped = !isViewSwapped },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("swap_views_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Swap Views",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            if (isHandRaised) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFFB300),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FrontHand,
                                        contentDescription = "Hand Raised",
                                        tint = Color.Black,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(16.dp)
                                    )
                                }
                            }

                            // Quran Board Button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isQuranOverlayVisible) GoldSecondary else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { viewModel.toggleQuranOverlay() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        tint = if (isQuranOverlayVisible) Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Quran Board",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isQuranOverlayVisible) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Reconnecting Network Recovery Banner (Industry-Standard WebRTC HUD)
                AnimatedVisibility(
                    visible = connectionQuality == "RECONNECTING" || connectionMode.contains("Reconnecting", ignoreCase = true),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        color = Color(0xFFD97706),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reconnecting to Live Class... Please hold on",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Middle Video Stage Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val isTeacher = currentUser.role == UserRole.TEACHER
                    val showRemoteInMain = !isViewSwapped

                    val remoteName = if (isTeacher) quranClass.studentName else quranClass.teacherName
                    val remoteTitle = if (isTeacher) "Student • Intermediate Tajweed Recitation" else quranClass.teacherTitle
                    val remoteRole = if (isTeacher) "Student" else "Teacher"

                    val localName = currentUser.name
                    val localRole = if (isTeacher) "Teacher" else "Student"

                    if (showRemoteInMain) {
                        // Main Stage: Remote Participant Real Live Video Feed
                        RemoteLiveFeedTile(
                            participantName = remoteName,
                            participantTitle = remoteTitle,
                            participantRole = remoteRole,
                            room = liveKitRoom,
                            videoTrack = remoteVideoTrack,
                            isRemoteCameraOn = remoteIsCameraOn,
                            isRemoteMicMuted = remoteIsMicMuted,
                            isRemoteSpeaking = remoteIsSpeaking,
                            audioLevel = remoteAudioLevel,
                            connectionMode = connectionMode,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Main Stage: Local Camera Full Screen (when swapped)
                        LocalLiveFeedTile(
                            participantName = localName,
                            participantRole = localRole,
                            hasPermission = hasCameraPermission,
                            isCameraOn = isCameraOn,
                            isMicMuted = isMicMuted,
                            localAudioLevel = localAudioLevel,
                            room = liveKitRoom,
                            videoTrack = localVideoTrack,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Floating Picture-In-Picture Tile (Top-Right)
                    FloatingPipTile(
                        showRemoteInPip = !showRemoteInMain,
                        remoteName = remoteName,
                        remoteRole = remoteRole,
                        room = liveKitRoom,
                        localVideoTrack = localVideoTrack,
                        remoteVideoTrack = remoteVideoTrack,
                        isRemoteCameraOn = remoteIsCameraOn,
                        isRemoteMicMuted = remoteIsMicMuted,
                        localName = localName,
                        localRole = localRole,
                        hasPermission = hasCameraPermission,
                        isCameraOn = isCameraOn,
                        isMicMuted = isMicMuted,
                        onSwap = { isViewSwapped = !isViewSwapped },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    )

                    // Interactive Quran Recitation Companion (Overlay over video)
                    if (isQuranOverlayVisible) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            QuranCompanionCard(
                                verse = currentVerse,
                                currentIndex = selectedVerseIndex,
                                totalCount = verses.size,
                                onPrev = {
                                    if (selectedVerseIndex > 0) viewModel.selectVerse(selectedVerseIndex - 1)
                                },
                                onNext = {
                                    if (selectedVerseIndex < verses.size - 1) viewModel.selectVerse(selectedVerseIndex + 1)
                                },
                                onClose = { viewModel.toggleQuranOverlay() }
                            )
                        }
                    }

                    // In-Class Quick Chat Overlay
                    if (isInClassChatOpen) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            InClassChatSheet(
                                quranClass = quranClass,
                                viewModel = viewModel,
                                onClose = { viewModel.toggleInClassChat() }
                            )
                        }
                    }
                }

                // Bottom Floating Control Bar
                LiveClassControlBar(
                    isMicMuted = isMicMuted,
                    isCameraOn = isCameraOn,
                    isSpeakerOn = isSpeakerOn,
                    isHandRaised = isHandRaised,
                    isInClassChatOpen = isInClassChatOpen,
                    onToggleMic = { viewModel.toggleMic() },
                    onToggleCamera = { viewModel.toggleCamera() },
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    onToggleHandRaise = { viewModel.toggleHandRaise() },
                    onToggleChat = { viewModel.toggleInClassChat() },
                    onEndCall = { showEndCallDialog = true }
                )
            }
        }
    }

    // Confirmation Dialog before leaving
    if (showEndCallDialog) {
        AlertDialog(
            onDismissRequest = { showEndCallDialog = false },
            title = {
                Text(
                    text = "End Live Quran Session?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Ending the session will conclude this live class, mark it as Completed, and disconnect the video feeds. The app will no longer show this session as Live.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndCallDialog = false
                        viewModel.endSession(quranClass.id)
                        onLeaveClass()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.testTag("confirm_end_session_button")
                ) {
                    Text("End Session")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showEndCallDialog = false
                            viewModel.leaveClass(markCompleted = false)
                            onLeaveClass()
                        }
                    ) {
                        Text("Leave Call Only")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showEndCallDialog = false }) {
                        Text("Stay in Class")
                    }
                }
            }
        )
    }
}

/**
 * High-performance hardware-accelerated LiveKit WebRTC video renderer for Jetpack Compose.
 * Connects directly to WebRTC hardware decoder and renders onto TextureView GPU surface.
 */
@Composable
fun LiveKitVideoRendererView(
    room: Room?,
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    mirror: Boolean = false
) {
    var boundTrack by remember { mutableStateOf<VideoTrack?>(null) }

    AndroidView(
        factory = { ctx ->
            TextureViewRenderer(ctx).apply {
                try {
                    room?.initVideoRenderer(this)
                    setMirror(mirror)
                    setEnableHardwareScaler(true)
                } catch (e: Exception) {
                    android.util.Log.w("LiveKitRenderer", "init error: ${e.message}")
                }
            }
        },
        update = { renderer ->
            try {
                renderer.setMirror(mirror)
                if (boundTrack != videoTrack) {
                    boundTrack?.removeRenderer(renderer)
                    boundTrack = videoTrack
                    videoTrack?.addRenderer(renderer)
                }
            } catch (e: Exception) {
                android.util.Log.w("LiveKitRenderer", "update error: ${e.message}")
            }
        },
        onRelease = { renderer ->
            try {
                boundTrack?.removeRenderer(renderer)
                boundTrack = null
                renderer.release()
            } catch (_: Exception) {}
        },
        modifier = modifier
    )
}

@Composable
private fun RemoteLiveFeedTile(
    participantName: String,
    participantTitle: String,
    participantRole: String,
    room: Room?,
    videoTrack: VideoTrack?,
    isRemoteCameraOn: Boolean,
    isRemoteMicMuted: Boolean,
    isRemoteSpeaking: Boolean,
    audioLevel: Float,
    connectionMode: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1E19),
                        Color(0xFF0A1512),
                        Color(0xFF050B09)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (videoTrack != null && isRemoteCameraOn) {
            // Live real WebRTC hardware GPU video feed from remote peer
            LiveKitVideoRendererView(
                room = room,
                videoTrack = videoTrack,
                mirror = false,
                modifier = Modifier.fillMaxSize()
            )

            // Live Stream HUD Badge (Top-Start)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE • $participantName ($participantRole)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldPrimary.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = connectionMode,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            // Camera Off or Connecting State with Animated Voice Aura
            val infiniteTransition = rememberInfiniteTransition(label = "remoteWave")
            val wavePulse by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wavePulse"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Pulsing aura when speaking
                    if (isRemoteSpeaking) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.25f),
                            modifier = Modifier
                                .size(170.dp)
                                .scale(wavePulse)
                        ) {}
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (participantRole == "Teacher") EmeraldPrimary else Color(0xFF1976D2),
                        border = androidx.compose.foundation.BorderStroke(3.dp, GoldSecondary),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = participantName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 48.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$participantRole LIVE FEED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = participantName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = participantTitle,
                    fontSize = 13.sp,
                    color = Color(0xFFC5EEDB)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (!isRemoteCameraOn) "Camera is Turned Off • Audio Connected" else "Connecting Live Video Feed...",
                    fontSize = 12.sp,
                    color = Color(0xFFB0BEC5)
                )
            }
        }

        // Live Audio & Recitation status at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isRemoteMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isRemoteMicMuted) Color(0xFFE53935) else (if (isRemoteSpeaking) Color(0xFF4CAF50) else Color(0xFF81C784)),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRemoteMicMuted) "Mic Muted" else (if (isRemoteSpeaking) "Speaking Live • VoIP" else "Voice Connected"),
                    fontSize = 11.sp,
                    color = if (isRemoteMicMuted) Color(0xFFFF8A80) else Color(0xFF81C784),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Encrypted VoIP",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalLiveFeedTile(
    participantName: String,
    participantRole: String,
    hasPermission: Boolean,
    isCameraOn: Boolean,
    isMicMuted: Boolean,
    localAudioLevel: Float,
    room: Room?,
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1E19)),
        contentAlignment = Alignment.Center
    ) {
        if (hasPermission && isCameraOn) {
            LiveKitVideoRendererView(
                room = room,
                videoTrack = videoTrack,
                mirror = true,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = Color(0xFF4CAF50), modifier = Modifier.size(8.dp)) {}
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "YOU ($participantRole) • LIVE CAMERA",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary,
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = participantName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your Camera is Off",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap 'Start Cam' in control bar to stream your face",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun FloatingPipTile(
    showRemoteInPip: Boolean,
    remoteName: String,
    remoteRole: String,
    room: Room?,
    localVideoTrack: VideoTrack?,
    remoteVideoTrack: VideoTrack?,
    isRemoteCameraOn: Boolean,
    isRemoteMicMuted: Boolean,
    localName: String,
    localRole: String,
    hasPermission: Boolean,
    isCameraOn: Boolean,
    isMicMuted: Boolean,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(width = 130.dp, height = 175.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                2.dp,
                if ((showRemoteInPip && isRemoteMicMuted) || (!showRemoteInPip && isMicMuted)) Color(0xFFE53935) else EmeraldPrimary,
                RoundedCornerShape(18.dp)
            )
            .clickable { onSwap() }
            .testTag("interactive_pip_tile"),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E2824),
        shadowElevation = 10.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!showRemoteInPip) {
                // PiP displays Local Camera Preview
                if (hasPermission && isCameraOn) {
                    LiveKitVideoRendererView(
                        room = room,
                        videoTrack = localVideoTrack,
                        mirror = true,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color(0xFF24362E), Color(0xFF14201B)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(localName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("You ($localRole)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Cam Off", fontSize = 9.sp, color = Color(0xFFC5EEDB))
                        }
                    }
                }
            } else {
                // PiP displays Remote Participant Video
                if (remoteVideoTrack != null && isRemoteCameraOn) {
                    LiveKitVideoRendererView(
                        room = room,
                        videoTrack = remoteVideoTrack,
                        mirror = false,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color(0xFF24362E), Color(0xFF14201B)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = if (remoteRole == "Teacher") EmeraldPrimary else Color(0xFF1976D2),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(remoteName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(remoteName, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("Cam Off", fontSize = 9.sp, color = Color(0xFFC5EEDB))
                        }
                    }
                }
            }

            // Top Swap Hint Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap",
                        tint = GoldSecondary,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Tap to swap", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }

            // Mic status pill in PiP
            val pipMuted = if (showRemoteInPip) isRemoteMicMuted else isMicMuted
            Surface(
                shape = CircleShape,
                color = if (pipMuted) Color(0xFFD32F2F) else Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (pipMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TeacherFacePortrait(
    teacherName: String,
    teacherTitle: String,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "teacherSpeech")

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "teacherWave"
    )

    val mouthHeight by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "teacherMouth"
    )

    val eyeBlink by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "teacherBlink"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Audio wave halo & Teacher Face
            Box(contentAlignment = Alignment.Center) {
                // Outer acoustic ripple
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary.copy(alpha = 0.20f),
                    modifier = Modifier
                        .size(170.dp)
                        .scale(waveScale)
                ) {}

                // Inner gold acoustic ring
                Surface(
                    shape = CircleShape,
                    color = GoldSecondary.copy(alpha = 0.15f),
                    modifier = Modifier.size(145.dp)
                ) {}

                // Realistic Teacher Face Composable
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF132F27),
                    border = androidx.compose.foundation.BorderStroke(3.dp, GoldSecondary),
                    modifier = Modifier.size(125.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)
                        ) {
                            // White Kufi / Ghutra headdress with gold embroidery
                            Surface(
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                                color = Color(0xFFFBFBFB),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldSecondary),
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Surface(
                                        color = GoldSecondary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .align(Alignment.BottomCenter)
                                    ) {}
                                }
                            }

                            // Face base
                            Surface(
                                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                                color = Color(0xFFE2BA98),
                                modifier = Modifier
                                    .fillMaxWidth(0.68f)
                                    .height(58.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    // Eyes with blinking
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF2C1E14),
                                            modifier = Modifier
                                                .width(7.dp)
                                                .height((4 * eyeBlink).dp.coerceAtLeast(1.dp))
                                        ) {}
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF2C1E14),
                                            modifier = Modifier
                                                .width(7.dp)
                                                .height((4 * eyeBlink).dp.coerceAtLeast(1.dp))
                                        ) {}
                                    }

                                    // Nose
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = Color(0xFFCC9975),
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(7.dp)
                                    ) {}

                                    // Reciting Animated Mouth
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF8D4A4A),
                                        modifier = Modifier
                                            .width(14.dp)
                                            .height(mouthHeight.dp)
                                    ) {}

                                    // Trimmed Sheikh Beard
                                    Surface(
                                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                                        color = Color(0xFF212B26),
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(14.dp)
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldSecondary.copy(alpha = 0.6f)),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "TEACHER LIVE FEED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = teacherName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = teacherTitle,
                fontSize = 13.sp,
                color = Color(0xFFC5EEDB)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EmeraldPrimary.copy(alpha = 0.25f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Reciting: Surah Al-Mulk (Ayah 1-15)",
                        fontSize = 12.sp,
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "LiveKit HD 1080p • 30fps Encrypted Feed",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun StudentFacePortrait(
    studentName: String,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "studentSpeech")

    val mouthHeight by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(340, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "studentMouth"
    )

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "studentWave"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1976D2).copy(alpha = 0.20f),
                    modifier = Modifier
                        .size(160.dp)
                        .scale(waveScale)
                ) {}

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1A382C),
                    border = androidx.compose.foundation.BorderStroke(3.dp, EmeraldPrimary),
                    modifier = Modifier.size(125.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                                color = Color(0xFFF5F5F5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth(0.68f)
                                    .height(24.dp)
                            ) {}

                            Surface(
                                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                                color = Color(0xFFECC8A8),
                                modifier = Modifier
                                    .fillMaxWidth(0.64f)
                                    .height(52.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF2C1E14),
                                            modifier = Modifier.size(5.dp)
                                        ) {}
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF2C1E14),
                                            modifier = Modifier.size(5.dp)
                                        ) {}
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = Color(0xFFD29F78),
                                        modifier = Modifier.width(4.dp).height(6.dp)
                                    ) {}

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF8D4A4A),
                                        modifier = Modifier
                                            .width(12.dp)
                                            .height(mouthHeight.dp)
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "STUDENT LIVE FEED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = studentName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Student • Intermediate Tajweed Recitation",
                fontSize = 13.sp,
                color = Color(0xFFC5EEDB)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Reciting Ayahs • Tajweed Audio Live",
                    fontSize = 12.sp,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QuranCompanionCard(
    verse: com.example.data.model.QuranVerse,
    currentIndex: Int,
    totalCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("quran_companion_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = BrandDarkEmerald.copy(alpha = 0.94f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandDarkGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${verse.surahName} · Ayah ${verse.ayahNumber} of $totalCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = BrandGold
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = BrandMint)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Arabic text in large, readable script
            Text(
                text = verse.arabicText,
                fontSize = 24.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = verse.transliteration,
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = BrandMint.copy(alpha = 0.8f)
            )

            Text(
                text = verse.translation,
                fontSize = 13.sp,
                color = BrandGold,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (verse.tajweedNote.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Tajweed: ${verse.tajweedNote}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandMint,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pagination Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrev,
                    enabled = currentIndex > 0
                ) {
                    Icon(imageVector = Icons.Default.NavigateBefore, contentDescription = "Prev", tint = Color.White)
                }

                Text(
                    text = "Ayah ${currentIndex + 1} of $totalCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandMint
                )

                IconButton(
                    onClick = onNext,
                    enabled = currentIndex < totalCount - 1
                ) {
                    Icon(imageVector = Icons.Default.NavigateNext, contentDescription = "Next", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun InClassChatSheet(
    quranClass: QuranClass,
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var textInput by remember { mutableStateOf("") }

    val isTeacher = currentUser.role == UserRole.TEACHER
    val recipientName = if (isTeacher) quranClass.studentName else quranClass.teacherName

    val classMessages = remember(messages, currentUser, quranClass) {
        messages.filter { msg ->
            val sender = msg.senderName.trim()
            val receiver = msg.receiverName.trim()
            val me = currentUser.name.trim()
            val other = recipientName.trim()
            (sender.equals(me, ignoreCase = true) && (receiver.isEmpty() || receiver.equals(other, ignoreCase = true))) ||
                    (sender.equals(other, ignoreCase = true) && (receiver.isEmpty() || receiver.equals(me, ignoreCase = true)))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .padding(16.dp)
            .testTag("in_class_chat_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "In-Class Live Chat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Chatting with $recipientName", fontSize = 11.sp, color = EmeraldPrimary)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (classMessages.isEmpty()) {
                    item {
                        Text(
                            text = "No messages yet in this session. Send a note or greeting below.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(classMessages.takeLast(8)) { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (msg.isFromMe) EmeraldPrimary.copy(alpha = 0.15f) else Color(0xFFF0F0F0)
                            ) {
                                Text(
                                    text = "${if (msg.isFromMe) "You" else msg.senderName}: ${msg.text}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF191C1B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Message $recipientName...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput, null, recipientName)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(EmeraldPrimary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveClassControlBar(
    isMicMuted: Boolean,
    isCameraOn: Boolean,
    isSpeakerOn: Boolean,
    isHandRaised: Boolean,
    isInClassChatOpen: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHandRaise: () -> Unit,
    onToggleChat: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic Control
            ControlButton(
                icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMicMuted) "Unmute" else "Mute",
                isActive = !isMicMuted,
                activeColor = Color.White.copy(alpha = 0.2f),
                inactiveColor = Color(0xFFD32F2F),
                onClick = onToggleMic
            )

            // Camera Control
            ControlButton(
                icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                label = if (isCameraOn) "Stop Cam" else "Start Cam",
                isActive = isCameraOn,
                activeColor = Color.White.copy(alpha = 0.2f),
                inactiveColor = Color(0xFFD32F2F),
                onClick = onToggleCamera
            )

            // Speakerphone Control
            ControlButton(
                icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.Headphones,
                label = if (isSpeakerOn) "Speaker" else "Earpiece",
                isActive = isSpeakerOn,
                activeColor = Color.White.copy(alpha = 0.2f),
                inactiveColor = Color.White.copy(alpha = 0.2f),
                onClick = onToggleSpeaker
            )

            // Raise Hand
            ControlButton(
                icon = Icons.Default.FrontHand,
                label = "Hand",
                isActive = isHandRaised,
                activeColor = Color(0xFFFFB300),
                inactiveColor = Color.White.copy(alpha = 0.2f),
                onClick = onToggleHandRaise
            )

            // In-Call Chat
            ControlButton(
                icon = Icons.Default.Chat,
                label = "Chat",
                isActive = isInClassChatOpen,
                activeColor = BrandPrimaryEmerald,
                inactiveColor = Color.White.copy(alpha = 0.2f),
                onClick = onToggleChat
            )

            // End Session Red Button (44x44 circular red button)
            Surface(
                shape = CircleShape,
                color = Color(0xFFD32F2F),
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onEndCall() }
                    .testTag("end_class_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Session",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) activeColor else inactiveColor,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFB0BEC5)
        )
    }
}

@Composable
fun ConnectionQualityBars(quality: String, modifier: Modifier = Modifier) {
    val barColor = when (quality.uppercase()) {
        "EXCELLENT" -> Color(0xFF4CAF50)
        "GOOD" -> Color(0xFFFFB300)
        else -> Color(0xFFE53935)
    }
    val activeBars = when (quality.uppercase()) {
        "EXCELLENT" -> 3
        "GOOD" -> 2
        else -> 1
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(4.dp)
                .background(if (activeBars >= 1) barColor else Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(7.dp)
                .background(if (activeBars >= 2) barColor else Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(10.dp)
                .background(if (activeBars >= 3) barColor else Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
        )
    }
}
