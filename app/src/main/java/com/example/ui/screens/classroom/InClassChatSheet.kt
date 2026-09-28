package com.example.ui.screens.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.QuranClass
import com.example.data.model.UserRole
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun InClassChatSheet(
    quranClass: QuranClass,
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var textInput by remember { mutableStateOf("") }

    val isTeacher = currentUser.role == UserRole.TEACHER
    val recipientName = if (isTeacher) quranClass.studentName else quranClass.teacherName

    val classMessages = remember(messages, currentUser, quranClass) {
        messages.filter { msg ->
            val sender = msg.senderName.trim()
            val receiver = msg.receiverName.trim()
            val me = currentUser.name.trim()
            val other = recipientName.trim()
            (sender.equals(me, ignoreCase = true) && (receiver.isEmpty() || receiver.equals(other, ignoreCase = true))) ||
                    (sender.equals(other, ignoreCase = true) && (receiver.isEmpty() || receiver.equals(me, ignoreCase = true)))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .padding(16.dp)
            .testTag("in_class_chat_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "In-Class Live Chat", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Chatting with $recipientName", fontSize = 11.sp, color = EmeraldPrimary)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (classMessages.isEmpty()) {
                    item {
                        Text(
                            text = "No messages yet in this session. Send a note or greeting below.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(classMessages.takeLast(8)) { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (msg.isFromMe) EmeraldPrimary.copy(alpha = 0.15f) else Color(0xFFF0F0F0)
                            ) {
                                Text(
                                    text = "${if (msg.isFromMe) "You" else msg.senderName}: ${msg.text}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF191C1B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Message $recipientName...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendMessage(textInput, null, recipientName)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(EmeraldPrimary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
