package com.laara.textnowcmp.features.home.presentation.chat

sealed interface ChatAction {
    data class OnInputChange(val text: String) : ChatAction
    data object OnSendClick : ChatAction
    data object OnBackClick : ChatAction
}