package com.laara.textnowcmp.features.splash.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laara.textnowcmp.config.navigation.AuthScreenDestination
import com.laara.textnowcmp.config.navigation.MainGraph
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

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
            delay(500L) // exit animation duration
            _events.send(SplashEvent.Navigate(
                navKey = AuthScreenDestination.LoginScreen
            ))
        }
    }

    fun onAction(action: SplashAction) {
    }
}