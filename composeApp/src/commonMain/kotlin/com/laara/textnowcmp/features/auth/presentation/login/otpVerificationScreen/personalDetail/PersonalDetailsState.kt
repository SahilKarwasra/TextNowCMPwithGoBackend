package com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.personalDetail

data class PersonalDetailsState(
    val name: String = "",
    val email: String = "",
    val profilePicUri: String? = null,
    val isNameError: Boolean = false,
    val isEmailError: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)