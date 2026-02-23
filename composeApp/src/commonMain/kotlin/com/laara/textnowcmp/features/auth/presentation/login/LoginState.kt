package com.laara.textnowcmp.features.auth.presentation.login

data class LoginState(
    val phoneNumber: String = "",
    val countryCode: String = "+1",
    val isLoading: Boolean = false,
    val error: String? = null,
)