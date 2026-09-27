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
import androidx.compose.material.icons.filled.ChevronRight
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
                        com.example.ui.theme.M3MintContainer.copy(alpha = 0.6f),
                        com.example.ui.theme.M3CanvasBackground,
                        Color.White
                    ),
                    startY = 0f,
                    endY = 800f
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
            Spacer(modifier = Modifier.height(24.dp))

            // Sacred Quran Academy Connect Emblem (Stitch M3 Design)
            Surface(
                shape = CircleShape,
                color = com.example.ui.theme.M3MintContainer,
                modifier = Modifier
                    .size(76.dp)
                    .border(2.dp, com.example.ui.theme.M3EmeraldPrimary.copy(alpha = 0.25f), CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Quran Academy Emblem",
                                tint = com.example.ui.theme.M3EmeraldPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quran Academy Connect",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = com.example.ui.theme.M3CharcoalText,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "Welcome back! Enter your details to access your live classes",
                fontSize = 13.sp,
                color = com.example.ui.theme.M3SlateText,
                modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Card (Material 3 Elevated Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.M3SageOutline),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Material 3 SELECT ROLE Label
                    Text(
                        text = "SELECT ROLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.M3MutedText,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Material 3 FilterChips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf(
                            Triple(UserRole.STUDENT, "Student", Icons.Default.School),
                            Triple(UserRole.TEACHER, "Teacher", Icons.Default.Person),
                            Triple(UserRole.ADMIN, "Admin", Icons.Default.Security)
                        )

                        roles.forEach { (role, label, icon) ->
                            val isSelected = activeRole == role
                            Surface(
                                onClick = { selectedRole = role },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) com.example.ui.theme.M3MintSelected else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) com.example.ui.theme.M3EmeraldPrimary else com.example.ui.theme.M3SageOutline
                                ),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = com.example.ui.theme.M3EmeraldPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) com.example.ui.theme.M3EmeraldPrimary else com.example.ui.theme.M3SlateText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) com.example.ui.theme.M3EmeraldPrimary else com.example.ui.theme.M3SlateText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Input: Full Name (Material 3 OutlinedTextField)
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Full Name") },
                        placeholder = {
                            Text(
                                when (activeRole) {
                                    UserRole.STUDENT -> "e.g., Zaid Ahmed"
                                    UserRole.TEACHER -> "e.g., Sheikh Abdullah"
                                    UserRole.ADMIN -> "e.g., Ustadh Ibrahim"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = com.example.ui.theme.M3EmeraldPrimary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_name_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.example.ui.theme.M3EmeraldPrimary,
                            unfocusedBorderColor = com.example.ui.theme.M3SageOutline,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input: Email / Gmail (Material 3 OutlinedTextField)
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Gmail / Email Address") },
                        placeholder = {
                            Text(
                                when (activeRole) {
                                    UserRole.ADMIN -> "admin@quranacademy.com"
                                    UserRole.TEACHER -> "teacher@gmail.com"
                                    UserRole.STUDENT -> "student@gmail.com"
                                }
                            )
                        },
                        supportingText = {
                            Text(
                                text = "Use your registered academy Google account",
                                fontSize = 11.sp,
                                color = com.example.ui.theme.M3SlateText
                            )
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = com.example.ui.theme.M3EmeraldPrimary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_email_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = com.example.ui.theme.M3EmeraldPrimary,
                            unfocusedBorderColor = com.example.ui.theme.M3SageOutline,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    // Account Already Exists Banner (Material 3 Callout Card)
                    if (detectedExistingAccount != null) {
                        Surface(
                            color = com.example.ui.theme.M3MintContainer,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.M3EmeraldPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = com.example.ui.theme.M3EmeraldPrimary,
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
                                        color = com.example.ui.theme.M3EmeraldDark
                                    )
                                    Text(
                                        text = "Identity: ${detectedExistingAccount.second} (${activeRole.name.lowercase().replaceFirstChar { it.uppercase() }}). Tap below to log in.",
                                        fontSize = 11.sp,
                                        color = com.example.ui.theme.M3EmeraldPrimary
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

                    // Primary Button: High-emphasis Material 3 Filled Button (54dp Stadium Pill)
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
                            .height(54.dp)
                            .testTag("sign_in_and_sync_button"),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.M3EmeraldPrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (detectedExistingAccount != null) {
                                    "Continue to Portal (${activeRole.name.lowercase().replaceFirstChar { it.uppercase() }})"
                                } else {
                                    "Continue to Portal"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = com.example.ui.theme.M3SageBorder)
                        Text(
                            text = "  or continue with  ",
                            fontSize = 11.sp,
                            color = com.example.ui.theme.M3MutedText,
                            fontWeight = FontWeight.Medium
                        )
                        Divider(modifier = Modifier.weight(1f), color = com.example.ui.theme.M3SageBorder)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary: Sign In with Google (M3 Stadium Pill)
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
                            .height(50.dp)
                            .testTag("continue_with_google_button"),
                        shape = RoundedCornerShape(25.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.M3SageBorder),
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
                                text = "Sign In with Google",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = com.example.ui.theme.M3CharcoalText
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Quick Demo Profiles (Material 3 Elevated Card from Stitch design)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = com.example.ui.theme.M3MintSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.M3SageBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.M3EmeraldPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "QUICK DEMO PROFILES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.M3EmeraldPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PresetButton(
                                    title = "Sheikh Abdullah",
                                    sub = "Teacher",
                                    avatarInitial = "SA",
                                    isGreen = true,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.signInWithEmailAndRole("saqib saib", "itskhan7733@gmail.com", UserRole.TEACHER)
                                        onLoginSuccess(UserRole.TEACHER)
                                    }
                                )
                                PresetButton(
                                    title = "Zaid Ahmed",
                                    sub = "Student",
                                    avatarInitial = "ZA",
                                    isGreen = false,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        viewModel.signInWithEmailAndRole("zaidkhan", "mytest5072@gmail.com", UserRole.STUDENT)
                                        onLoginSuccess(UserRole.STUDENT)
                                    }
                                )
                            }
                        }
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
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(25.dp),
        color = if (isSelected) Color(0xFF059669) else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
    avatarInitial: String = "",
    isGreen: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        color = com.example.ui.theme.M3MintSubtle,
        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.M3SageBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isGreen) com.example.ui.theme.M3MintContainer else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitial,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGreen) com.example.ui.theme.M3EmeraldPrimary else com.example.ui.theme.M3CharcoalText
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = com.example.ui.theme.M3CharcoalText,
                    maxLines = 1
                )
                Text(
                    text = sub,
                    fontSize = 10.sp,
                    color = com.example.ui.theme.M3SlateText
                )
            }
        }
    }
}
