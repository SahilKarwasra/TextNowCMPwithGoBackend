package com.laara.textnowcmp.features.splash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laara.textnowcmp.config.navigation.MainGraph
import com.laara.textnowcmp.config.network.onError
import com.laara.textnowcmp.config.network.onSuccess
import com.laara.textnowcmp.core.shared.ContactsReader
import com.laara.textnowcmp.core.util.TokenProvider
import com.laara.textnowcmp.features.contacts.domain.repository.ContactsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel(
    private val tokenProvider: TokenProvider,
    private val contactsReader: ContactsReader,
    private val contactsRepository: ContactsRepository,
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(SplashState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                hasLoadedInitialData = true
                navigateAfterDelay()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SplashState()
        )

    private val _events = Channel<SplashEvent>()
    val events = _events.receiveAsFlow()

    private fun navigateAfterDelay() {
        viewModelScope.launch {
            delay(2_500L)
            _state.update { it.copy(isNavigatingAway = true) }
            delay(500L)

            val isLoggedIn = !tokenProvider.getAccessToken().isNullOrBlank()

            println("AccessToken: ${tokenProvider.getAccessToken()}")
            println("RefreshToken: ${tokenProvider.getRefreshToken()}")

            if (isLoggedIn) {
                launch { syncContactsInBackground() }
            }

            _events.send(
                SplashEvent.Navigate(
                    navKey = if (isLoggedIn) MainGraph.HomeGraph
                             else MainGraph.AuthGraph
                )
            )
        }
    }

    private suspend fun syncContactsInBackground() {
        try {
            val deviceContacts = contactsReader.getContacts()
            if (deviceContacts.isEmpty()) {
                println("[ContactSync] No contacts found on device.")
                return
            }

            println("[ContactSync] Found ${deviceContacts.size} contacts on device. Syncing...")

            contactsRepository.syncContacts(deviceContacts)
                .onSuccess { response ->
                    println("[ContactSync] ✅ Synced and cached! Total: ${response.total}, Onboarded: ${response.onboardedCount}, Not onboarded: ${response.notOnboardedCount}")
                }
                .onError { error ->
                    println("[ContactSync] ❌ API error: ${error.message}")
                }
        } catch (e: Exception) {
            println("[ContactSync] ❌ Exception: ${e.message}")
        }
    }

    fun onAction(action: SplashAction) {
    }
}