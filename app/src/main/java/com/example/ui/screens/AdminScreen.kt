package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.data.model.StudentInfo
import com.example.data.model.TeacherInfo
import com.example.ui.MainViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

@Composable
fun AdminScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val students: List<StudentInfo> by viewModel.students.collectAsStateWithLifecycle()
    val teachers: List<TeacherInfo> by viewModel.teachers.collectAsStateWithLifecycle()
    val classes: List<QuranClass> by viewModel.classes.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Student Roster (${students.size})", "Class Operations (${classes.size})", "Teachers (${teachers.size})")

    var selectedStudentToAssign by remember { mutableStateOf<StudentInfo?>(null) }
    var showCreateClassDialog by remember { mutableStateOf(false) }
    var feedbackBannerMessage by remember { mutableStateOf<String?>(null) }

    var studentToDelete by remember { mutableStateOf<StudentInfo?>(null) }
    var teacherToDelete by remember { mutableStateOf<TeacherInfo?>(null) }
    var classToDelete by remember { mutableStateOf<QuranClass?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF9F5))
            .testTag("admin_screen")
    ) {
        // 08A. Admin Header Banner
        Surface(
            color = com.example.ui.theme.BrandPageBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = com.example.ui.theme.BrandSoftGreenSurface,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.BrandPrimaryEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Admin Control Center",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = com.example.ui.theme.BrandPrimaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentUser.email,
                                fontSize = 11.sp,
                                color = com.example.ui.theme.BrandSecondaryText
                            )
                        }
                    }

                    Button(
                        onClick = { showCreateClassDialog = true },
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.BrandPrimaryEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(width = 104.dp, height = 34.dp)
                            .testTag("admin_schedule_class_button")
                    ) {
                        Text(
                            text = "+ Add Class",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 08B. Stats Bento Card (Three Columns with subtle dividers)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.BrandDivider),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${students.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.BrandPrimaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Students",
                                fontSize = 10.sp,
                                color = com.example.ui.theme.BrandSecondaryText
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(com.example.ui.theme.BrandDivider)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${classes.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.BrandPrimaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Classes",
                                fontSize = 10.sp,
                                color = com.example.ui.theme.BrandSecondaryText
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(com.example.ui.theme.BrandDivider)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${teachers.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.BrandPrimaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Teachers",
                                fontSize = 10.sp,
                                color = com.example.ui.theme.BrandSecondaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 08C. Admin Segmented Control (Height 42, Radius 21, Background #ECEEE8)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color(0xFFECEEE8), RoundedCornerShape(21.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val adminTabs = listOf("Student Roster", "Class Operations", "Teachers")
                    adminTabs.forEachIndexed { index, title ->
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
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) com.example.ui.theme.BrandPrimaryEmerald else com.example.ui.theme.BrandSecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Feedback Banner
        feedbackBannerMessage?.let { msg ->
            Surface(
                color = Color(0xFFE8F5E9),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = msg, fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    IconButton(onClick = { feedbackBannerMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFF2E7D32))
                    }
                }
            }
        }

        // Content Area based on tab
        when (selectedTabIndex) {
            0 -> {
                // Students & Teacher Assignment
                if (students.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Students in Database",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF191C1B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Only real users saved in Supabase will appear here. When a student signs in or registers, their profile is synced automatically.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("admin_students_list")
                    ) {
                        items(items = students, key = { it.id }) { student ->
                            StudentAdminCard(
                                student = student,
                                onAssignClick = { selectedStudentToAssign = student },
                                onDeleteClick = { studentToDelete = student }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Class Operations & Administration
                if (classes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Classes in Database",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF191C1B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap '+ Add Class' above to schedule and assign a session for a real student and teacher.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("admin_classes_list")
                    ) {
                        items(items = classes, key = { it.id }) { qClass ->
                            ClassAdminCard(
                                quranClass = qClass,
                                onDeleteClass = { classToDelete = qClass }
                            )
                        }
                    }
                }
            }
            2 -> {
                // Teachers Directory
                if (teachers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Teachers in Database",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF191C1B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Only real teachers saved in Supabase will appear here. When a teacher signs in or registers, their profile is synced automatically.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("admin_teachers_list")
                    ) {
                        items(items = teachers, key = { it.id }) { teacher ->
                            TeacherAdminCard(
                                teacher = teacher,
                                onDeleteTeacher = { teacherToDelete = teacher }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirm Delete Student Dialog
    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete student '${student.name}' (${student.email}) from the academy database in Supabase?",
                    fontSize = 13.sp,
                    color = Color(0xFF454B46)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(student.id)
                        feedbackBannerMessage = "Student '${student.name}' was removed from the database."
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete Teacher Dialog
    teacherToDelete?.let { teacher ->
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            title = { Text("Delete Teacher?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete teacher '${teacher.name}' (${teacher.email}) from the academy database in Supabase?",
                    fontSize = 13.sp,
                    color = Color(0xFF454B46)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTeacher(teacher.id)
                        feedbackBannerMessage = "Teacher '${teacher.name}' was removed from the database."
                        teacherToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete Class Dialog
    classToDelete?.let { qClass ->
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { Text("Delete Class?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete class '${qClass.title}' (${qClass.teacherName} with ${qClass.studentName}) from Supabase?",
                    fontSize = 13.sp,
                    color = Color(0xFF454B46)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClass(qClass.id)
                        feedbackBannerMessage = "Class '${qClass.title}' was deleted from the database."
                        classToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Delete Class")
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Teacher Assignment Dialog
    selectedStudentToAssign?.let { student ->
        AlertDialog(
            onDismissRequest = { selectedStudentToAssign = null },
            title = {
                Text(
                    text = "Assign Teacher to ${student.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Current Teacher: ${student.assignedTeacherName}",
                        fontSize = 13.sp,
                        color = Color(0xFF616161)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Select New Instructor:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (teachers.isEmpty()) {
                        Text(
                            text = "No teachers registered in the database yet. Please have a teacher sign in first to assign them.",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    } else {
                        teachers.forEach { t ->
                            val isAssigned = student.assignedTeacherName.equals(t.name, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAssigned) EmeraldPrimary.copy(alpha = 0.12f) else Color(0xFFF5F5F5),
                                border = if (isAssigned) androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldPrimary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.assignTeacherToStudent(student.id, t.name)
                                        feedbackBannerMessage = "Assigned ${t.name} to ${student.name}!"
                                        selectedStudentToAssign = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = EmeraldPrimary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = t.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = t.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "${t.title} • ${t.email}", fontSize = 11.sp, color = Color(0xFF757575))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedStudentToAssign = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Schedule New Class Dialog
    if (showCreateClassDialog) {
        var title by remember { mutableStateOf("Quran Tajweed Recitation") }
        var topic by remember { mutableStateOf("Surah Al-Mulk (Ayah 15-30)") }
        var date by remember { mutableStateOf("Today") }
        var time by remember { mutableStateOf("10:00 PM") }
        var duration by remember { mutableStateOf("45") }
        var selectedStudent by remember { mutableStateOf(students.firstOrNull()?.name ?: "") }
        var selectedTeacher by remember { mutableStateOf(teachers.firstOrNull()?.name ?: "") }

        var studentMenuExpanded by remember { mutableStateOf(false) }
        var teacherMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreateClassDialog = false },
            title = {
                Text("Admin: Schedule New Quran Class", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Class Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Student Picker Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedStudent,
                            onValueChange = { selectedStudent = it },
                            readOnly = students.isNotEmpty(),
                            label = { Text("Assigned Student") },
                            trailingIcon = if (students.isNotEmpty()) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.clickable { studentMenuExpanded = true }
                                    )
                                }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (students.isNotEmpty()) Modifier.clickable { studentMenuExpanded = true } else Modifier)
                        )
                        if (students.isNotEmpty()) {
                            DropdownMenu(
                                expanded = studentMenuExpanded,
                                onDismissRequest = { studentMenuExpanded = false }
                            ) {
                                students.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.name} (${s.email})") },
                                        onClick = {
                                            selectedStudent = s.name
                                            if (s.assignedTeacherName.isNotBlank()) {
                                                selectedTeacher = s.assignedTeacherName
                                            }
                                            studentMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Teacher Picker Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedTeacher,
                            onValueChange = { selectedTeacher = it },
                            readOnly = teachers.isNotEmpty(),
                            label = { Text("Assigned Teacher") },
                            trailingIcon = if (teachers.isNotEmpty()) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.clickable { teacherMenuExpanded = true }
                                    )
                                }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (teachers.isNotEmpty()) Modifier.clickable { teacherMenuExpanded = true } else Modifier)
                        )
                        if (teachers.isNotEmpty()) {
                            DropdownMenu(
                                expanded = teacherMenuExpanded,
                                onDismissRequest = { teacherMenuExpanded = false }
                            ) {
                                teachers.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text("${t.name} (${t.email})") },
                                        onClick = {
                                            selectedTeacher = t.name
                                            teacherMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Surah / Lesson Topic") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Time (e.g. 10:00 PM)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createClass(
                            title = title,
                            teacher = selectedTeacher,
                            student = selectedStudent,
                            date = date,
                            time = time,
                            duration = duration.toIntOrNull() ?: 45,
                            topic = topic
                        )
                        feedbackBannerMessage = "Scheduled class for $selectedStudent with $selectedTeacher at $time!"
                        showCreateClassDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Schedule Class")
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
private fun AdminMetricChip(
    title: String,
    count: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF1EFEA)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            Text(text = title, fontSize = 11.sp, color = Color(0xFF616161))
        }
    }
}

@Composable
private fun StudentAdminCard(
    student: StudentInfo,
    onAssignClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("admin_student_card_${student.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = student.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = student.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1B)
                        )
                        Text(
                            text = student.email,
                            fontSize = 11.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = student.attendanceRate + " Attended",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFAF9F5),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Assigned Teacher:", fontSize = 10.sp, color = Color(0xFF757575))
                        Text(
                            text = if (student.assignedTeacherName.isNotBlank()) student.assignedTeacherName else "Not assigned yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (student.assignedTeacherName.isNotBlank()) EmeraldPrimary else Color(0xFFE65100)
                        )
                    }

                    OutlinedButton(
                        onClick = onAssignClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("reassign_button_${student.id}")
                    ) {
                        Icon(imageVector = Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (student.assignedTeacherName.isBlank()) "Assign" else "Reassign", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tajweed: ${student.tajweedLevel}",
                    fontSize = 11.sp,
                    color = Color(0xFF616161),
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = onDeleteClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                    modifier = Modifier.testTag("delete_student_${student.id}")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 11.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ClassAdminCard(
    quranClass: QuranClass,
    onDeleteClass: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = quranClass.status == ClassStatus.LIVE_NOW

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("admin_class_card_${quranClass.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${quranClass.date} • ${quranClass.startTime}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLive) Color(0xFFE53935) else EmeraldPrimary
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (quranClass.status) {
                        ClassStatus.LIVE_NOW -> Color(0xFFFFEBEE)
                        ClassStatus.SCHEDULED -> Color(0xFFE3F2FD)
                        ClassStatus.COMPLETED -> Color(0xFFE8F5E9)
                        ClassStatus.CANCELLED -> Color(0xFFEEEEEE)
                    }
                ) {
                    Text(
                        text = quranClass.status.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (quranClass.status) {
                            ClassStatus.LIVE_NOW -> Color(0xFFE53935)
                            ClassStatus.SCHEDULED -> Color(0xFF1976D2)
                            ClassStatus.COMPLETED -> Color(0xFF2E7D32)
                            ClassStatus.CANCELLED -> Color(0xFF757575)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = quranClass.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF191C1B)
            )

            Text(
                text = "Teacher: ${quranClass.teacherName}  ➜  Student: ${quranClass.studentName}",
                fontSize = 12.sp,
                color = Color(0xFF616161),
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = "Topic: ${quranClass.surahTopic}",
                fontSize = 11.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDeleteClass,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_delete_class_${quranClass.id}")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Class Session", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TeacherAdminCard(
    teacher: TeacherInfo,
    onDeleteTeacher: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = GoldSecondary.copy(alpha = 0.2f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = teacher.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = teacher.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C1B)
                )
                Text(
                    text = teacher.title,
                    fontSize = 12.sp,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Email: ${teacher.email}",
                    fontSize = 11.sp,
                    color = Color(0xFF757575)
                )
                Text(
                    text = "Ijazah: ${teacher.tajweedIjazah}",
                    fontSize = 11.sp,
                    color = Color(0xFF757575)
                )
                Text(
                    text = "Availability: ${teacher.availability}",
                    fontSize = 10.sp,
                    color = Color(0xFF9E9E9E)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1EFEA)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${teacher.assignedStudentCount}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(text = "Students", fontSize = 9.sp, color = Color(0xFF616161))
                    }
                }

                IconButton(
                    onClick = onDeleteTeacher,
                    modifier = Modifier.size(36.dp).testTag("delete_teacher_${teacher.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Teacher",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
