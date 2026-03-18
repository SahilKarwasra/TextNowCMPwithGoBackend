package com.laara.textnowcmp.features.home.presentation.newChat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laara.textnowcmp.config.network.onError
import com.laara.textnowcmp.config.network.onSuccess
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

class NewChatViewModel(
    private val contactsRepository: ContactsRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(NewChatState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                hasLoadedInitialData = true
                loadContacts()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = NewChatState()
        )

    private val _events = Channel<NewChatEvent>()
    val events = _events.receiveAsFlow()

    private fun loadContacts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            launch {
                contactsRepository.getOnboardedContacts().collect { contacts ->
                    _state.update { it.copy(isLoading = false, onboardedContacts = contacts) }
                }
            }

            launch {
                contactsRepository.getNotOnboardedContacts().collect { contacts ->
                    _state.update { it.copy(isLoading = false, notOnboardedContacts = contacts) }
                }
            }
        }
    }

    fun onAction(action: NewChatAction) {
        when (action) {
            is NewChatAction.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = action.query) }
            }

            is NewChatAction.OnContactClick -> {
                // Create conversation via API, then navigate
                viewModelScope.launch {
                    _state.update { it.copy(isLoading = true) }

                    chatRepository.createOrGetConversation(action.userId)
                        .onSuccess { response ->
                            _state.update { it.copy(isLoading = false) }
                            _events.send(
                                NewChatEvent.NavigateToChat(
                                    conversationId = response.conversationId,
                                    recipientName = response.participant.name,
                                )
                            )
                        }
                        .onError { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "Failed to create conversation"
                                )
                            }
                        }
                }
            }

            is NewChatAction.OnInviteClick -> {
                viewModelScope.launch {
                    _events.send(NewChatEvent.ShareInviteLink(action.phone))
                }
            }

            NewChatAction.OnNewGroupClick -> {
                // TODO: Navigate to create group screen
            }

            NewChatAction.OnBackClick -> {
                viewModelScope.launch {
                    _events.send(NewChatEvent.NavigateBack)
                }
            }
        }
    }
}