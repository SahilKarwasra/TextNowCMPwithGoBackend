package com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen

sealed interface OtpVerificationEvent {
    data class Navigate(val navKey: Any) : OtpVerificationEvent
    data object NavigateBack : OtpVerificationEvent
}