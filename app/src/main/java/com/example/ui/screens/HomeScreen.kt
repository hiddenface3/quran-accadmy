package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.QuranClass
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.AcademyHeader
import com.example.ui.components.ClassListItem
import com.example.ui.components.LearningProgressCard
import com.example.ui.components.NextClassCard
import com.example.ui.theme.EmeraldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToLiveClass: (QuranClass) -> Unit,
    onNavigateToClasses: () -> Unit,
    onNavigateToMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val nextClass by viewModel.nextClass.collectAsStateWithLifecycle()
    val upcomingClasses by viewModel.upcomingClasses.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    val unreadNotifs = notifications.count { !it.isRead }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.BrandPageBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_list")
        ) {
            // 02A. Header
            item {
                AcademyHeader(
                    userProfile = currentUser,
                    unreadNotificationCount = unreadNotifs,
                    onNotificationsClick = {
                        showNotificationsSheet = true
                        viewModel.markNotificationsRead()
                    },
                    onRoleSwitchClick = {}
                )
            }

            // 02B. Next Class Bento Hero
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    nextClass?.let { next ->
                        NextClassCard(
                            quranClass = next,
                            userRole = currentUser.role,
                            onJoinClick = {
                                if (currentUser.role == UserRole.TEACHER) {
                                    viewModel.initiateClassCall(next.id)
                                    viewModel.joinClass(next)
                                    onNavigateToLiveClass(next)
                                } else if (currentUser.role == UserRole.STUDENT) {
                                    viewModel.joinClass(next)
                                    onNavigateToLiveClass(next)
                                } else {
                                    onNavigateToClasses()
                                }
                            }
                        )
                    } ?: run {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.BrandDivider)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No classes scheduled for today.",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = com.example.ui.theme.BrandSecondaryText
                                )
                            }
                        }
                    }
                }
            }

            // 02C. Learning Progress Bento
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    LearningProgressCard(userProfile = currentUser)
                }
            }

            // 02D. Upcoming Classes Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Classes",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = com.example.ui.theme.BrandPrimaryText
                    )
                    Text(
                        text = "See All",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = com.example.ui.theme.BrandPrimaryEmerald,
                        modifier = Modifier
                            .clickable { onNavigateToClasses() }
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 02E. Class Cards
            val otherUpcoming = upcomingClasses.filter { it.id != nextClass?.id }.take(2)
            if (otherUpcoming.isEmpty()) {
                item {
                    Text(
                        text = "All scheduled sessions are up to date.",
                        fontSize = 12.sp,
                        color = com.example.ui.theme.BrandMutedText,
                        modifier = Modifier.padding(start = 20.dp, bottom = 16.dp)
                    )
                }
            } else {
                items(otherUpcoming) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        ClassListItem(
                            quranClass = item,
                            onJoinClick = {
                                viewModel.joinClass(item)
                                onNavigateToLiveClass(item)
                            },
                            onCardClick = { onNavigateToClasses() }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Apple Notifications Bottom Sheet
    if (showNotificationsSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 36.dp, height = 4.dp)
                ) {}
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.BrandPrimaryText
                    )
                    TextButton(
                        onClick = {
                            viewModel.markNotificationsRead()
                            showNotificationsSheet = false
                        }
                    ) {
                        Text(
                            text = "Mark all read",
                            fontSize = 12.sp,
                            color = com.example.ui.theme.BrandPrimaryEmerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                notifications.forEach { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.BrandSoftSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.BrandDivider),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = com.example.ui.theme.BrandSoftGreenSurface,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = com.example.ui.theme.BrandPrimaryEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = notif.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = com.example.ui.theme.BrandPrimaryText
                                )
                                Text(
                                    text = notif.body,
                                    fontSize = 12.sp,
                                    color = com.example.ui.theme.BrandSecondaryText
                                )
                                Text(
                                    text = notif.timestamp,
                                    fontSize = 10.sp,
                                    color = com.example.ui.theme.BrandMutedText,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

