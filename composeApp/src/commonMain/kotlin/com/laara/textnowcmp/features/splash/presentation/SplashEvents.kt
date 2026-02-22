package com.laara.textnowcmp.features.splash.presentation

sealed interface SplashEvent {
    data class Navigate(val navKey: Any): SplashEvent
}