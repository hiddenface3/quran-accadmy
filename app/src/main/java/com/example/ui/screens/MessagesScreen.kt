package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.MessageBubble
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldSecondary

data class ChatContact(
    val id: String,
    val name: String,
    val role: UserRole,
    val subtitle: String,
    val levelOrTopic: String,
    val avatarInitial: String = name.take(1).uppercase(),
    val isOnline: Boolean = true
)

@Composable
fun MessagesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allMessages by viewModel.messages.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val teachers by viewModel.teachers.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()

    var selectedContact by remember { mutableStateOf<ChatContact?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var adminTabSelection by remember { mutableIntStateOf(0) }

    // When inside a chat conversation with a selected contact, pressing back returns to contact list
    BackHandler(enabled = selectedContact != null) {
        selectedContact = null
    }

    if (selectedContact == null) {
        // --- 1. DIRECTORY LIST VIEW (Teachers for Student / Students for Teacher) ---
        val contacts: List<ChatContact> = remember(currentUser, students, teachers, classes, adminTabSelection, searchQuery) {
            val list = when (currentUser.role) {
                UserRole.TEACHER -> {
                    // For a teacher, list students who have classes with this teacher or are assigned
                    val teacherName = currentUser.name
                    val studentNamesFromClasses = classes
                        .filter { it.teacherName.equals(teacherName, ignoreCase = true) }
                        .map { it.studentName }
                        .toSet()

                    students.map { s ->
                        val isEnrolled = studentNamesFromClasses.contains(s.name) ||
                                s.assignedTeacherName.equals(teacherName, ignoreCase = true)
                        ChatContact(
                            id = s.id,
                            name = s.name,
                            role = UserRole.STUDENT,
                            subtitle = if (isEnrolled) "Enrolled Student • ${s.currentSurah}" else "Student • ${s.tajweedLevel}",
                            levelOrTopic = s.currentSurah
                        )
                    }
                }
                UserRole.STUDENT -> {
                    // For a student, list teachers they have classes with or who teach at the academy
                    val studentName = currentUser.name
                    val teachersFromMyClasses = classes
                        .filter { it.studentName.equals(studentName, ignoreCase = true) }
                        .map { it.teacherName }
                        .toSet()

                    teachers.map { t ->
                        val hasClassWith = teachersFromMyClasses.contains(t.name) ||
                                currentUser.assignedTeacherName.equals(t.name, ignoreCase = true)
                        ChatContact(
                            id = t.id,
                            name = t.name,
                            role = UserRole.TEACHER,
                            subtitle = if (hasClassWith) "My Quran Teacher • ${t.title}" else t.title,
                            levelOrTopic = t.tajweedIjazah
                        )
                    }
                }
                UserRole.ADMIN -> {
                    if (adminTabSelection == 0) {
                        teachers.map { t ->
                            ChatContact(
                                id = t.id,
                                name = t.name,
                                role = UserRole.TEACHER,
                                subtitle = t.title,
                                levelOrTopic = t.tajweedIjazah
                            )
                        }
                    } else {
                        students.map { s ->
                            ChatContact(
                                id = s.id,
                                name = s.name,
                                role = UserRole.STUDENT,
                                subtitle = "Student • ${s.tajweedLevel}",
                                levelOrTopic = s.currentSurah
                            )
                        }
                    }
                }
            }

            if (searchQuery.isBlank()) {
                list
            } else {
                list.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.subtitle.contains(searchQuery, ignoreCase = true) ||
                            it.levelOrTopic.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFAF9F5))
                .testTag("chat_contacts_screen")
        ) {
            // Header
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (currentUser.role) {
                            UserRole.TEACHER -> "My Students"
                            UserRole.STUDENT -> "My Quran Teachers"
                            UserRole.ADMIN -> "Academy Messages"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1B)
                    )
                    Text(
                        text = when (currentUser.role) {
                            UserRole.TEACHER -> "Select a student to review recitation notes and direct messages"
                            UserRole.STUDENT -> "Chat directly with teachers you have classes with"
                            UserRole.ADMIN -> "Connect directly with teachers and students"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF616161)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = when (currentUser.role) {
                                    UserRole.TEACHER -> "Search students..."
                                    UserRole.STUDENT -> "Search teachers..."
                                    UserRole.ADMIN -> "Search contacts..."
                                },
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("search_contacts_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color(0xFFFBFBF8),
                            unfocusedContainerColor = Color(0xFFFBFBF8)
                        ),
                        singleLine = true
                    )

                    if (currentUser.role == UserRole.ADMIN) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TabRow(
                            selectedTabIndex = adminTabSelection,
                            containerColor = Color.White,
                            contentColor = EmeraldPrimary
                        ) {
                            Tab(
                                selected = adminTabSelection == 0,
                                onClick = { adminTabSelection = 0 },
                                text = { Text("Teachers (${teachers.size})") }
                            )
                            Tab(
                                selected = adminTabSelection == 1,
                                onClick = { adminTabSelection = 1 },
                                text = { Text("Students (${students.size})") }
                            )
                        }
                    }
                }
            }

            // Contacts List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("contacts_lazy_column")
            ) {
                if (contacts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No contacts found.",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(contacts) { contact ->
                        // Calculate last message preview for this contact
                        val lastMsg = allMessages.lastOrNull { msg ->
                            val currentUserName = currentUser.name.trim()
                            val contactName = contact.name.trim()
                            val sender = msg.senderName.trim()
                            val receiver = msg.receiverName.trim()

                            val isFromContact = sender.equals(contactName, ignoreCase = true) &&
                                    (receiver.isEmpty() || receiver.equals(currentUserName, ignoreCase = true) || msg.receiverId == currentUser.id)
                            val isToContact = sender.equals(currentUserName, ignoreCase = true) &&
                                    (receiver.isEmpty() || receiver.equals(contactName, ignoreCase = true) || msg.receiverId == contact.id)
                            val idMatch = (msg.senderId == contact.id && (msg.receiverId == currentUser.id || msg.receiverId.isBlank())) ||
                                    (msg.senderId == currentUser.id && msg.receiverId == contact.id)

                            isFromContact || isToContact || idMatch
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable { selectedContact = contact }
                                .testTag("contact_item_${contact.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Contact Avatar
                                Box {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (contact.role == UserRole.TEACHER) EmeraldPrimary else Color(0xFF1976D2),
                                        modifier = Modifier.size(50.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = contact.avatarInitial,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }

                                    // Online green indicator
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier
                                            .size(12.dp)
                                            .align(Alignment.BottomEnd)
                                    ) {}
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = contact.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF191C1B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = lastMsg?.timestamp ?: "Available",
                                            fontSize = 11.sp,
                                            color = Color(0xFF888888)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = contact.subtitle,
                                        fontSize = 12.sp,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = lastMsg?.text ?: "Tap to start conversation",
                                        fontSize = 12.sp,
                                        color = Color(0xFF616161),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Chat",
                                    tint = Color(0xFFB0BEC5),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- 2. CHAT CONVERSATION VIEW (One-on-One Chat with Selected Contact) ---
        val contact = selectedContact!!
        var inputText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        // Filter messages for this specific conversation
        val conversationMessages = remember(allMessages, contact, currentUser) {
            allMessages.filter { msg ->
                val currentUserName = currentUser.name.trim()
                val contactName = contact.name.trim()
                val sender = msg.senderName.trim()
                val receiver = msg.receiverName.trim()

                val isFromContactToMe = sender.equals(contactName, ignoreCase = true) &&
                        (receiver.isEmpty() || receiver.equals(currentUserName, ignoreCase = true) || msg.receiverId == currentUser.id)
                val isFromMeToContact = sender.equals(currentUserName, ignoreCase = true) &&
                        (receiver.isEmpty() || receiver.equals(contactName, ignoreCase = true) || msg.receiverId == contact.id)
                val idMatch = (msg.senderId == contact.id && (msg.receiverId == currentUser.id || msg.receiverId.isBlank())) ||
                        (msg.senderId == currentUser.id && msg.receiverId == contact.id)

                isFromContactToMe || isFromMeToContact || idMatch
            }
        }

        LaunchedEffect(conversationMessages.size) {
            if (conversationMessages.isNotEmpty()) {
                listState.animateScrollToItem(conversationMessages.size - 1)
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFAF9F5))
                .testTag("chat_conversation_screen")
        ) {
            // Chat Top Bar with Back Button to return to contact list
            Surface(
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedContact = null },
                        modifier = Modifier.testTag("back_to_contacts_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to contacts",
                            tint = Color(0xFF191C1B)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (contact.role == UserRole.TEACHER) EmeraldPrimary else Color(0xFF1976D2),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = contact.avatarInitial,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(8.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Online • ${contact.subtitle}",
                                fontSize = 11.sp,
                                color = Color(0xFF616161),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("messages_list")
            ) {
                if (conversationMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldPrimary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Start conversation with ${contact.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF333333)
                                )
                                Text(
                                    text = "Send a message or greeting below.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    items(conversationMessages) { msg ->
                        MessageBubble(message = msg)
                    }
                }
            }

            // Quick Islamic Greeting Chips
            val quickPhrases = if (currentUser.role == UserRole.TEACHER) {
                listOf(
                    "Assalamu Alaikum",
                    "Please review Ayahs 1 to 15",
                    "Ready for your live recitation test",
                    "Masha'Allah, excellent Tajweed today!",
                    "Please practice Qalqalah rules"
                )
            } else {
                listOf(
                    "Assalamu Alaikum wa Rahmatullah",
                    "Wa Alaikum Assalam Sheikh",
                    "JazakAllah Khair",
                    "I have reviewed today's Ayahs",
                    "Ready for the live lesson"
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPhrases) { phrase ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1EFEA),
                        modifier = Modifier.clickable {
                            viewModel.sendMessage(phrase, contact.id, contact.name)
                        }
                    ) {
                        Text(
                            text = phrase,
                            fontSize = 12.sp,
                            color = Color(0xFF0E5B44),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Message Input Field
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Message ${contact.name.split(" ").first()}...",
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color(0xFFFAF9F5),
                            unfocusedContainerColor = Color(0xFFFAF9F5)
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText, contact.id, contact.name)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(EmeraldPrimary, CircleShape)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
