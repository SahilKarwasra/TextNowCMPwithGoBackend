package com.laara.textnowcmp.features.home.presentation.newChat

sealed interface NewChatEvent {
    data object NavigateBack : NewChatEvent
    data class NavigateToChat(val userId: String) : NewChatEvent
    data class ShareInviteLink(val phone: String) : NewChatEvent
}