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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.data.backend.SupabaseConfig
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var showBackendConfigDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.BrandPageBackground)
            .verticalScroll(scrollState)
    ) {
        // 07A. Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Profile",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = com.example.ui.theme.BrandPrimaryText
            )

            // Settings 40x40 touch target
            IconButton(
                onClick = {
                    if (currentUser.role == UserRole.ADMIN) {
                        showBackendConfigDialog = true
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = com.example.ui.theme.BrandPrimaryEmerald,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 07B. Profile Hero Bento
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.BrandDivider),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 84x84 Avatar with 3dp #C5EEDB border
                    Surface(
                        shape = CircleShape,
                        color = com.example.ui.theme.BrandPrimaryEmerald,
                        modifier = Modifier
                            .size(84.dp)
                            .border(3.dp, com.example.ui.theme.BrandMint, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = currentUser.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = currentUser.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.ui.theme.BrandPrimaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentUser.email,
                            fontSize = 10.sp,
                            color = com.example.ui.theme.BrandSecondaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = com.example.ui.theme.BrandSoftGreenSurface,
                            modifier = Modifier.height(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = currentUser.role.name,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.BrandPrimaryEmerald
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(com.example.ui.theme.BrandSuccess, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Active ${currentUser.role.name.lowercase()}",
                                fontSize = 10.sp,
                                color = com.example.ui.theme.BrandSuccess,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 07C. Academy Enrollment Bento
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Academy Enrollment",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = com.example.ui.theme.BrandPrimaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("academy_details_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.BrandDivider),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ProfileItemRow(
                        icon = Icons.Default.School,
                        label = "Assigned Teacher",
                        value = currentUser.assignedTeacherName
                    )
                    ProfileDivider()
                    ProfileItemRow(
                        icon = Icons.Default.MilitaryTech,
                        label = "Tajweed Level",
                        value = currentUser.tajweedLevel
                    )
                    ProfileDivider()
                    ProfileItemRow(
                        icon = Icons.Default.Book,
                        label = "Memorization",
                        value = "${currentUser.completedJuzCount} Juz completed"
                    )
                    ProfileDivider()
                    ProfileItemRow(
                        icon = Icons.Default.CheckCircle,
                        label = "Attendance",
                        value = "${currentUser.attendanceRate} · Excellent"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 07D. Recent Attendance & Grades Bento
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Recent Attendance & Grades",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = com.example.ui.theme.BrandPrimaryText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.BrandDivider),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Surah Al-Mulk Tajweed Rules",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = com.example.ui.theme.BrandPrimaryText
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = com.example.ui.theme.BrandSoftGreenSurface,
                            modifier = Modifier.height(22.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "A+",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.BrandSuccess
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Yesterday · 45 mins By Sheikh Abdullah",
                        fontSize = 10.sp,
                        color = com.example.ui.theme.BrandSecondaryText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = com.example.ui.theme.BrandSoftSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "“Makharij of throat letters improved; practice Qalqalah.”",
                            fontSize = 11.sp,
                            color = com.example.ui.theme.BrandSecondaryText,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // 07E. Admin-Only Backend Card
        if (currentUser.role == UserRole.ADMIN) {
            Spacer(modifier = Modifier.height(18.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBackendConfigDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F1)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.BrandDivider),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Backend Services",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = com.example.ui.theme.BrandPrimaryText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = com.example.ui.theme.BrandSoftGreenSurface
                                ) {
                                    Text(
                                        text = "Active",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = com.example.ui.theme.BrandPrimaryEmerald,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Supabase · LiveKit Cloud",
                                fontSize = 11.sp,
                                color = com.example.ui.theme.BrandSecondaryText
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure",
                            tint = com.example.ui.theme.BrandPrimaryEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // 07F. Logout Action (Outlined Red, Radius 16, Height 48)
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedButton(
                onClick = {
                    viewModel.signOut()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.BrandDanger),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.ui.theme.BrandDanger)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showBackendConfigDialog) {
        val context = LocalContext.current
        var supabaseUrl by remember { mutableStateOf(SupabaseConfig.projectUrl) }
        var supabaseKey by remember { mutableStateOf(SupabaseConfig.anonKey) }
        var liveKitUrl by remember { mutableStateOf(SupabaseConfig.liveKitServerUrl) }
        var liveKitKey by remember { mutableStateOf(SupabaseConfig.liveKitApiKey) }
        var liveKitSecret by remember { mutableStateOf(SupabaseConfig.liveKitApiSecret) }

        val sqlSchema = """
            -- Quran Academy Real-Time Backend Schema
            create table if not exists public.profiles (
              id text primary key,
              name text not null,
              email text not null,
              role text not null,
              avatar_url text default '',
              assigned_teacher_name text default '',
              tajweed_level text default 'Beginner',
              fcm_token text default '',
              updated_at timestamp with time zone default timezone('utc'::text, now())
            );
            create index if not exists idx_profiles_fcm_token on public.profiles(fcm_token);

            create table if not exists public.classes (
              id text primary key,
              title text not null,
              teacher_name text not null,
              teacher_title text default 'Certified Qari',
              student_name text not null,
              date text not null,
              start_time text not null,
              duration_minutes integer default 45,
              status text default 'SCHEDULED',
              description text default '',
              livekit_room_name text not null,
              surah_topic text default '',
              updated_at timestamp with time zone default timezone('utc'::text, now())
            );

            create table if not exists public.active_calls (
              id text primary key,
              class_id text not null,
              teacher_name text not null,
              student_name text not null,
              room_name text not null,
              is_ringing boolean default false,
              updated_at timestamp with time zone default timezone('utc'::text, now())
            );

            alter table public.profiles enable row level security;
            create policy "Allow all profiles" on public.profiles for all using (true) with check (true);

            alter table public.classes enable row level security;
            create policy "Allow all classes" on public.classes for all using (true) with check (true);

            alter table public.active_calls enable row level security;
            create policy "Allow all calls" on public.active_calls for all using (true) with check (true);
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showBackendConfigDialog = false },
            title = {
                Text("Backend & LiveKit Cloud Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Multi-Device Calling Setup:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "When you install the APK on other phones, these settings are saved and used automatically. You do not need to configure other phones individually if they use the same APK.",
                        fontSize = 11.sp,
                        color = Color(0xFF616161),
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = liveKitUrl,
                        onValueChange = { liveKitUrl = it },
                        label = { Text("LiveKit Cloud WebSocket URL") },
                        placeholder = { Text("wss://your-project.livekit.cloud") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = liveKitKey,
                            onValueChange = { liveKitKey = it },
                            label = { Text("LiveKit API Key") },
                            placeholder = { Text("API...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        OutlinedTextField(
                            value = liveKitSecret,
                            onValueChange = { liveKitSecret = it },
                            label = { Text("LiveKit Secret") },
                            placeholder = { Text("Secret...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = { supabaseUrl = it },
                        label = { Text("Supabase URL") },
                        placeholder = { Text("https://xyz.supabase.co") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = supabaseKey,
                        onValueChange = { supabaseKey = it },
                        label = { Text("Supabase Anon Key") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Supabase Schema SQL", sqlSchema)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "SQL Schema copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📋 Copy Supabase SQL Tables Script", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveLiveKitCredentials(liveKitUrl, liveKitKey, liveKitSecret)
                        viewModel.saveSupabaseCredentials(supabaseUrl, supabaseKey)
                        showBackendConfigDialog = false
                        Toast.makeText(context, "Credentials saved & active on this device!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackendConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = com.example.ui.theme.BrandPrimaryEmerald, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = com.example.ui.theme.BrandSecondaryText)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = com.example.ui.theme.BrandPrimaryText)
        }
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(com.example.ui.theme.BrandDivider)
    )
}

