package com.laara.textnowcmp.features.home.presentation.chat

import com.laara.textnowcmp.config.database.entity.MessageEntity

data class ChatState(
    val conversationId: String = "",
    val recipientName: String = "",
    val messages: List<MessageEntity> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val isLoading: Boolean = true,
)