package com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.personalDetail

sealed interface PersonalDetailsAction {
    data class OnNameChange(val name: String) : PersonalDetailsAction
    data class OnEmailChange(val email: String) : PersonalDetailsAction
    data class OnProfilePicSelected(val uri: String) : PersonalDetailsAction
    data object OnRemoveProfilePic : PersonalDetailsAction
    data object OnContinueClick : PersonalDetailsAction
    data object OnBackClick : PersonalDetailsAction
}