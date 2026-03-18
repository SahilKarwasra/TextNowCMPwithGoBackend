package com.laara.textnowcmp.features.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WsMessage(
    val type: String,
    val messageId: String? = null,
    val conversationId: String? = null,
    val recipientId: String? = null,
    val content: String? = null,
    val senderId: String? = null,
    val senderPhone: String? = null,
    val senderName: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val timestamp: String? = null,
    val groupId: String? = null,
) {
    companion object {
        const val TYPE_DIRECT_MESSAGE = "direct_message"
        const val TYPE_GROUP_MESSAGE = "group_message"
        const val TYPE_ACK = "ack"
        const val TYPE_DELIVERED = "delivered"
        const val TYPE_READ = "read"
        const val TYPE_TYPING = "typing"
        const val TYPE_STOP_TYPING = "stop_typing"
    }
}
