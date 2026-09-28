package com.example.ui.screens

import com.example.ui.screens.classroom.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.DisposableEffect
import com.example.utils.AudioRoute
import com.example.utils.AudioRoutingManager
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

    val isInPip by com.example.MainActivity.isInPipMode.collectAsStateWithLifecycle()
    val currentAudioRoute by AudioRoutingManager.currentRoute.collectAsStateWithLifecycle()
    val isBluetoothAvailable by AudioRoutingManager.isBluetoothAvailable.collectAsStateWithLifecycle()
    var showAudioRouteDialog by remember { mutableStateOf(false) }
    val syncedAyahIndex by com.example.livekit.LiveCallEngine.syncedAyahIndex.collectAsStateWithLifecycle()

    // 1. Screen Sleep Guard (FLAG_KEEP_SCREEN_ON)
    val activity = context as? android.app.Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 2. Real-Time Ayah Pointer Synchronization
    LaunchedEffect(syncedAyahIndex) {
        if (currentUser.role == UserRole.STUDENT && syncedAyahIndex in verses.indices) {
            viewModel.selectVerse(syncedAyahIndex)
        }
    }

    // 3. Picture-in-Picture Mode Minimal Rendering
    if (isInPip) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val pipTrack = if (isViewSwapped) localVideoTrack else remoteVideoTrack
            if (pipTrack != null) {
                LiveKitVideoRendererView(
                    room = liveKitRoom,
                    videoTrack = pipTrack,
                    mirror = isViewSwapped,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Quran Class Live",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }

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

                // Poor Network Warning Pill (Adaptive Network Drop Guard HUD)
                AnimatedVisibility(
                    visible = connectionQuality == "POOR" || connectionQuality == "VERY_POOR",
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Surface(
                        color = Color(0xFFE65100),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unstable network connection • Audio prioritized",
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
                                    if (selectedVerseIndex > 0) {
                                        val newIdx = selectedVerseIndex - 1
                                        viewModel.selectVerse(newIdx)
                                        if (currentUser.role == UserRole.TEACHER) {
                                            com.example.livekit.LiveCallEngine.sendAyahSync(newIdx, -1)
                                        }
                                    }
                                },
                                onNext = {
                                    if (selectedVerseIndex < verses.size - 1) {
                                        val newIdx = selectedVerseIndex + 1
                                        viewModel.selectVerse(newIdx)
                                        if (currentUser.role == UserRole.TEACHER) {
                                            com.example.livekit.LiveCallEngine.sendAyahSync(newIdx, -1)
                                        }
                                    }
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
                    currentAudioRoute = currentAudioRoute,
                    isHandRaised = isHandRaised,
                    isInClassChatOpen = isInClassChatOpen,
                    onToggleMic = { viewModel.toggleMic() },
                    onToggleCamera = { viewModel.toggleCamera() },
                    onSelectAudioRoute = { showAudioRouteDialog = true },
                    onToggleHandRaise = { viewModel.toggleHandRaise() },
                    onToggleChat = { viewModel.toggleInClassChat() },
                    onEnterPip = { (context as? com.example.MainActivity)?.enterPipMode() },
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

    // Interactive Audio Route Selector Dialog (Loudspeaker / Earpiece / Bluetooth)
    if (showAudioRouteDialog) {
        AlertDialog(
            onDismissRequest = { showAudioRouteDialog = false },
            title = {
                Text(
                    text = "Select Audio Output Device",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Loudspeaker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AudioRoutingManager.setAudioRoute(context, AudioRoute.SPEAKER)
                                showAudioRouteDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (currentAudioRoute == AudioRoute.SPEAKER) GoldSecondary else Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Loudspeaker",
                            color = if (currentAudioRoute == AudioRoute.SPEAKER) GoldSecondary else Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (currentAudioRoute == AudioRoute.SPEAKER) {
                            Text("Active", color = GoldSecondary, fontSize = 12.sp)
                        }
                    }

                    // Phone Earpiece
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AudioRoutingManager.setAudioRoute(context, AudioRoute.EARPIECE)
                                showAudioRouteDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = if (currentAudioRoute == AudioRoute.EARPIECE) GoldSecondary else Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Phone Earpiece",
                            color = if (currentAudioRoute == AudioRoute.EARPIECE) GoldSecondary else Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (currentAudioRoute == AudioRoute.EARPIECE) {
                            Text("Active", color = GoldSecondary, fontSize = 12.sp)
                        }
                    }

                    // Bluetooth Headset (if paired/connected)
                    if (isBluetoothAvailable) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AudioRoutingManager.setAudioRoute(context, AudioRoute.BLUETOOTH)
                                    showAudioRouteDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = if (currentAudioRoute == AudioRoute.BLUETOOTH) GoldSecondary else Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Bluetooth Headset",
                                color = if (currentAudioRoute == AudioRoute.BLUETOOTH) GoldSecondary else Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (currentAudioRoute == AudioRoute.BLUETOOTH) {
                                Text("Active", color = GoldSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAudioRouteDialog = false }) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = Color(0xFF1B2621)
        )
    }
}
