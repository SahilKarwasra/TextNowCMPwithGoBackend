package com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.personalDetail

sealed interface PersonalDetailsEvent {
    data object NavigateToHome : PersonalDetailsEvent
    data object NavigateBack : PersonalDetailsEvent
}