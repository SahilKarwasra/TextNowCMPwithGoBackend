package com.laara.textnowcmp.features.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laara.textnowcmp.core.shared.ContactsReader
import com.laara.textnowcmp.core.util.TokenProvider
import com.laara.textnowcmp.config.network.onError
import com.laara.textnowcmp.config.network.onSuccess
import com.laara.textnowcmp.features.chat.data.remote.dto.WsMessage
import com.laara.textnowcmp.features.chat.domain.repository.ChatRepository
import com.laara.textnowcmp.features.contacts.domain.repository.ContactsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val contactsRepository: ContactsRepository,
    private val contactsReader: ContactsReader,
    private val tokenProvider: TokenProvider,
) : ViewModel() {

    private var hasLoadedInitialData = false
    private var hasRequestedContactsSync = false

    private val myUserId: String by lazy {
        tokenProvider.getAccessToken()?.let { token ->
            try {
                val parts = token.split(".")
                if (parts.size >= 2) {
                    val payload = parts[1]
                    val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
                    val decoded = kotlin.io.encoding.Base64.decode(padded)
                    val json = decoded.decodeToString()
                    val regex = """"(?:userId|sub)"\s*:\s*"([^"]+)"""".toRegex()
                    regex.find(json)?.groupValues?.get(1) ?: ""
                } else ""
            } catch (e: Exception) { "" }
        } ?: ""
    }

    private val _state = MutableStateFlow(HomeState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                hasLoadedInitialData = true
                chatRepository.connectWebSocket()
                loadConversations()
                observeIncoming()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeState()
        )

    sealed interface HomeEvent {
        data class NavigateToChat(val conversationId: String, val recipientName: String) : HomeEvent
    }

    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    private fun loadConversations() {
        viewModelScope.launch {
            // Fetch from API and cache
            chatRepository.fetchAndCacheConversations()

            // Then observe from Room
            chatRepository.observeConversations().collect { conversations ->
                _state.update { it.copy(conversations = conversations, isLoading = false) }
            }
        }
    }

    private fun observeIncoming() {
        viewModelScope.launch {
            chatRepository.observeIncoming().collect { message ->
                if (message.type == WsMessage.TYPE_DIRECT_MESSAGE) {
                    chatRepository.handleIncomingMessage(message, myUserId)
                }
            }
        }
    }

    private fun syncContactsInBackground() {
        if (hasRequestedContactsSync) return
        hasRequestedContactsSync = true

        viewModelScope.launch {
            try {
                val deviceContacts = contactsReader.getContacts()
                if (deviceContacts.isEmpty()) {
                    println("[ContactSync] No contacts found on device.")
                    return@launch
                }

                println("[ContactSync] Found ${deviceContacts.size} contacts on device. Syncing...")

                contactsRepository.syncContacts(deviceContacts)
                    .onSuccess { response ->
                        println("[ContactSync] ✅ Synced to Room DB! Onboarded: ${response.onboardedCount}, Not onboarded: ${response.notOnboardedCount}")
                    }
                    .onError { error ->
                        println("[ContactSync] ❌ API error: ${error.message}")
                        hasRequestedContactsSync = false
                    }
            } catch (e: Exception) {
                println("[ContactSync] ❌ Exception: ${e.message}")
                hasRequestedContactsSync = false
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnConversationClick -> {
                viewModelScope.launch {
                    _events.send(
                        HomeEvent.NavigateToChat(
                            conversationId = action.conversationId,
                            recipientName = action.recipientName,
                        )
                    )
                }
            }

            HomeAction.OnContactsPermissionGranted -> {
                syncContactsInBackground()
            }
        }
    }
}