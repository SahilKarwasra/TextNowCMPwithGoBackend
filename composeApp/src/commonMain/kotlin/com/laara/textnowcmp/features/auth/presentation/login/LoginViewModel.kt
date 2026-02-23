package com.laara.textnowcmp.features.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laara.textnowcmp.config.navigation.AuthScreenDestination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(LoginState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = LoginState()
        )

    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnPhoneNumberChange -> {
                _state.update { it.copy(phoneNumber = action.phoneNumber, error = null) }
            }
            is LoginAction.OnCountryCodeChange -> {
                _state.update { it.copy(countryCode = action.countryCode) }
            }
            is LoginAction.OnSendOtpClick -> sendOtp()
        }
    }

    private fun sendOtp() {
        val currentState = _state.value
        if (currentState.phoneNumber.length !in 7..12) {
            _state.update { it.copy(error = "Phone number must be 7–12 digits") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            kotlinx.coroutines.delay(1000L)
            _state.update { it.copy(isLoading = false) }
            _events.send(
                LoginEvent.NavigateToOtpVerification(
                    navKey = AuthScreenDestination.OtpVerification(
                        phoneNumber = "${currentState.countryCode}${currentState.phoneNumber}"
                    )
                )
            )
        }
    }
}