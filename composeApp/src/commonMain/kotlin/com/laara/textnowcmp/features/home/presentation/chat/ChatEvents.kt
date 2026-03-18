package com.laara.textnowcmp.features.home.presentation.chat

sealed interface ChatEvent {
    data object NavigateBack : ChatEvent
}