package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
                .background(Color(0xFFF8FAFC))
                .testTag("chat_contacts_screen")
        ) {
            // Apple Header
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text(
                        text = when (currentUser.role) {
                            UserRole.TEACHER -> "My Students"
                            UserRole.STUDENT -> "My Quran Teachers"
                            UserRole.ADMIN -> "Academy Messages"
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = when (currentUser.role) {
                            UserRole.TEACHER -> "Select a student to review recitation notes and direct messages"
                            UserRole.STUDENT -> "Chat directly with teachers you have classes with"
                            UserRole.ADMIN -> "Connect directly with teachers and students"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Apple Capsule Search Input
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
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("search_contacts_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9)
                        ),
                        singleLine = true
                    )

                    if (currentUser.role == UserRole.ADMIN) {
                        Spacer(modifier = Modifier.height(10.dp))
                        // Apple Segmented Pill Selector for Admin
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (adminTabSelection == 0) Color.White else Color.Transparent)
                                        .then(if (adminTabSelection == 0) Modifier.shadow(2.dp, RoundedCornerShape(11.dp)) else Modifier)
                                        .clickable { adminTabSelection = 0 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Teachers (${teachers.size})",
                                        fontSize = 12.sp,
                                        fontWeight = if (adminTabSelection == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (adminTabSelection == 0) Color(0xFF059669) else Color(0xFF64748B)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (adminTabSelection == 1) Color.White else Color.Transparent)
                                        .then(if (adminTabSelection == 1) Modifier.shadow(2.dp, RoundedCornerShape(11.dp)) else Modifier)
                                        .clickable { adminTabSelection = 1 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Students (${students.size})",
                                        fontSize = 12.sp,
                                        fontWeight = if (adminTabSelection == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (adminTabSelection == 1) Color(0xFF059669) else Color(0xFF64748B)
                                    )
                                }
                            }
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
                                color = Color(0xFF94A3B8),
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

                        // Apple Inset Grouped Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedContact = contact }
                                .testTag("contact_item_${contact.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                        color = if (contact.role == UserRole.TEACHER) Color(0xFF059669) else Color(0xFF2563EB),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = contact.avatarInitial,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 19.sp
                                            )
                                        }
                                    }

                                    // Online green indicator dot with white border ring
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981),
                                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                                        modifier = Modifier
                                            .size(13.dp)
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
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = lastMsg?.timestamp ?: "Available",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = contact.subtitle,
                                        fontSize = 12.sp,
                                        color = Color(0xFF059669),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = lastMsg?.text ?: "Tap to start conversation",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Chat",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
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
                .background(Color(0xFFF8FAFC))
                .testTag("chat_conversation_screen")
        ) {
            // Apple Chat Top Bar
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedContact = null },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .testTag("back_to_contacts_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to contacts",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box {
                        Surface(
                            shape = CircleShape,
                            color = if (contact.role == UserRole.TEACHER) Color(0xFF059669) else Color(0xFF2563EB),
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

                        // Online green dot
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                            modifier = Modifier
                                .size(11.dp)
                                .align(Alignment.BottomEnd)
                        ) {}
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active now • ${contact.subtitle}",
                                fontSize = 11.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.Medium,
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
                                    color = Color(0xFFECFDF5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Start conversation with ${contact.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Send a direct message or greeting below.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
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

            // Quick Islamic Greeting Chips (Apple Pill Carousel)
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
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable {
                            viewModel.sendMessage(phrase, contact.id, contact.name)
                        }
                    ) {
                        Text(
                            text = phrase,
                            fontSize = 12.sp,
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Apple Pinned Message Input Bar
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
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
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF059669),
                        modifier = Modifier
                            .size(46.dp)
                            .clickable {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText, contact.id, contact.name)
                                    inputText = ""
                                }
                            }
                            .testTag("send_message_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
