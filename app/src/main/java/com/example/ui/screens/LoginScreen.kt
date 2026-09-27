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

    var isSignUpMode by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var googleSignInNote by remember { mutableStateOf<String?>(null) }

    val trimmedEmail = inputEmail.trim().lowercase()
    val trimmedName = inputName.trim().lowercase()

    // Auto-detect existing accounts
    val matchingAdmin = if (trimmedEmail == "swabi5072@gmail.com" || (trimmedEmail.isNotBlank() && trimmedEmail.contains("admin"))) {
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

    val activeRole = if (!isSignUpMode && detectedExistingAccount != null) {
        detectedExistingAccount.first
    } else if (trimmedEmail == "swabi5072@gmail.com") {
        UserRole.ADMIN
    } else {
        selectedRole
    }

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
                text = if (!isSignUpMode) "Welcome back! Enter your details to log in to your account." else "New to Quran Academy? Sign up to start your recitation journey.",
                fontSize = 13.sp,
                color = com.example.ui.theme.M3SlateText,
                modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                    // M3 Segmented Selector: Log In vs Sign Up
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = com.example.ui.theme.M3MintSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.M3SageBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (!isSignUpMode) com.example.ui.theme.M3EmeraldPrimary else Color.Transparent)
                                    .clickable {
                                        isSignUpMode = false
                                        authErrorMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Log In",
                                    fontSize = 13.sp,
                                    fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isSignUpMode) Color.White else com.example.ui.theme.M3SlateText
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSignUpMode) com.example.ui.theme.M3EmeraldPrimary else Color.Transparent)
                                    .clickable {
                                        isSignUpMode = true
                                        authErrorMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign Up",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSignUpMode) Color.White else com.example.ui.theme.M3SlateText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Material 3 SELECT ROLE Label
                    Text(
                        text = if (!isSignUpMode) "ACCOUNT ROLE" else "CHOOSE YOUR ROLE",
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
                                onClick = {
                                    selectedRole = role
                                    authErrorMessage = null
                                },
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
                        onValueChange = {
                            inputName = it
                            authErrorMessage = null
                        },
                        label = { Text("Full Name") },
                        placeholder = {
                            Text(
                                when (activeRole) {
                                    UserRole.STUDENT -> "e.g., Zaid Ahmed"
                                    UserRole.TEACHER -> "e.g., Sheikh Abdullah"
                                    UserRole.ADMIN -> "e.g., Director Ibrahim"
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
                        onValueChange = {
                            inputEmail = it
                            authErrorMessage = null
                        },
                        label = { Text("Gmail / Email Address") },
                        placeholder = {
                            Text(
                                when (activeRole) {
                                    UserRole.ADMIN -> "swabi5072@gmail.com"
                                    UserRole.TEACHER -> "teacher@gmail.com"
                                    UserRole.STUDENT -> "student@gmail.com"
                                }
                            )
                        },
                        supportingText = {
                            Text(
                                text = if (!isSignUpMode) "Enter your registered academy email" else "Enter a valid email for class notifications",
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

                    // Error Banner / Switch Prompt
                    if (authErrorMessage != null) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = authErrorMessage ?: "",
                                        fontSize = 12.sp,
                                        color = Color(0xFF991B1B),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (!isSignUpMode && (authErrorMessage?.contains("Sign Up", ignoreCase = true) == true)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            isSignUpMode = true
                                            authErrorMessage = null
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.M3EmeraldPrimary)
                                    ) {
                                        Text(
                                            text = "Switch to Sign Up",
                                            color = com.example.ui.theme.M3EmeraldPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (isSignUpMode && (authErrorMessage?.contains("Log In", ignoreCase = true) == true)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            isSignUpMode = false
                                            authErrorMessage = null
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.M3EmeraldPrimary)
                                    ) {
                                        Text(
                                            text = "Switch to Log In",
                                            color = com.example.ui.theme.M3EmeraldPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Account Already Exists Banner in Log In mode
                    if (!isSignUpMode && detectedExistingAccount != null && authErrorMessage == null) {
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
                                        text = "Account recognized!",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.M3EmeraldDark
                                    )
                                    Text(
                                        text = "Welcome back ${detectedExistingAccount.second} (${activeRole.name.lowercase().replaceFirstChar { it.uppercase() }}). Tap below to enter.",
                                        fontSize = 11.sp,
                                        color = com.example.ui.theme.M3EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Button: High-emphasis Material 3 Filled Button (54dp Stadium Pill)
                    Button(
                        onClick = {
                            authErrorMessage = null
                            val cleanName = inputName.trim()
                            val cleanEmail = inputEmail.trim().lowercase()

                            if (cleanName.isBlank()) {
                                authErrorMessage = "Please enter your full name."
                                return@Button
                            }
                            if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                authErrorMessage = "Please enter a valid email address."
                                return@Button
                            }

                            if (!isSignUpMode) {
                                // --- LOG IN FLOW ---
                                val isRegistered = viewModel.isEmailRegistered(cleanEmail) || detectedExistingAccount != null
                                if (!isRegistered) {
                                    authErrorMessage = "No account found with this email. Please switch to Sign Up to create your account."
                                    return@Button
                                }
                                val finalRole = activeRole
                                viewModel.signInWithEmailAndRole(cleanName, cleanEmail, finalRole)
                                onLoginSuccess(finalRole)
                            } else {
                                // --- SIGN UP FLOW ---
                                if (selectedRole == UserRole.ADMIN && cleanEmail != "swabi5072@gmail.com" && !com.example.auth.AuthManager.authorizedAdminEmails.contains(cleanEmail)) {
                                    authErrorMessage = "Administrator accounts cannot be created publicly. Please choose Student or Teacher."
                                    return@Button
                                }
                                val alreadyExists = viewModel.isEmailRegistered(cleanEmail) || detectedExistingAccount != null
                                if (alreadyExists) {
                                    authErrorMessage = "An account with this email already exists. Please switch to Log In."
                                    return@Button
                                }
                                viewModel.registerNewUser(cleanName, cleanEmail, selectedRole)
                                onLoginSuccess(selectedRole)
                            }
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
                                text = if (!isSignUpMode) "Log In to Portal" else "Create Account & Continue",
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode Switcher Link
                    Row(
                        modifier = Modifier
                            .clickable {
                                isSignUpMode = !isSignUpMode
                                authErrorMessage = null
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (!isSignUpMode) "Don't have an academy account? " else "Already have an account? ",
                            fontSize = 12.sp,
                            color = com.example.ui.theme.M3SlateText
                        )
                        Text(
                            text = if (!isSignUpMode) "Sign Up" else "Log In",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.M3EmeraldPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
