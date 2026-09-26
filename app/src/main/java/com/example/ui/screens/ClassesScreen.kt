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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ClassStatus
import com.example.data.model.QuranClass
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.ClassListItem
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

@Composable
fun ClassesScreen(
    viewModel: MainViewModel,
    onNavigateToLiveClass: (QuranClass) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allClasses by viewModel.classes.collectAsStateWithLifecycle()
    val upcomingClasses by viewModel.upcomingClasses.collectAsStateWithLifecycle()
    val previousClasses by viewModel.previousClasses.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedClassForDetails by remember { mutableStateOf<QuranClass?>(null) }
    var showCreateClassDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Upcoming (${upcomingClasses.size})", "Previous (${previousClasses.size})", "All (${allClasses.size})")

    val displayedClasses = when (selectedTabIndex) {
        0 -> upcomingClasses
        1 -> previousClasses
        else -> allClasses
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9F5))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quran Classes",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191C1B)
                            )
                            Text(
                                text = if (currentUser.role == UserRole.STUDENT) "Enrolled Curriculum & Sessions" else "Teaching Schedule & Lessons",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }

                        if (currentUser.role == UserRole.TEACHER || currentUser.role == UserRole.ADMIN) {
                            Button(
                                onClick = { showCreateClassDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Class", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = EmeraldPrimary
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Classes List
            if (displayedClasses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No classes found in this category.",
                        fontSize = 15.sp,
                        color = Color(0xFF888888)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("classes_list")
                ) {
                    items(displayedClasses) { item ->
                        ClassListItem(
                            quranClass = item,
                            onJoinClick = {
                                viewModel.joinClass(item)
                                onNavigateToLiveClass(item)
                            },
                            onCardClick = {
                                selectedClassForDetails = item
                            },
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Floating Action Button for Teachers/Admins to add a class
        if (currentUser.role != UserRole.STUDENT) {
            FloatingActionButton(
                onClick = { showCreateClassDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("schedule_class_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Schedule Class")
            }
        }
    }

    // Class Details Dialog
    selectedClassForDetails?.let { qClass ->
        AlertDialog(
            onDismissRequest = { selectedClassForDetails = null },
            title = {
                Text(
                    text = qClass.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF191C1B)
                )
            },
            text = {
                Column {
                    DetailRow(icon = Icons.Default.Person, label = "Teacher", value = qClass.teacherName)
                    DetailRow(icon = Icons.Default.DateRange, label = "Schedule", value = "${qClass.date} at ${qClass.startTime}")
                    DetailRow(icon = Icons.Default.Schedule, label = "Duration", value = "${qClass.durationMinutes} minutes")
                    DetailRow(icon = Icons.Default.Book, label = "Curriculum Topic", value = qClass.surahTopic)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Syllabus Notes:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF454B46)
                    )
                    Text(
                        text = qClass.syllabusNotes,
                        fontSize = 12.sp,
                        color = Color(0xFF616161),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECEFF1),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "LiveKit Room: ${qClass.liveKitRoomName}",
                            fontSize = 11.sp,
                            color = Color(0xFF546E7A),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    if (currentUser.role == UserRole.TEACHER) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Teacher Actions:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF191C1B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                viewModel.initiateClassCall(qClass.id)
                                viewModel.joinClass(qClass)
                                selectedClassForDetails = null
                                onNavigateToLiveClass(qClass)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("teacher_start_class_ring_button")
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Class & Ring Student", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (currentUser.role == UserRole.ADMIN) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.deleteClass(qClass.id)
                                selectedClassForDetails = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_delete_class_from_dialog")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete This Class", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                if (qClass.canJoin && currentUser.role != UserRole.ADMIN) {
                    Button(
                        onClick = {
                            if (currentUser.role == UserRole.TEACHER) {
                                viewModel.initiateClassCall(qClass.id)
                            }
                            selectedClassForDetails = null
                            viewModel.joinClass(qClass)
                            onNavigateToLiveClass(qClass)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentUser.role == UserRole.TEACHER) "Start & Enter Class" else "Join Room")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedClassForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Create Class Dialog
    if (showCreateClassDialog) {
        var newTitle by remember { mutableStateOf("Surah Al-Mulk Tajweed Session") }
        var newTopic by remember { mutableStateOf("Surah Al-Mulk Ayah 16-30") }
        var newDate by remember { mutableStateOf("Thursday, Oct 1") }
        var newTime by remember { mutableStateOf("6:00 PM") }

        AlertDialog(
            onDismissRequest = { showCreateClassDialog = false },
            title = {
                Text("Schedule New Quran Class", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Class Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTopic,
                        onValueChange = { newTopic = it },
                        label = { Text("Surah / Lesson Topic") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newDate,
                            onValueChange = { newDate = it },
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newTime,
                            onValueChange = { newTime = it },
                            label = { Text("Time") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createClass(
                            title = newTitle,
                            teacher = currentUser.name,
                            student = "Zaid Ahmed",
                            date = newDate,
                            time = newTime,
                            duration = 45,
                            topic = newTopic
                        )
                        showCreateClassDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateClassDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF191C1B))
        Text(text = value, fontSize = 13.sp, color = Color(0xFF616161))
    }
}
