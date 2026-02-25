package com.laara.textnowcmp.config.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.laara.textnowcmp.core.util.ObserveAsEvents
import com.laara.textnowcmp.core.util.ui.AppDialog
import com.laara.textnowcmp.core.util.ui.DialogButtonStyle
import com.laara.textnowcmp.core.util.ui.UiEvent
import com.laara.textnowcmp.core.util.ui.UiEventController
import com.laara.textnowcmp.features.auth.presentation.login.LoginRoot
import com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.OtpVerificationRoot
import com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.personalDetail.PersonalDetailsRoot
import com.laara.textnowcmp.features.splash.presentation.SplashRoot
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val appController: TextNowController = rememberTextNowController()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialogEvent by remember { mutableStateOf<UiEvent.Dialog?>(null) }
    var fullScreenDialog by remember { mutableStateOf<UiEvent.FullScreenDialog?>(null) }

    ObserveAsEvents(UiEventController.events) { uiEvent ->
        when (uiEvent) {
            is UiEvent.Dialog -> {
                dialogEvent = uiEvent
            }

            is UiEvent.FullScreenDialog -> {
                fullScreenDialog = uiEvent
            }

            is UiEvent.Snackbar -> {
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = uiEvent.message,
                        actionLabel = uiEvent.actionLabel,
                        withDismissAction = uiEvent.withDismissAction,
                        duration = uiEvent.duration
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        uiEvent.onAction?.invoke()
                    }
                }
            }

            is UiEvent.SessionExpired -> {
                dialogEvent = UiEvent.Dialog(
                    title = "Session Expired",
                    message = {
                        Text(text = "Please login again")
                    },
                    confirmText = "Ok",
                    buttonStyle = DialogButtonStyle.Primary,
                    cancelable = false,
                    onConfirm = {
                        appController.navigateToTop(MainGraph.AuthGraph)
                        dialogEvent = null
                    },
                    onDismiss = null
                )
            }
        }
    }

    dialogEvent?.let {
        AppDialog(
            event = it, onDismiss = { dialogEvent = null })
    }
    fullScreenDialog?.let { event ->
        Dialog(
            onDismissRequest = {
                if (event.cancelable) {
                    event.onDismiss?.invoke()
                    fullScreenDialog = null
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            ),
        ) {
            event.content {
                event.onDismiss?.invoke()
                fullScreenDialog = null
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(
                    snackbarData = it,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) {
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

