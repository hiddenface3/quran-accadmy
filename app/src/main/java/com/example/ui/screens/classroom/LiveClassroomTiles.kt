package com.example.ui.screens.classroom

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack

@Composable
fun RemoteLiveFeedTile(
    participantName: String,
    participantTitle: String,
    participantRole: String,
    room: Room?,
    videoTrack: VideoTrack?,
    isRemoteCameraOn: Boolean,
    isRemoteMicMuted: Boolean,
    isRemoteSpeaking: Boolean,
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
fun LocalLiveFeedTile(
    participantName: String,
    participantRole: String,
    hasPermission: Boolean,
    isCameraOn: Boolean,
    isMicMuted: Boolean,
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
fun FloatingPipTile(
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
fun TeacherFacePortrait(
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
fun StudentFacePortrait(
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
