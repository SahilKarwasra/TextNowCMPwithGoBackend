package com.laara.textnowcmp.config.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val conversationId: String,
    val recipientUserId: String,
    val recipientName: String,
    val recipientPhone: String = "",
    val recipientProfilePic: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
)
