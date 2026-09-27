package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuranClass
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandDarkEmerald
import com.example.ui.theme.BrandDarkGold
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandMint
import com.example.ui.theme.BrandPrimaryEmerald
import com.example.ui.theme.BrandSuccess

@Composable
fun IncomingCallOverlay(
    quranClass: QuranClass,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // 09A. FULL BACKGROUND (#09392B -> #071F18)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandDarkEmerald,
                        Color(0xFF071F18)
                    )
                )
            )
            .testTag("incoming_call_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 09B. TOP STATUS PILL (h = 38dp, radius: 19dp, background: rgba(255,255,255,0.10), border: 1dp #C89B3C, gold text)
            Surface(
                shape = RoundedCornerShape(19.dp),
                color = Color.White.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandDarkGold),
                modifier = Modifier.height(38.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE QURAN CLASS CALLING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // 09C & 09D. CALLER AVATAR & NAME
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 09C. CALLER AVATAR (Outer aura 170x170 animated pulse, inner 112x112 circle, border 3dp #FFD54F, bg #0E5B44)
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        shape = CircleShape,
                        color = BrandSuccess.copy(alpha = 0.20f),
                        modifier = Modifier
                            .size(170.dp)
                            .scale(pulseScale)
                    ) {}

                    Surface(
                        shape = CircleShape,
                        color = BrandPrimaryEmerald,
                        border = androidx.compose.foundation.BorderStroke(3.dp, BrandGold),
                        modifier = Modifier.size(112.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = quranClass.teacherName.take(1).uppercase(),
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 09D. CALLER NAME & TITLE (26sp Bold white, 12sp #C5EEDB)
                Text(
                    text = quranClass.teacherName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = quranClass.teacherTitle,
                    fontSize = 12.sp,
                    color = BrandMint,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 09E. CLASS DETAILS BENTO (radius: 22dp, background: rgba(255,255,255,0.10))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = quranClass.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = BrandGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = quranClass.surahTopic,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${quranClass.durationMinutes} minutes",
                                fontSize = 11.sp,
                                color = BrandMint
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = BrandMint,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LiveKit HD WebRTC",
                                        fontSize = 10.sp,
                                        color = BrandMint,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 09F. ACTION BUTTONS (Decline: circle 68x68 #D32F2F; Accept: circle 68x68 #2E7D32 with emerald aura)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DECLINE BUTTON
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = BrandDanger,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(68.dp)
                            .clickable { onDecline() }
                            .testTag("decline_call_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Decline",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // ACCEPT BUTTON
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            shape = CircleShape,
                            color = BrandSuccess.copy(alpha = 0.35f),
                            modifier = Modifier
                                .size(88.dp)
                                .scale(pulseScale)
                        ) {}

                        Surface(
                            shape = CircleShape,
                            color = BrandSuccess,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(68.dp)
                                .clickable { onAccept() }
                                .testTag("accept_call_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Accept Call",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Accept Call",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
