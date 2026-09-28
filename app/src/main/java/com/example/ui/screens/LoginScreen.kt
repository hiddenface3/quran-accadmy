package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.theme.BrandDarkEmerald
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandMint
import com.example.ui.theme.BrandMutedText
import com.example.ui.theme.BrandPageBackground
import com.example.ui.theme.BrandPrimaryEmerald
import com.example.ui.theme.BrandPrimaryText
import com.example.ui.theme.BrandSecondaryText
import com.example.ui.theme.BrandSoftGreenSurface
import com.example.ui.theme.BrandSoftSurface
import com.example.ui.theme.BrandSurface

/**
 * Every account here is a real Supabase Auth user - role is never chosen on this screen.
 * Sign-up always provisions a STUDENT profile server-side (see the handle_new_user trigger in
 * supabase/migrations); a TEACHER/ADMIN account can only be granted by an existing admin.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val authBusy by viewModel.authBusy.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var isSignUpMode by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var googleSignInNote by remember { mutableStateOf<String?>(null) }

    fun submit() {
        authErrorMessage = null
        infoMessage = null
        val cleanName = inputName.trim()
        val cleanEmail = inputEmail.trim().lowercase()
        val cleanPassword = inputPassword

        if (isSignUpMode && cleanName.isBlank()) {
            authErrorMessage = "Please enter your full name."
            return
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            authErrorMessage = "Please enter a valid email address."
            return
        }
        if (cleanPassword.length < 8) {
            authErrorMessage = "Password must be at least 8 characters."
            return
        }

        if (isSignUpMode) {
            viewModel.signUp(cleanName, cleanEmail, cleanPassword) { result ->
                result.onSuccess {
                    onLoginSuccess(UserRole.STUDENT)
                }.onFailure { e ->
                    val msg = e.localizedMessage ?: "Could not create account."
                    if (msg.contains("check your email", ignoreCase = true)) {
                        infoMessage = msg
                        isSignUpMode = false
                    } else {
                        authErrorMessage = msg
                    }
                }
            }
        } else {
            viewModel.signIn(cleanEmail, cleanPassword) { result ->
                result.onSuccess {
                    onLoginSuccess(viewModel.currentUser.value.role)
                }.onFailure { e ->
                    authErrorMessage = e.localizedMessage ?: "Invalid email or password."
                }
            }
        }
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
            // BRANDING HERO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Brush.verticalGradient(colors = listOf(BrandDarkEmerald, BrandPrimaryEmerald))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
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

                    Text(
                        text = "Quran Academy Connect",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Online Tajweed & Hifz Live Classroom Portal",
                        fontSize = 12.sp,
                        color = BrandMint,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // LOGIN CONTENT SHEET
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
                                        infoMessage = null
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
                                        infoMessage = null
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

                    if (isSignUpMode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BrandSoftGreenSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "New accounts start as Student. Teacher or Admin access is granted by an academy administrator after sign-up.",
                                fontSize = 12.sp,
                                color = BrandDarkEmerald,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSignUpMode) {
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
                            colors = fieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("user_name_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("user_email_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = {
                            inputPassword = it
                            authErrorMessage = null
                        },
                        placeholder = { Text("Password", fontSize = 13.sp, color = BrandMutedText) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = BrandPrimaryEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("user_password_input")
                    )

                    if (authErrorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MessageBanner(authErrorMessage!!, isError = true)
                    }
                    if (infoMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MessageBanner(infoMessage!!, isError = false)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { submit() },
                        enabled = !authBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("sign_in_and_sync_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimaryEmerald)
                    ) {
                        if (authBusy) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (!isSignUpMode) "Log In" else "Create Account",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.signInWithGoogle(
                                onSuccess = { onLoginSuccess(viewModel.currentUser.value.role) },
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Surface(shape = CircleShape, color = Color(0xFF4285F4), modifier = Modifier.size(20.dp)) {
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // FEATURE CHIPS
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
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Text(text = feature, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = BrandPrimaryEmerald)
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

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandPrimaryEmerald,
    unfocusedBorderColor = Color(0xFFE2E5E0),
    focusedContainerColor = Color(0xFFF7F7F3),
    unfocusedContainerColor = Color(0xFFF7F7F3),
    focusedTextColor = BrandPrimaryText,
    unfocusedTextColor = BrandPrimaryText
)

@Composable
private fun MessageBanner(message: String, isError: Boolean) {
    val bg = if (isError) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
    val border = if (isError) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
    val iconTint = if (isError) Color(0xFFDC2626) else Color(0xFF16A34A)
    val textColor = if (isError) Color(0xFF991B1B) else Color(0xFF166534)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = message, fontSize = 12.sp, color = textColor)
        }
    }
}
