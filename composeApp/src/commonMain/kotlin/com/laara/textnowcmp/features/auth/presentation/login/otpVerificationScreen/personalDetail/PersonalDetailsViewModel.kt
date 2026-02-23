package com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.personalDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonalDetailsViewModel : ViewModel() {

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(PersonalDetailsState())
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = PersonalDetailsState(),
        )

    private val _events = Channel<PersonalDetailsEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: PersonalDetailsAction) {
        when (action) {
            is PersonalDetailsAction.OnNameChange -> {
                _state.update { it.copy(name = action.name, isNameError = false, error = null) }
            }
            is PersonalDetailsAction.OnEmailChange -> {
                _state.update { it.copy(email = action.email, isEmailError = false, error = null) }
            }
            is PersonalDetailsAction.OnProfilePicSelected -> {
                _state.update { it.copy(profilePicUri = action.uri) }
            }
            PersonalDetailsAction.OnRemoveProfilePic -> {
                _state.update { it.copy(profilePicUri = null) }
            }
            PersonalDetailsAction.OnContinueClick -> saveDetails()
            PersonalDetailsAction.OnBackClick -> {
                viewModelScope.launch { _events.send(PersonalDetailsEvent.NavigateBack) }
            }
        }
    }

    private fun saveDetails() {
        val current = _state.value
        val nameBlank = current.name.isBlank()
        val emailInvalid = !isValidEmail(current.email)

        if (nameBlank || emailInvalid) {
            _state.update {
                it.copy(
                    isNameError = nameBlank,
                    isEmailError = emailInvalid,
                    error = when {
                        nameBlank && emailInvalid -> "Please enter your name and a valid email."
                        nameBlank -> "Name is required."
                        else -> "Please enter a valid email address."
                    },
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            delay(1200L) // Simulate network / DB call
            _state.update { it.copy(isLoading = false) }
            _events.send(PersonalDetailsEvent.NavigateToHome)
        }
    }
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val emailRegex = Regex(
            pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        )
        return emailRegex.matches(email)
    }
}