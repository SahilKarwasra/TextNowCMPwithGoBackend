package com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen

data class OtpVerificationState(
    val phoneNumber: String = "",
    val otpCode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val resendCooldownSeconds: Int = 30,
    val canResend: Boolean = false,
)