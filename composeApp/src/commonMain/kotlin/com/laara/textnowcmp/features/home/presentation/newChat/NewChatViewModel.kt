package com.laara.textnowcmp.features.home.presentation.newChat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laara.textnowcmp.config.network.onError
import com.laara.textnowcmp.config.network.onSuccess
import com.laara.textnowcmp.core.shared.ContactsReader
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
    private val contactsReader: ContactsReader,
    private val contactsRepository: ContactsRepository,
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

            try {
                val phones = contactsReader.getPhoneNumbers()
                if (phones.isEmpty()) {
                    _state.update {
                        it.copy(isLoading = false, error = "No contacts found on device")
                    }
                    return@launch
                }

                contactsRepository.checkContacts(phones)
                    .onSuccess { response ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                onboardedContacts = response.onboarded,
                                notOnboardedPhones = response.notOnboarded,
                            )
                        }
                    }
                    .onError { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to check contacts"
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Something went wrong")
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
                viewModelScope.launch {
                    _events.send(NewChatEvent.NavigateToChat(action.userId))
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