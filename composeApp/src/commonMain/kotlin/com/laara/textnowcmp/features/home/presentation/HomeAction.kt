package com.laara.textnowcmp.features.home.presentation

sealed interface HomeAction {
    data class OnConversationClick(val conversationId: String, val recipientName: String) : HomeAction
    data object OnContactsPermissionGranted : HomeAction
}