package com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laara.textnowcmp.core.theme.TextNowCMPTheme
import com.laara.textnowcmp.core.util.ObserveAsEvents

private const val OTP_LENGTH = 6

@Composable
fun OtpVerificationRoot(
    viewModel: OtpVerificationViewModel = viewModel(),
    onNavigate: (Any) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) {
        when (it) {
            OtpVerificationEvent.NavigateBack -> onNavigateBack()
            is OtpVerificationEvent.Navigate -> onNavigate(it.navKey)
        }
    }

    OtpVerificationScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun OtpVerificationScreen(
    state: OtpVerificationState,
    onAction: (OtpVerificationAction) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        visible = true
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {

            Column {
                Spacer(modifier = Modifier.height(12.dp))

                IconButton(onClick = { onAction(OtpVerificationAction.OnBackClick) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { -40 },
                ) {
                    Column {
                        Text(
                            text = "Check your\nmessages.",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                lineHeight = 44.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row {
                            Text(
                                text = "We sent a 6-digit code to ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = state.phoneNumber,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { 60 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "VERIFICATION CODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OtpInputRow(
                        otp = state.otpCode,
                        onOtpChange = { onAction(OtpVerificationAction.OnOtpChange(it)) },
                        hasError = state.error != null,
                        focusRequester = focusRequester,
                        onRequestFocus = {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        },
                    )

                    AnimatedVisibility(visible = state.error != null) {
                        state.error?.let {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { 80 },
            ) {
                Column {
                    Button(
                        onClick = { onAction(OtpVerificationAction.OnVerifyClick) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        enabled = !state.isLoading && state.otpCode.length == OTP_LENGTH,
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "Verify & Continue",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Didn't receive the code?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (state.canResend) {
                            TextButton(
                                onClick = { onAction(OtpVerificationAction.OnResendClick) },
                            ) {
                                Text(
                                    text = "Resend",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            Text(
                                text = "Resend in ${state.resendCooldownSeconds}s",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun OtpInputRow(
    otp: String,
    onOtpChange: (String) -> Unit,
    hasError: Boolean,
    focusRequester: FocusRequester,
    onRequestFocus: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onRequestFocus() },
    ) {
        BasicTextField(
            value = otp,
            onValueChange = { incoming ->
                if (incoming.length <= OTP_LENGTH && incoming.all { it.isDigit() }) {
                    onOtpChange(incoming)
                }
            },
            modifier = Modifier
                .wrapContentSize()
                .offset(x = (-9999).dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) keyboardController?.show()
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
            singleLine = true,
        )

        // Visual OTP boxes
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(OTP_LENGTH) { index ->
                val char = otp.getOrNull(index)
                val isFocused = index == otp.length && index < OTP_LENGTH
                val isError = hasError && otp.length == OTP_LENGTH

                val borderColor by animateColorAsState(
                    targetValue = when {
                        isError -> MaterialTheme.colorScheme.error
                        isFocused -> MaterialTheme.colorScheme.primary
                        char != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                    animationSpec = tween(200),
                    label = "borderColor_$index",
                )

                val borderWidth by animateDpAsState(
                    targetValue = if (isFocused || isError) 2.dp else 1.dp,
                    animationSpec = tween(200),
                    label = "borderWidth_$index",
                )

                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (char != null)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        )
                        .border(
                            width = borderWidth,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        char != null -> Text(
                            text = char.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        isFocused -> Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(22.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TextNowCMPTheme {
        OtpVerificationScreen(
            state = OtpVerificationState(phoneNumber = "+1 9876543210"),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFilled() {
    TextNowCMPTheme {
        OtpVerificationScreen(
            state = OtpVerificationState(phoneNumber = "+1 9876543210", otpCode = "123456"),
            onAction = {},
        )
    }
}