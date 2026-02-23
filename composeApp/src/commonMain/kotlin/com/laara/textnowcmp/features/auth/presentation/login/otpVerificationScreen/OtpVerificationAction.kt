package com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen

sealed interface OtpVerificationAction {
    data class OnOtpChange(val otp: String) : OtpVerificationAction
    data object OnVerifyClick : OtpVerificationAction
    data object OnResendClick : OtpVerificationAction
    data object OnBackClick : OtpVerificationAction
}