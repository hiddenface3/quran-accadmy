package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthState
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var googleSignInNote by remember { mutableStateOf<String?>(null) }

    val trimmedEmail = inputEmail.trim().lowercase()
    val trimmedName = inputName.trim().lowercase()

    // Auto-detect existing accounts
    val matchingAdmin = if (trimmedEmail == "swabi5072@gmail.com" || (trimmedEmail.isNotBlank() && trimmedEmail.contains("admin")) || (trimmedName.isNotBlank() && trimmedName.contains("admin"))) {
        "Academy Administrator"
    } else null

    val matchingTeacher = teachers.firstOrNull { t ->
        (trimmedEmail.isNotBlank() && t.email.trim().lowercase() == trimmedEmail) ||
        (trimmedName.isNotBlank() && t.name.trim().lowercase() == trimmedName)
    }

    val matchingStudent = students.firstOrNull { s ->
        (trimmedEmail.isNotBlank() && s.email.trim().lowercase() == trimmedEmail) ||
        (trimmedName.isNotBlank() && s.name.trim().lowercase() == trimmedName)
    }

    val detectedExistingAccount: Pair<UserRole, String>? = when {
        matchingAdmin != null -> Pair(UserRole.ADMIN, matchingAdmin)
        matchingTeacher != null -> Pair(UserRole.TEACHER, matchingTeacher.name)
        matchingStudent != null -> Pair(UserRole.STUDENT, matchingStudent.name)
        else -> null
    }

    val activeRole = detectedExistingAccount?.first ?: if (trimmedEmail.contains("admin")) UserRole.ADMIN else selectedRole

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF09392B),
                        Color(0xFF0E5B44),
                        Color(0xFFF9F8F5),
                        Color(0xFFFAF9F5)
                    ),
                    startY = 0f,
                    endY = 1200f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Academy Logo Crest
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(88.dp)
                    .border(2.dp, GoldSecondary, CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFFDF5), Color(0xFFF1EAD8))
                        )
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Quran Academy Crest",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Quran Academy",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Live 1-on-1 Quran Classroom with LiveKit & Supabase",
                fontSize = 12.sp,
                color = Color(0xFFC5EEDB),
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Device Setup & Sign In",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = "Sign in on this phone as Student, Teacher, or Admin.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Role selector tabs
                    Text(
                        text = "CHOOSE THIS DEVICE ROLE:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleSelectionTab(
                            title = "Student",
                            subtitle = if (activeRole == UserRole.STUDENT && detectedExistingAccount != null) "Detected" else "Portal",
                            isSelected = activeRole == UserRole.STUDENT,
                            onClick = { selectedRole = UserRole.STUDENT },
                            modifier = Modifier.weight(1f)
                        )
                        RoleSelectionTab(
                            title = "Teacher",
                            subtitle = if (activeRole == UserRole.TEACHER && detectedExistingAccount != null) "Detected" else "Portal",
                            isSelected = activeRole == UserRole.TEACHER,
                            onClick = { selectedRole = UserRole.TEACHER },
                            modifier = Modifier.weight(1f)
                        )
                        RoleSelectionTab(
                            title = "Admin",
                            subtitle = if (activeRole == UserRole.ADMIN && detectedExistingAccount != null) "Detected" else "Portal",
                            isSelected = activeRole == UserRole.ADMIN,
                            onClick = { selectedRole = UserRole.ADMIN },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input: Full Name
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Your Full Name") },
                        placeholder = {
                            Text(
                                when (activeRole) {
                                    UserRole.STUDENT -> "e.g. Zaid Ahmed"
                                    UserRole.TEACHER -> "e.g. Sheikh Abdullah"
                                    UserRole.ADMIN -> "e.g. Ustadh Ibrahim"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF059669))
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_name_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input: Email / Gmail
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Gmail or Academy Email") },
                        placeholder = {
                            Text(
                                when (activeRole) {
                                    UserRole.ADMIN -> "swabi5072@gmail.com"
                                    UserRole.TEACHER -> "teacher@gmail.com"
                                    UserRole.STUDENT -> "student@gmail.com"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFF059669))
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_email_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    // Account Already Exists Banner (Apple HIG Callout Card)
                    if (detectedExistingAccount != null) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Account already exists!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = "Identity: ${detectedExistingAccount.second} (${activeRole.name.lowercase().replaceFirstChar { it.uppercase() }}). Tap below to log in.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }
                        }
                    } else if (matchingAdmin != null || trimmedEmail.contains("admin")) {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Authorized Academy Director Email detected", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Button: Apple Pill Sign In
                    Button(
                        onClick = {
                            val finalRole = activeRole
                            val finalEmail = if (inputEmail.isNotBlank()) inputEmail.trim() else when (finalRole) {
                                UserRole.STUDENT -> matchingStudent?.email ?: "student.quran@gmail.com"
                                UserRole.TEACHER -> matchingTeacher?.email ?: "teacher.abdullah@gmail.com"
                                UserRole.ADMIN -> "swabi5072@gmail.com"
                            }
                            val finalName = if (inputName.isNotBlank()) inputName.trim() else when (finalRole) {
                                UserRole.STUDENT -> matchingStudent?.name ?: "Zaid Ahmed"
                                UserRole.TEACHER -> matchingTeacher?.name ?: "Sheikh Abdullah Al-Mansoor"
                                UserRole.ADMIN -> "Ustadh Ibrahim (Director)"
                            }

                            viewModel.signInWithEmailAndRole(finalName, finalEmail, finalRole)
                            onLoginSuccess(finalRole)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("sign_in_and_sync_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text(
                            text = if (detectedExistingAccount != null) {
                                "Log In to Existing ${activeRole.name.lowercase().replaceFirstChar { it.uppercase() }} Account"
                            } else {
                                "Log In as ${activeRole.name.lowercase().replaceFirstChar { it.uppercase() }}"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary: Continue with Google (Apple Inset Style)
                    OutlinedButton(
                        onClick = {
                            viewModel.signInWithGoogle(
                                onSuccess = {
                                    onLoginSuccess(viewModel.currentUser.value.role)
                                },
                                onFailure = { errMsg ->
                                    googleSignInNote = "Google Play Services note: $errMsg. You can use the form above to sign in directly with your Gmail."
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continue_with_google_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4285F4),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google Account",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }

                    googleSignInNote?.let { note ->
                        Text(
                            text = note,
                            fontSize = 11.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                        Text(
                            text = "  ONE-TAP PRESETS  ",
                            fontSize = 10.sp,
                            color = Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Bold
                        )
                        Divider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1-Tap Quick Setup Presets for 3 Phones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetButton(
                            title = "Student",
                            sub = "Zaid",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.signInWithEmailAndRole("zaidkhan", "mytest5072@gmail.com", UserRole.STUDENT)
                                onLoginSuccess(UserRole.STUDENT)
                            }
                        )
                        PresetButton(
                            title = "Teacher",
                            sub = "Saqib",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.signInWithEmailAndRole("saqib saib", "itskhan7733@gmail.com", UserRole.TEACHER)
                                onLoginSuccess(UserRole.TEACHER)
                            }
                        )
                        PresetButton(
                            title = "Admin",
                            sub = "Director",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.signInWithEmailAndRole("Admin Khan", "swabi5072@gmail.com", UserRole.ADMIN)
                                onLoginSuccess(UserRole.ADMIN)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoleSelectionTab(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF059669) else Color(0xFFF1F5F9),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF1E293B)
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isSelected) Color(0xFFD1FAE5) else Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun PresetButton(
    title: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text(text = sub, fontSize = 9.sp, color = Color(0xFF64748B))
        }
    }
}
