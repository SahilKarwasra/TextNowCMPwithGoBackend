package com.laara.textnowcmp.config.navigation

import kotlinx.serialization.Serializable


sealed interface MainGraph {
    @Serializable
    data object HomeGraph: MainGraph
    @Serializable
    data object AuthGraph: MainGraph
}
sealed interface AuthScreenDestination {
    @Serializable
    data object SplashScreen: AuthScreenDestination
    @Serializable
    data object LoginScreen: AuthScreenDestination
    @Serializable
    data class OtpVerification(val phoneNumber: String): AuthScreenDestination
    @Serializable
    data object PersonalDetails: AuthScreenDestination
}

sealed interface HomeScreenDestination {
    @Serializable
    data object HomeScreen: HomeScreenDestination
    @Serializable
    data object ChatScreen: HomeScreenDestination
    @Serializable
    data object ProfileScreen: HomeScreenDestination
}

