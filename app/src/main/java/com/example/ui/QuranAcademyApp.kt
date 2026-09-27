package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.togetherWith
import com.example.auth.AuthState
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.outlined.AdminPanelSettings
import com.example.data.model.UserRole
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.ClassesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncomingCallOverlay
import com.example.ui.screens.LiveClassroomScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.M3EmeraldPrimary
import com.example.ui.theme.M3MintSelected
import com.example.ui.theme.M3SlateText
import com.example.ui.theme.M3SurfaceWhite

enum class AcademyTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    CLASSES("Classes", Icons.Filled.Book, Icons.Outlined.Book),
    ADMIN("Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings),
    MESSAGES("Messages", Icons.Filled.Chat, Icons.Outlined.Chat),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun QuranAcademyApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val activeLiveClass by viewModel.activeLiveClass.collectAsStateWithLifecycle()
    val incomingCallClass by viewModel.incomingCallClass.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()

    var currentTab by remember {
        mutableStateOf(
            when (currentUser.role) {
                UserRole.ADMIN -> AcademyTab.ADMIN
                UserRole.TEACHER -> AcademyTab.CLASSES
                UserRole.STUDENT -> AcademyTab.HOME
            }
        )
    }
    val unreadMessageCount = messages.count { !it.isRead && !it.isFromMe }

    val visibleTabs = if (currentUser.role == UserRole.ADMIN) {
        listOf(AcademyTab.HOME, AcademyTab.CLASSES, AcademyTab.ADMIN, AcademyTab.MESSAGES, AcademyTab.PROFILE)
    } else {
        listOf(AcademyTab.HOME, AcademyTab.CLASSES, AcademyTab.MESSAGES, AcademyTab.PROFILE)
    }

    androidx.compose.animation.AnimatedContent(
        targetState = Triple(incomingCallClass, authState, activeLiveClass),
        transitionSpec = {
            androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) togetherWith
                    androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400))
        },
        label = "AppStageTransition"
    ) { (callClass, auth, liveClass) ->
        when {
            // Incoming Call Ringing Overlay Takes Full Precedence
            callClass != null -> {
                IncomingCallOverlay(
                    quranClass = callClass,
                    onAccept = {
                        viewModel.acceptIncomingCall(callClass)
                    },
                    onDecline = {
                        viewModel.dismissIncomingCall()
                    }
                )
            }

            // Unauthenticated -> Show Login Screen
            auth is AuthState.Unauthenticated -> {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { role ->
                        currentTab = when (role) {
                            UserRole.ADMIN -> AcademyTab.ADMIN
                            UserRole.TEACHER -> AcademyTab.CLASSES
                            UserRole.STUDENT -> AcademyTab.HOME
                        }
                    }
                )
            }

            // Active Live Quran Classroom Session -> Immersive Fullscreen Video Call
            liveClass != null -> {
                BackHandler {
                    // Back handler leaves class cleanly
                    viewModel.leaveClass()
                }
                LiveClassroomScreen(
                    quranClass = liveClass,
                    viewModel = viewModel,
                    onLeaveClass = {
                        viewModel.leaveClass()
                    }
                )
            }

            // Authenticated Main Academy Experience with Navigation
            else -> {
                BackHandler(enabled = currentTab != AcademyTab.HOME) {
                    currentTab = AcademyTab.HOME
                }

                Scaffold(
                    modifier = modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = M3SurfaceWhite,
                            contentColor = M3EmeraldPrimary,
                            modifier = Modifier.testTag("academy_bottom_navigation")
                        ) {
                            visibleTabs.forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        if (tab == AcademyTab.MESSAGES && unreadMessageCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(containerColor = GoldSecondary) {
                                                        Text("$unreadMessageCount", color = Color.White)
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.title
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = M3EmeraldPrimary,
                                        selectedTextColor = M3EmeraldPrimary,
                                        indicatorColor = M3MintSelected,
                                        unselectedIconColor = M3SlateText,
                                        unselectedTextColor = M3SlateText
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        androidx.compose.animation.AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                                        androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
                            },
                            label = "TabTransition"
                        ) { tab ->
                            when (tab) {
                                AcademyTab.HOME -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onNavigateToLiveClass = { quranClass ->
                                            viewModel.joinClass(quranClass)
                                        },
                                        onNavigateToClasses = { currentTab = AcademyTab.CLASSES },
                                        onNavigateToMessages = { currentTab = AcademyTab.MESSAGES }
                                    )
                                }
                                AcademyTab.CLASSES -> {
                                    ClassesScreen(
                                        viewModel = viewModel,
                                        onNavigateToLiveClass = { quranClass ->
                                            viewModel.joinClass(quranClass)
                                        }
                                    )
                                }
                                AcademyTab.ADMIN -> {
                                    AdminScreen(
                                        viewModel = viewModel
                                    )
                                }
                                AcademyTab.MESSAGES -> {
                                    MessagesScreen(
                                        viewModel = viewModel
                                    )
                                }
                                AcademyTab.PROFILE -> {
                                    ProfileScreen(
                                        viewModel = viewModel,
                                        onLogout = {
                                            currentTab = AcademyTab.HOME
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
