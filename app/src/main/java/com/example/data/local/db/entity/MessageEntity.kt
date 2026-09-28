package com.example.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Message
import com.example.data.model.UserRole

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val receiverId: String,
    val receiverName: String,
    val text: String,
    val timestamp: String,
    val isRead: Boolean,
    val isFromMe: Boolean
) {
    fun toDomain(): Message {
        val role = try {
            UserRole.valueOf(senderRole)
        } catch (_: Exception) {
            UserRole.STUDENT
        }
        return Message(
            id = id,
            senderId = senderId,
            senderName = senderName,
            senderRole = role,
            receiverId = receiverId,
            receiverName = receiverName,
            text = text,
            timestamp = timestamp,
            isRead = isRead,
            isFromMe = isFromMe
        )
    }

    companion object {
        fun fromDomain(m: Message): MessageEntity {
            return MessageEntity(
                id = m.id,
                senderId = m.senderId,
                senderName = m.senderName,
                senderRole = m.senderRole.name,
                receiverId = m.receiverId,
                receiverName = m.receiverName,
                text = m.text,
                timestamp = m.timestamp,
                isRead = m.isRead,
                isFromMe = m.isFromMe
            )
        }
    }
}
