package com.laara.textnowcmp.config.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String = "",
    val content: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: String = "sent", // sent, delivered, read
)
