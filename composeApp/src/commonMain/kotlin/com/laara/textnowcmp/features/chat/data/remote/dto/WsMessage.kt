package com.laara.textnowcmp.features.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class  WsMessage(
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
    // Call-specific fields
    val callId: String? = null,
    val callType: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val sdpData: String? = null,
    val iceCandidate: String? = null,
    val duration: Int? = null,
    val status: String? = null,
    val direction: String? = null,
    val participantId: String? = null,
    val participantName: String? = null,
    val participantPhone: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
) {
    companion object {
        // Chat types
        const val TYPE_DIRECT_MESSAGE = "direct_message"
        const val TYPE_GROUP_MESSAGE = "group_message"
        const val TYPE_ACK = "ack"
        const val TYPE_DELIVERED = "delivered"
        const val TYPE_READ = "read"
        const val TYPE_TYPING = "typing"
        const val TYPE_STOP_TYPING = "stop_typing"
        // Call signaling types
        const val TYPE_CALL_INITIATE = "call_initiate"
        const val TYPE_CALL_RINGING = "call_ringing"
        const val TYPE_CALL_ANSWER = "call_answer"
        const val TYPE_CALL_DECLINE = "call_decline"
        const val TYPE_CALL_END = "call_end"
        const val TYPE_CALL_BUSY = "call_busy"
        const val TYPE_CALL_NO_ANSWER = "call_no_answer"
        const val TYPE_CALL_MISSED = "call_missed"
        const val TYPE_CALL_LOG = "call_log"
        const val TYPE_CALL_SDP_OFFER = "call_sdp_offer"
        const val TYPE_CALL_SDP_ANSWER = "call_sdp_answer"
        const val TYPE_CALL_ICE = "call_ice_candidate"
        // Call media types
        const val CALL_TYPE_VOICE = "voice"
        const val CALL_TYPE_VIDEO = "video"
    }
}
