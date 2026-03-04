package com.laara.textnowcmp.features.home.presentation.newChat

sealed interface NewChatAction {
    data class OnSearchQueryChange(val query: String) : NewChatAction
    data class OnContactClick(val userId: String) : NewChatAction
    data class OnInviteClick(val phone: String) : NewChatAction
    data object OnNewGroupClick : NewChatAction
    data object OnBackClick : NewChatAction
}