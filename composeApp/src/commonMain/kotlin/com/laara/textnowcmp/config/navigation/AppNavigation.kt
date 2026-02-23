package com.laara.textnowcmp.config.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.laara.textnowcmp.features.auth.presentation.login.LoginRoot
import com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.OtpVerificationRoot
import com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.personalDetail.PersonalDetailsRoot
import com.laara.textnowcmp.features.splash.presentation.SplashRoot

@Composable
fun AppNavigation() {
    val appController: TextNowController = rememberTextNowController()

    NavHost(
        navController = appController.navController,
        startDestination = AuthScreenDestination.SplashScreen
    ) {
        composable<AuthScreenDestination.SplashScreen> {
            SplashRoot(
                navigateToTop = appController::navigateToTop
            )
        }
        authGraph(appController)
        homeGraph(appController)
    }
}

private fun NavGraphBuilder.authGraph(appController: TextNowController) {
    navigation<MainGraph.AuthGraph>(
        startDestination = AuthScreenDestination.LoginScreen
    ) {
        composable<AuthScreenDestination.LoginScreen> {
            LoginRoot(onNavigateToOtp = appController::navigate)
        }
        composable<AuthScreenDestination.OtpVerification> {
            OtpVerificationRoot(
                onNavigateBack = appController::upPress,
                onNavigate = appController::navigateToTop
            )
        }
        composable<AuthScreenDestination.PersonalDetails> {
            PersonalDetailsRoot(
                onNavigateToHome = {},
                onNavigateBack = appController::upPress
            )
        }

    }
}

private fun NavGraphBuilder.homeGraph(appController: TextNowController) {
}

