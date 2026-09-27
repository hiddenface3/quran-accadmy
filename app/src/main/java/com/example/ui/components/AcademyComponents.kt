package com.example.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassStatus
import com.example.data.model.Message
import com.example.data.model.QuranClass
import com.example.data.model.UserProfile
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandDarkEmerald
import com.example.ui.theme.BrandDarkGold
import com.example.ui.theme.BrandDarkVideo
import com.example.ui.theme.BrandDivider
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandLiveRed
import com.example.ui.theme.BrandMint
import com.example.ui.theme.BrandMutedText
import com.example.ui.theme.BrandPageBackground
import com.example.ui.theme.BrandPrimaryEmerald
import com.example.ui.theme.BrandPrimaryText
import com.example.ui.theme.BrandSecondaryText
import com.example.ui.theme.BrandSoftGreenSurface
import com.example.ui.theme.BrandSoftSurface
import com.example.ui.theme.BrandSuccess
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.M3CanvasBackground
import com.example.ui.theme.M3CharcoalText
import com.example.ui.theme.M3EmeraldPrimary
import com.example.ui.theme.M3MintContainer
import com.example.ui.theme.M3MintSelected
import com.example.ui.theme.M3MintSubtle
import com.example.ui.theme.M3SageBorder
import com.example.ui.theme.M3SlateText
import com.example.ui.theme.M3SurfaceWhite

@Composable
fun AcademyHeader(
    userProfile: UserProfile,
    unreadNotificationCount: Int,
    onNotificationsClick: () -> Unit,
    onRoleSwitchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BrandPageBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("academy_header_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left greeting and identity
            Column {
                Text(
                    text = "As-salamu alaykum,",
                    color = BrandSecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userProfile.name,
                        color = BrandPrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = BrandSoftGreenSurface,
                        modifier = Modifier.height(22.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = userProfile.role.name,
                                color = BrandPrimaryEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Right notification and online avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 40x40 Touch target, 20x20 bell
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("notification_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(
                                    containerColor = BrandLiveRed,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Text(
                                        "$unreadNotificationCount",
                                        color = Color.White,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = BrandPrimaryEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 36x36 Avatar with 8x8 online dot
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BrandPrimaryEmerald,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = userProfile.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                    // 8x8 online dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .background(BrandPageBackground, CircleShape)
                            .padding(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BrandSuccess, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NextClassCard(
    quranClass: QuranClass,
    onJoinClick: () -> Unit,
    userRole: com.example.data.model.UserRole = com.example.data.model.UserRole.STUDENT,
    modifier: Modifier = Modifier
) {
    val isLive = quranClass.status == ClassStatus.LIVE_NOW

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("next_class_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BrandDarkEmerald),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLive) "● LIVE NOW" else "NEXT CLASS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLive) BrandGold else BrandMint,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = quranClass.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Topic Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = quranClass.surahTopic,
                                fontSize = 10.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Schedule
                    Text(
                        text = "Today · ${quranClass.startTime} · ${quranClass.durationMinutes} min",
                        fontSize = 11.sp,
                        color = BrandMint
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Teacher Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = quranClass.teacherName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = quranClass.teacherName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "Senior Tajweed Teacher",
                                fontSize = 9.sp,
                                color = BrandMint
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // CTA Button
                    Button(
                        onClick = onJoinClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = BrandPrimaryEmerald
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("join_class_button")
                    ) {
                        Text(
                            text = if (isLive) "Resume Live Class →" else "Join Classroom →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Right Decorative Quran / Atmosphere Graphic
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = BrandGold.copy(alpha = 0.7f),
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LearningProgressCard(
    userProfile: UserProfile,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("learning_progress_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Learning Progress",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimaryText
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Three Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Metric 1: Attendance Progress Ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { 0.96f },
                            color = BrandPrimaryEmerald,
                            trackColor = BrandSoftSurface,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "96%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimaryText
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Attendance",
                        fontSize = 10.sp,
                        color = BrandSecondaryText
                    )
                }

                // Metric 2: Hifz Progress Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { 0.10f },
                            color = BrandDarkGold,
                            trackColor = BrandSoftSurface,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "${userProfile.completedJuzCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimaryText
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Hifz Progress",
                        fontSize = 10.sp,
                        color = BrandSecondaryText
                    )
                }

                // Metric 3: Current Level Pill
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrandSoftGreenSurface,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = userProfile.tajweedLevel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimaryEmerald
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current Level",
                        fontSize = 10.sp,
                        color = BrandSecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Milestone Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Surah milestone",
                    fontSize = 9.sp,
                    color = BrandSecondaryText
                )
                Text(
                    text = "60%",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimaryEmerald
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Progress Bar (Height 6, Track #E7ECE8, Fill #0E5B44, Radius 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFE7ECE8), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(6.dp)
                        .background(BrandPrimaryEmerald, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun ClassListItem(
    quranClass: QuranClass,
    onJoinClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = quranClass.status == ClassStatus.LIVE_NOW
    val isCompleted = quranClass.status == ClassStatus.COMPLETED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("class_item_${quranClass.id}")
            .clickable { onCardClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandDivider)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Block (48x62, radius 14, background #EAF4EE)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BrandSoftGreenSurface,
                modifier = Modifier.size(width = 48.dp, height = 62.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MON",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimaryEmerald
                    )
                    Text(
                        text = "28",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimaryEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quranClass.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = quranClass.teacherName,
                    fontSize = 10.sp,
                    color = BrandSecondaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${quranClass.startTime} · ${quranClass.durationMinutes} min",
                    fontSize = 10.sp,
                    color = BrandSecondaryText
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Chip & Chevron
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (quranClass.status) {
                        ClassStatus.LIVE_NOW -> Color(0xFFFFEBEE)
                        ClassStatus.COMPLETED -> Color(0xFFE8F5E9)
                        else -> BrandSoftGreenSurface
                    },
                    modifier = Modifier.height(24.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = when (quranClass.status) {
                                ClassStatus.LIVE_NOW -> "Live"
                                ClassStatus.COMPLETED -> "Completed"
                                else -> "Upcoming"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (quranClass.status) {
                                ClassStatus.LIVE_NOW -> BrandDanger
                                ClassStatus.COMPLETED -> BrandSuccess
                                else -> BrandPrimaryEmerald
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = BrandMutedText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    val isMe = message.isFromMe

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isMe) BrandPrimaryEmerald else BrandSurface,
            shadowElevation = if (isMe) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 268.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = if (isMe) Color.White else BrandPrimaryText,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp,
                        fontSize = 9.sp,
                        color = if (isMe) BrandMint else BrandSecondaryText
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BrandMint,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}
