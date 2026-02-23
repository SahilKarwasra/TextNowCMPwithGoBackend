package com.laara.textnowcmp.features.auth.presentation.login

import com.laara.textnowcmp.config.navigation.AuthScreenDestination

sealed interface LoginEvent {
    data class NavigateToOtpVerification(val navKey: AuthScreenDestination) : LoginEvent
}