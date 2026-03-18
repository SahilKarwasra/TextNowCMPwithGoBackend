package com.laara.textnowcmp.features.chat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateConversationRequest(
    val userId: String? = null,
    val phone: String? = null,
)

// Response from POST /conversations (data field)
@Serializable
data class CreateConversationResponse(
    val conversationId: String = "",
    val isNew: Boolean = false,
    val participant: ParticipantDto = ParticipantDto(),
    val createdAt: String = "",
)

// Response from GET /conversations (data field)
@Serializable
data class ListConversationsResponse(
    val conversations: List<ConversationDto> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class ConversationDto(
    val conversationId: String = "",
    val participant: ParticipantDto = ParticipantDto(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class ParticipantDto(
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val profilePic: String = "",
)
