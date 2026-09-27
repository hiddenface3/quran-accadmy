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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.BrandDivider
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

    // When inside a 1-on-1 chat, back button returns to the messages directory
    BackHandler(enabled = selectedContact != null) {
        selectedContact = null
    }

    if (selectedContact == null) {
        // ======================================================================
        // SCREEN 05 — MESSAGES DIRECTORY (1-on-1 Only, No Groups)
        // ======================================================================
        val contacts: List<ChatContact> = remember(currentUser, students, teachers, classes, searchQuery) {
            val list = when (currentUser.role) {
                UserRole.TEACHER -> {
                    students.map { s ->
                        ChatContact(
                            id = s.id,
                            name = s.name,
                            role = UserRole.STUDENT,
                            subtitle = "Student · ${s.tajweedLevel}",
                            levelOrTopic = s.currentSurah
                        )
                    }
                }
                UserRole.STUDENT -> {
                    teachers.map { t ->
                        ChatContact(
                            id = t.id,
                            name = t.name,
                            role = UserRole.TEACHER,
                            subtitle = t.title,
                            levelOrTopic = t.tajweedIjazah
                        )
                    }
                }
                UserRole.ADMIN -> {
                    val teacherContacts = teachers.map { t ->
                        ChatContact(
                            id = t.id,
                            name = t.name,
                            role = UserRole.TEACHER,
                            subtitle = t.title,
                            levelOrTopic = t.tajweedIjazah
                        )
                    }
                    val studentContacts = students.map { s ->
                        ChatContact(
                            id = s.id,
                            name = s.name,
                            role = UserRole.STUDENT,
                            subtitle = "Student · ${s.tajweedLevel}",
                            levelOrTopic = s.currentSurah
                        )
                    }
                    teacherContacts + studentContacts
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
                .background(BrandPageBackground)
                .testTag("chat_contacts_screen")
        ) {
            // 05A. Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Messages",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your conversations",
                        fontSize = 11.sp,
                        color = BrandSecondaryText
                    )
                }

                // 40x40 Touch target search button
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BrandPrimaryEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 05B. Search Bar (Height 44, Radius 22, Background #ECEEE8)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search people",
                            fontSize = 12.sp,
                            color = BrandMutedText
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = BrandSecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("search_contacts_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFFECEEE8),
                        unfocusedContainerColor = Color(0xFFECEEE8)
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 05C. Online Now Bento
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandDivider),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Online now",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(contacts.take(6)) { contact ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedContact = contact }
                            ) {
                                Box(
                                    modifier = Modifier.size(42.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandPrimaryEmerald,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = contact.avatarInitial,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                    // 8x8 Green online dot
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .align(Alignment.BottomEnd)
                                            .background(BrandSurface, CircleShape)
                                            .padding(1.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(BrandSuccess, CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = contact.name.split(" ").firstOrNull() ?: contact.name,
                                    fontSize = 9.sp,
                                    color = BrandPrimaryText,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 05D. Recent Conversations Header
            Text(
                text = "Recent conversations",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimaryText,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 05E. 1-on-1 Contact Rows (Light Bento Rows)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("contacts_lazy_column")
            ) {
                if (contacts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No conversations found.",
                                color = BrandMutedText,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(contacts) { contact ->
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
                                .height(72.dp)
                                .padding(vertical = 4.dp)
                                .clickable { selectedContact = contact }
                                .testTag("contact_item_${contact.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandDivider),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 44x44 Avatar
                                Box(modifier = Modifier.size(44.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandPrimaryEmerald,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = contact.avatarInitial,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .align(Alignment.BottomEnd)
                                            .background(BrandSurface, CircleShape)
                                            .padding(1.5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(BrandSuccess, CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Name, Role Badge, and Preview
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = contact.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = BrandPrimaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = BrandSoftGreenSurface,
                                            modifier = Modifier.height(20.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            ) {
                                                Text(
                                                    text = when (contact.role) {
                                                        UserRole.TEACHER -> "Teacher"
                                                        UserRole.STUDENT -> "Student"
                                                        UserRole.ADMIN -> "Admin"
                                                    },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandPrimaryEmerald
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = lastMsg?.text ?: "Tap to start conversation",
                                        fontSize = 11.sp,
                                        color = BrandSecondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Timestamp & Chevron
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = lastMsg?.timestamp ?: "Now",
                                        fontSize = 10.sp,
                                        color = BrandMutedText
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = BrandMutedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // ======================================================================
        // SCREEN 06 — 1-ON-1 CHAT (Strictly Text-Only, No Microphone, No Paperclip)
        // ======================================================================
        val contact = selectedContact!!
        var inputText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

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
                .background(BrandPageBackground)
                .testTag("chat_conversation_screen")
        ) {
            // 06A. Chat Header
            Surface(
                color = BrandPageBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button (touch 40x40, icon 20x20)
                    IconButton(
                        onClick = { selectedContact = null },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("back_to_contacts_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandPrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 36x36 Avatar
                    Surface(
                        shape = CircleShape,
                        color = BrandPrimaryEmerald,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = contact.avatarInitial,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Contact Name & Active Status
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandPrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(BrandSuccess, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active now",
                                fontSize = 9.sp,
                                color = BrandSuccess,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Call Button (touch 40x40, icon 20x20)
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = BrandPrimaryEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // More Button (touch 40x40, icon 20x20)
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = BrandSecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 06B. Message Feed (LazyColumn auto-scrolling to bottom)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("messages_list")
            ) {
                items(conversationMessages) { msg ->
                    MessageBubble(message = msg)
                }
            }

            // 06E. Message Composer (Outer radius 26, background white, border 1dp #E2E5E0)
            // NO Microphone, NO Paperclip, NO Attachments! Only [ Text field ] [ Send ]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = BrandSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E5E0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Type a message...",
                                    fontSize = 12.sp,
                                    color = BrandMutedText
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // 44x44 Circular Send Button
                        Surface(
                            shape = CircleShape,
                            color = BrandPrimaryEmerald,
                            modifier = Modifier
                                .size(40.dp)
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
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
