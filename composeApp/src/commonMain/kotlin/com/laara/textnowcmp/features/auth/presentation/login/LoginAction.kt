package com.laara.textnowcmp.features.auth.presentation.login

sealed interface LoginAction {
    data class OnPhoneNumberChange(val phoneNumber: String) : LoginAction
    data class OnCountryCodeChange(val countryCode: String) : LoginAction
    data object OnSendOtpClick : LoginAction
}