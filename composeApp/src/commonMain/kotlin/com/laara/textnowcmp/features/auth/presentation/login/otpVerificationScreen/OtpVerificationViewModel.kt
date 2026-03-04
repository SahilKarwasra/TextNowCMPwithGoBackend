package com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.laara.textnowcmp.config.navigation.AuthScreenDestination
import com.laara.textnowcmp.config.navigation.MainGraph
import com.laara.textnowcmp.config.network.onError
import com.laara.textnowcmp.config.network.onSuccess
import com.laara.textnowcmp.config.network.sendSnackbarOnError
import com.laara.textnowcmp.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtpVerificationViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<AuthScreenDestination.OtpVerification>()
    val phoneNumber = args.phoneNumber

    private var hasLoadedInitialData = false
    private var countdownJob: Job? = null

    private val _state = MutableStateFlow(OtpVerificationState(phoneNumber = phoneNumber))
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                startResendCountdown()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = OtpVerificationState(phoneNumber = phoneNumber)
        )

    private val _events = Channel<OtpVerificationEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: OtpVerificationAction) {
        when (action) {
            is OtpVerificationAction.OnOtpChange -> {
                if (action.otp.length <= 6 && action.otp.all { it.isDigit() }) {
                    _state.update { it.copy(otpCode = action.otp, error = null) }
                    if (action.otp.length == 6) onAction(OtpVerificationAction.OnVerifyClick)
                }
            }
            OtpVerificationAction.OnVerifyClick -> verifyOtp()
            OtpVerificationAction.OnResendClick -> resendOtp()
            OtpVerificationAction.OnBackClick -> {
                viewModelScope.launch { _events.send(OtpVerificationEvent.NavigateBack) }
            }
        }
    }

    private fun verifyOtp() {
        val otp = _state.value.otpCode
        if (otp.length < 6) {
            _state.update { it.copy(error = "Please enter the complete 6-digit code") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authRepository.verifyOtp(phone = phoneNumber, otp = otp)
                .onSuccess { data ->
                    _state.update { it.copy(isLoading = false) }

                    // Tokens are already saved by AuthRepositoryImpl
                    if (data.isNewUser) {
                        // New user → navigate to personal details
                        _events.send(
                            OtpVerificationEvent.Navigate(AuthScreenDestination.PersonalDetails)
                        )
                    } else {
                        // Existing user → navigate to home
                        _events.send(
                            OtpVerificationEvent.Navigate(MainGraph.HomeGraph)
                        )
                    }
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "OTP verification failed"
                        )
                    }
                }.sendSnackbarOnError(skipAuth = true)
        }
    }

    private fun resendOtp() {
        if (!_state.value.canResend) return
        viewModelScope.launch {
            _state.update { it.copy(otpCode = "", error = null) }

            authRepository.sendOtp(phone = phoneNumber)
                .onSuccess {
                    startResendCountdown()
                }
                .onError { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Failed to resend OTP")
                    }
                }.sendSnackbarOnError(skipAuth = true)
        }
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        _state.update { it.copy(canResend = false, resendCooldownSeconds = 30) }
        countdownJob = viewModelScope.launch {
            for (i in 29 downTo 0) {
                delay(1000L)
                _state.update { it.copy(resendCooldownSeconds = i) }
            }
            _state.update { it.copy(canResend = true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}