package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.theme.BrandDarkEmerald
import com.example.ui.theme.BrandDarkGold
import com.example.ui.theme.BrandDivider
import com.example.ui.theme.BrandGold
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

@OptIn(ExperimentalLayoutApi::class)
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

    // Smart identity recognition
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
            .background(BrandPageBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 01B. BRANDING HERO (Subtle vertical emerald gradient: #09392B -> #0E5B44)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BrandDarkEmerald, BrandPrimaryEmerald)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    // 01C. LOGO (88x88dp, circle, #FFFFFF, border: 2dp #FFD54F, icon: 38x38 #0E5B44)
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(2.dp, BrandGold),
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Quran Academy Mark",
                                tint = BrandPrimaryEmerald,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 01D. APP NAME (24sp Bold, white)
                    Text(
                        text = "Quran Academy Connect",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 01E. SUBTITLE (12sp, #C5EEDB)
                    Text(
                        text = "Online Tajweed & Hifz Live Classroom Portal",
                        fontSize = 12.sp,
                        color = BrandMint,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 01F. LOGIN CONTENT SHEET (Top corners 28dp radius, white sheet)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = BrandSurface,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Log In vs Sign Up Segmented Capsule
                    Surface(
                        shape = RoundedCornerShape(21.dp),
                        color = BrandSoftSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (!isSignUpMode) BrandPrimaryEmerald else Color.Transparent)
                                    .clickable {
                                        isSignUpMode = false
                                        authErrorMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Log In",
                                    fontSize = 13.sp,
                                    fontWeight = if (!isSignUpMode) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (!isSignUpMode) Color.White else BrandSecondaryText
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSignUpMode) BrandPrimaryEmerald else Color.Transparent)
                                    .clickable {
                                        isSignUpMode = true
                                        authErrorMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign Up",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSignUpMode) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSignUpMode) Color.White else BrandSecondaryText
                                )
                            }
                        }
                    }

                    // 01G. SMART IDENTITY CARD (Displayed when user recognized)
                    if (!isSignUpMode && detectedExistingAccount != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BrandSoftGreenSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandSuccess,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Recognized as ${detectedExistingAccount.first.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandDarkEmerald
                                    )
                                    Text(
                                        text = detectedExistingAccount.second,
                                        fontSize = 12.sp,
                                        color = BrandSecondaryText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 01H. ROLE PILLS (Student / Teacher / Admin, 42dp high)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val roles = listOf(
                            Triple(UserRole.STUDENT, "Student", Icons.Default.School),
                            Triple(UserRole.TEACHER, "Teacher", Icons.Default.Person),
                            Triple(UserRole.ADMIN, "Admin", Icons.Default.Security)
                        )

                        roles.forEach { (role, label, icon) ->
                            val isSelected = activeRole == role
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(21.dp))
                                    .background(if (isSelected) BrandPrimaryEmerald else BrandSoftSurface)
                                    .clickable {
                                        selectedRole = role
                                        authErrorMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else BrandSecondaryText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else BrandSecondaryText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 01I. FULL NAME (52dp, radius: 16dp, background: #F7F7F3, leading person icon 20x20)
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = {
                            inputName = it
                            authErrorMessage = null
                        },
                        placeholder = { Text("Full Name", fontSize = 13.sp, color = BrandMutedText) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = BrandPrimaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimaryEmerald,
                            unfocusedBorderColor = Color(0xFFE2E5E0),
                            focusedContainerColor = Color(0xFFF7F7F3),
                            unfocusedContainerColor = Color(0xFFF7F7F3),
                            focusedTextColor = BrandPrimaryText,
                            unfocusedTextColor = BrandPrimaryText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("user_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 01J. EMAIL (52dp, radius: 16dp, background: #F7F7F3, leading email icon 20x20)
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = {
                            inputEmail = it
                            authErrorMessage = null
                        },
                        placeholder = { Text("Email address", fontSize = 13.sp, color = BrandMutedText) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = BrandPrimaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimaryEmerald,
                            unfocusedBorderColor = Color(0xFFE2E5E0),
                            focusedContainerColor = Color(0xFFF7F7F3),
                            unfocusedContainerColor = Color(0xFFF7F7F3),
                            focusedTextColor = BrandPrimaryText,
                            unfocusedTextColor = BrandPrimaryText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("user_email_input")
                    )

                    // Error Message
                    if (authErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = authErrorMessage ?: "",
                                    fontSize = 12.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 01K. CONTINUE BUTTON (52dp, radius: 16dp, background: #0E5B44, text: Continue to Portal, 14sp SemiBold white)
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
                                val isRegisteredEmail = viewModel.isEmailRegistered(cleanEmail)
                                if (!isRegisteredEmail) {
                                    authErrorMessage = "No account found with this email. Switch to Sign Up."
                                    return@Button
                                }
                                val isAdmin = com.example.auth.AuthManager.authorizedAdminEmails.contains(cleanEmail) || cleanEmail == "swabi5072@gmail.com"
                                val exactMatchStudent = students.any { it.email.trim().lowercase() == cleanEmail && it.name.trim().lowercase() == cleanName }
                                val exactMatchTeacher = teachers.any { it.email.trim().lowercase() == cleanEmail && it.name.trim().lowercase() == cleanName }

                                if (!isAdmin && !exactMatchStudent && !exactMatchTeacher) {
                                    authErrorMessage = "Name does not match registered profile for this email."
                                    return@Button
                                }

                                val finalRole = if (isAdmin) UserRole.ADMIN else if (exactMatchTeacher) UserRole.TEACHER else UserRole.STUDENT
                                viewModel.signInWithEmailAndRole(cleanName, cleanEmail, finalRole)
                                onLoginSuccess(finalRole)
                            } else {
                                if (selectedRole == UserRole.ADMIN && cleanEmail != "swabi5072@gmail.com" && !com.example.auth.AuthManager.authorizedAdminEmails.contains(cleanEmail)) {
                                    authErrorMessage = "Administrator accounts cannot be created publicly."
                                    return@Button
                                }
                                val alreadyExists = viewModel.isEmailRegistered(cleanEmail) || detectedExistingAccount != null
                                if (alreadyExists) {
                                    authErrorMessage = "Account already exists. Please switch to Log In."
                                    return@Button
                                }
                                viewModel.registerNewUser(cleanName, cleanEmail, selectedRole)
                                onLoginSuccess(selectedRole)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("sign_in_and_sync_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryEmerald)
                    ) {
                        Text(
                            text = if (!isSignUpMode) "Continue to Portal" else "Create Account & Continue",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 01L. GOOGLE (52dp, radius: 16dp, background: white, border: 1dp #E1E2DC, text: Continue with Google)
                    OutlinedButton(
                        onClick = {
                            viewModel.signInWithGoogle(
                                onSuccess = {
                                    onLoginSuccess(viewModel.currentUser.value.role)
                                },
                                onFailure = { errMsg ->
                                    googleSignInNote = "Google sign-in note: $errMsg. You can use direct email sign-in above."
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("continue_with_google_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1E2DC)),
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
                                text = "Continue with Google",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPrimaryText
                            )
                        }
                    }

                    googleSignInNote?.let { note ->
                        Text(
                            text = note,
                            fontSize = 11.sp,
                            color = BrandMutedText,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 01M. DEMO PRESETS (Compact bento card, radius 18dp, title: Demo Profiles, subtitle: Quick test accounts)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandSoftSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Demo Profiles",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPrimaryText
                            )
                            Text(
                                text = "Quick test accounts",
                                fontSize = 11.sp,
                                color = BrandSecondaryText,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            val demoProfiles = listOf(
                                Triple("Sheikh Abdullah (Teacher)", "teacher@gmail.com", UserRole.TEACHER),
                                Triple("Ayesha Khan (Student)", "student@gmail.com", UserRole.STUDENT),
                                Triple("Director Ibrahim (Admin)", "swabi5072@gmail.com", UserRole.ADMIN)
                            )

                            demoProfiles.forEach { (name, email, role) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            inputName = name.substringBefore(" (")
                                            inputEmail = email
                                            selectedRole = role
                                            isSignUpMode = false
                                            authErrorMessage = null
                                        }
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandPrimaryText)
                                        Text(text = email, fontSize = 10.sp, color = BrandSecondaryText)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BrandSoftGreenSurface
                                    ) {
                                        Text(
                                            text = "Fill",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BrandPrimaryEmerald,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 01N. FEATURE CHIPS (Height: 30dp, radius: 15dp, background: #EAF4EE, text: #0E5B44)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val features = listOf(
                            "1-on-1 HD Live Video",
                            "Interactive Quran Board",
                            "Realtime Attendance"
                        )
                        features.forEach { feature ->
                            Surface(
                                shape = RoundedCornerShape(15.dp),
                                color = BrandSoftGreenSurface,
                                modifier = Modifier.height(30.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = feature,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BrandPrimaryEmerald
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
