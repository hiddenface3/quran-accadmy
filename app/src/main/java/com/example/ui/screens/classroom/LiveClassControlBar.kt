package com.example.ui.screens.classroom

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandPrimaryEmerald
import com.example.utils.AudioRoute

@Composable
fun LiveClassControlBar(
    isMicMuted: Boolean,
    isCameraOn: Boolean,
    currentAudioRoute: AudioRoute,
    isHandRaised: Boolean,
    isInClassChatOpen: Boolean,
    onToggleMic: () -> Unit,
    onToggleCamera: () -> Unit,
    onSelectAudioRoute: () -> Unit,
    onToggleHandRaise: () -> Unit,
    onToggleChat: () -> Unit,
    onEnterPip: () -> Unit,
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

            // Audio Device Selector (Speaker / Ear / BT)
            ControlButton(
                icon = when (currentAudioRoute) {
                    AudioRoute.BLUETOOTH -> Icons.Default.Headphones
                    AudioRoute.EARPIECE -> Icons.Default.Headphones
                    AudioRoute.SPEAKER -> Icons.Default.VolumeUp
                },
                label = when (currentAudioRoute) {
                    AudioRoute.BLUETOOTH -> "BT"
                    AudioRoute.EARPIECE -> "Ear"
                    AudioRoute.SPEAKER -> "Speaker"
                },
                isActive = true,
                activeColor = Color.White.copy(alpha = 0.2f),
                inactiveColor = Color.White.copy(alpha = 0.2f),
                onClick = onSelectAudioRoute
            )

            // PiP Button
            ControlButton(
                icon = Icons.Default.PictureInPicture,
                label = "PiP",
                isActive = true,
                activeColor = Color.White.copy(alpha = 0.2f),
                inactiveColor = Color.White.copy(alpha = 0.2f),
                onClick = onEnterPip
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
fun ControlButton(
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
