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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
    val students by viewModel.students.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedClassForDetails by remember { mutableStateOf<QuranClass?>(null) }
    var showCreateClassDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Upcoming", "Previous", "All")

    val displayedClasses = when (selectedTabIndex) {
        0 -> upcomingClasses
        1 -> previousClasses
        else -> allClasses
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.BrandPageBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 03A. Screen Header
            Surface(
                color = com.example.ui.theme.BrandPageBackground,
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
                                fontWeight = FontWeight.SemiBold,
                                color = com.example.ui.theme.BrandPrimaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentUser.role == UserRole.STUDENT) "Enrolled curriculum & sessions" else "Teaching schedule & lessons",
                                fontSize = 11.sp,
                                color = com.example.ui.theme.BrandSecondaryText
                            )
                        }

                        if (currentUser.role == UserRole.ADMIN) {
                            Button(
                                onClick = { showCreateClassDialog = true },
                                shape = RoundedCornerShape(17.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = com.example.ui.theme.BrandPrimaryEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(width = 96.dp, height = 34.dp)
                            ) {
                                Text(
                                    text = "+ New Class",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 03B. Segmented Control (Height 42, Radius 21, Background #ECEEE8)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .background(Color(0xFFECEEE8), RoundedCornerShape(21.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTabIndex == index
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .padding(horizontal = 2.dp),
                                shape = RoundedCornerShape(17.dp),
                                color = if (isSelected) Color.White else Color.Transparent,
                                shadowElevation = if (isSelected) 1.5.dp else 0.dp,
                                onClick = { selectedTabIndex = index }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) com.example.ui.theme.BrandPrimaryEmerald else com.example.ui.theme.BrandSecondaryText
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
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

        // Floating Action Button for Admins to add a class
        if (currentUser.role == UserRole.ADMIN) {
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

    // Class Details Apple Floating Sheet Dialog
    selectedClassForDetails?.let { qClass ->
        AlertDialog(
            onDismissRequest = { selectedClassForDetails = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            tonalElevation = 8.dp,
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // iOS Drag Handle Indicator
                    Box(
                        modifier = Modifier
                            .size(36.dp, 5.dp)
                            .background(Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = qClass.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFD1FAE5), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (qClass.status == ClassStatus.LIVE_NOW) "• LIVE NOW" else "Scheduled",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                        IconButton(
                            onClick = { selectedClassForDetails = null },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF1F5F9), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Apple Inset Grouped Table
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            DetailRow(icon = Icons.Default.Person, label = "Student", value = qClass.studentName)
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
                            DetailRow(icon = Icons.Default.Person, label = "Teacher", value = qClass.teacherName)
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
                            DetailRow(icon = Icons.Default.DateRange, label = "Schedule", value = "${qClass.date} at ${qClass.startTime}")
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
                            DetailRow(icon = Icons.Default.Schedule, label = "Duration", value = "${qClass.durationMinutes} minutes")
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 14.dp))
                            DetailRow(icon = Icons.Default.Book, label = "Curriculum Topic", value = qClass.surahTopic)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Syllabus Notes Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3E8CE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Syllabus Notes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = qClass.syllabusNotes.ifBlank { "Live Quran recitation and Tajweed practice." },
                                fontSize = 12.sp,
                                color = Color(0xFF451A03),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    if (currentUser.role == UserRole.TEACHER) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.initiateClassCall(qClass.id)
                                viewModel.joinClass(qClass)
                                selectedClassForDetails = null
                                onNavigateToLiveClass(qClass)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("teacher_start_class_ring_button")
                        ) {
                            Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Class & Ring Student (${qClass.studentName})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else if (currentUser.role == UserRole.ADMIN) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.deleteClass(qClass.id)
                                selectedClassForDetails = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("admin_delete_class_from_dialog")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete This Class", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                        }
                    }
                }
            },
            confirmButton = {
                if (qClass.canJoin && currentUser.role != UserRole.ADMIN && currentUser.role != UserRole.TEACHER) {
                    Button(
                        onClick = {
                            selectedClassForDetails = null
                            viewModel.joinClass(qClass)
                            onNavigateToLiveClass(qClass)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Join Live Classroom", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = {}
        )
    }

    // Create Class Dialog
    if (showCreateClassDialog) {
        var newTitle by remember { mutableStateOf("Surah Al-Mulk Tajweed Session") }
        var newTopic by remember { mutableStateOf("Surah Al-Mulk Ayah 16-30") }
        var newDate by remember { mutableStateOf("Thursday, Oct 1") }
        var newTime by remember { mutableStateOf("6:00 PM") }
        var selectedStudent by remember { mutableStateOf(students.firstOrNull()?.name ?: "") }
        var selectedTeacher by remember { mutableStateOf(teachers.firstOrNull()?.name ?: "") }
        var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
        var selectedTeacherId by remember { mutableStateOf(teachers.firstOrNull()?.id ?: "") }
        var studentMenuExpanded by remember { mutableStateOf(false) }
        var teacherMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateClassDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Schedule New Quran Class",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Class Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Student picker - must be a real registered account, never free text,
                    // otherwise the class is invisible to them under RLS and calls can't connect.
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedStudent,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Student") },
                            placeholder = { if (students.isEmpty()) Text("No students registered yet") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable(enabled = students.isNotEmpty()) { studentMenuExpanded = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = students.isNotEmpty()) { studentMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = studentMenuExpanded,
                            onDismissRequest = { studentMenuExpanded = false }
                        ) {
                            students.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.name} (${s.email})") },
                                    onClick = {
                                        selectedStudent = s.name
                                        selectedStudentId = s.id
                                        if (s.assignedTeacherName.isNotBlank()) {
                                            selectedTeacher = s.assignedTeacherName
                                            selectedTeacherId = teachers.firstOrNull {
                                                it.name.equals(s.assignedTeacherName, ignoreCase = true)
                                            }?.id ?: selectedTeacherId
                                        }
                                        studentMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Teacher picker - same reasoning as above.
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedTeacher,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Teacher") },
                            placeholder = { if (teachers.isEmpty()) Text("No teachers registered yet") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable(enabled = teachers.isNotEmpty()) { teacherMenuExpanded = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = teachers.isNotEmpty()) { teacherMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = teacherMenuExpanded,
                            onDismissRequest = { teacherMenuExpanded = false }
                        ) {
                            teachers.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("${t.name} (${t.email})") },
                                    onClick = {
                                        selectedTeacher = t.name
                                        selectedTeacherId = t.id
                                        teacherMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTopic,
                        onValueChange = { newTopic = it },
                        label = { Text("Surah / Lesson Topic") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newDate,
                            onValueChange = { newDate = it },
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = newTime,
                            onValueChange = { newTime = it },
                            label = { Text("Time") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedStudent.isBlank() || selectedTeacher.isBlank()) {
                            return@Button
                        }
                        viewModel.createClass(
                            title = newTitle,
                            teacher = selectedTeacher,
                            student = selectedStudent,
                            date = newDate,
                            time = newTime,
                            duration = 45,
                            topic = newTopic,
                            teacherId = selectedTeacherId,
                            studentId = selectedStudentId
                        )
                        showCreateClassDialog = false
                    },
                    enabled = selectedStudent.isNotBlank() && selectedTeacher.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Schedule", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateClassDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
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
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color(0xFFE6F4EA), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B), modifier = Modifier.width(100.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
