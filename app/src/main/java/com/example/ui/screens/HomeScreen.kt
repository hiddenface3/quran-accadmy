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
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showRoleSwitchDialog by remember { mutableStateOf(false) }
    val unreadNotifs = notifications.count { !it.isRead }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFAF9F5))) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_list")
        ) {
            item {
                AcademyHeader(
                    userProfile = currentUser,
                    unreadNotificationCount = unreadNotifs,
                    onNotificationsClick = {
                        showNotificationsSheet = true
                        viewModel.markNotificationsRead()
                    },
                    onRoleSwitchClick = { showRoleSwitchDialog = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Next Class Hero Card
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
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No classes scheduled for today.",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF616161)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Learning Progress
                    LearningProgressCard(userProfile = currentUser)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Upcoming Classes Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Classes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1B)
                        )
                        Text(
                            text = "View All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary,
                            modifier = Modifier
                                .clickable { onNavigateToClasses() }
                                .padding(4.dp)
                        )
                    }
                }
            }

            // List of upcoming classes
            val otherUpcoming = upcomingClasses.filter { it.id != nextClass?.id }.take(2)
            if (otherUpcoming.isEmpty()) {
                item {
                    Text(
                        text = "All scheduled sessions are up to date.",
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp)
                    )
                }
            } else {
                items(otherUpcoming) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
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
            }

            // Recent Messages Preview
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentUser.role == UserRole.TEACHER) "Student Messages" else "Teacher Messages",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1B)
                        )
                        Text(
                            text = "Open Chat",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary,
                            modifier = Modifier
                                .clickable { onNavigateToMessages() }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val lastTeacherMessage = messages.lastOrNull { !it.isFromMe }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToMessages() }
                            .testTag("recent_messages_preview_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lastTeacherMessage?.senderName ?: "Sheikh Abdullah",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF191C1B)
                                )
                                Text(
                                    text = lastTeacherMessage?.text ?: "No messages yet",
                                    fontSize = 12.sp,
                                    color = Color(0xFF616161),
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFBDBDBD)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Role Switch Dialog (Allows switching between Student, Teacher, and Admin)
    if (showRoleSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showRoleSwitchDialog = false },
            title = {
                Text(
                    text = "Switch Academy Role",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose which role perspective to preview in Quran Academy:",
                        fontSize = 13.sp,
                        color = Color(0xFF616161)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    RoleOptionItem(
                        title = "Student (Zaid Ahmed)",
                        desc = "View next class, join live lessons, read Quran, message teacher",
                        isSelected = currentUser.role == UserRole.STUDENT,
                        onClick = {
                            viewModel.signInAsRole(UserRole.STUDENT)
                            showRoleSwitchDialog = false
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    RoleOptionItem(
                        title = "Teacher (Sheikh Abdullah)",
                        desc = "Start live classes, manage students, reply to questions",
                        isSelected = currentUser.role == UserRole.TEACHER,
                        onClick = {
                            viewModel.signInAsRole(UserRole.TEACHER)
                            showRoleSwitchDialog = false
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    RoleOptionItem(
                        title = "Admin (Ustadh Ibrahim)",
                        desc = "Academy coordinator, manage scheduling and curriculum",
                        isSelected = currentUser.role == UserRole.ADMIN,
                        onClick = {
                            viewModel.signInAsRole(UserRole.ADMIN)
                            showRoleSwitchDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleSwitchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Notifications Bottom Sheet
    if (showNotificationsSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Academy Notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1B)
                    )
                    IconButton(onClick = { showNotificationsSheet = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                notifications.forEach { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F8F5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = notif.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF191C1B)
                                )
                                Text(
                                    text = notif.body,
                                    fontSize = 12.sp,
                                    color = Color(0xFF616161)
                                )
                                Text(
                                    text = notif.timestamp,
                                    fontSize = 10.sp,
                                    color = Color(0xFF9E9E9E),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RoleOptionItem(
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) EmeraldPrimary.copy(alpha = 0.12f) else Color(0xFFF5F5F5),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldPrimary) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) EmeraldPrimary else Color(0xFF191C1B)
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFF757575)
            )
        }
    }
}
