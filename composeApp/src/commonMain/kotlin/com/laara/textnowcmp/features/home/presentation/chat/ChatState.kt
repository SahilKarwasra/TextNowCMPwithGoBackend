package com.laara.textnowcmp.features.home.presentation.chat

import com.laara.textnowcmp.config.database.entity.MessageEntity

data class ChatState(
    val conversationId: String = "",
    val recipientName: String = "",
    val messages: List<MessageEntity> = emptyList(),
    val inputText: String = "",
    val isTyping: Boolean = false,
    val isLoading: Boolean = true,
    val callScreenState: CallScreenState = CallScreenState.Idle,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOn: Boolean = true,
    /** Non-null when waiting for permissions before initiating a call. */
    val pendingCallType: String? = null,
    /** True once the remote peer's video track arrives. Used to trigger recomposition. */
    val remoteVideoReady: Boolean = false,
)

sealed interface CallScreenState {
    data object Idle : CallScreenState

    data class Incoming(
        val callId: String,
        val callType: String, // "voice" | "video"
        val callerName: String,
        val callerPhone: String,
    ) : CallScreenState

    data class Outgoing(
        val callId: String, // empty until server sends call_ringing
        val callType: String,
        val recipientName: String,
    ) : CallScreenState

    data class Active(
        val callId: String,
        val callType: String,
        val recipientName: String,
        val durationSeconds: Int = 0,
    ) : CallScreenState
}
