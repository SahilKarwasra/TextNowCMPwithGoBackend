package com.laara.textnowcmp.features.auth.presentation.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laara.textnowcmp.config.navigation.AuthScreenDestination
import com.laara.textnowcmp.core.theme.TextNowCMPTheme
import com.laara.textnowcmp.core.util.ObserveAsEvents
import com.laara.textnowcmp.core.util.allCountries
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoot(
    viewModel: LoginViewModel = koinViewModel(),
    onNavigateToOtp: (AuthScreenDestination) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) {
        when (it) {
            is LoginEvent.NavigateToOtpVerification -> onNavigateToOtp(it.navKey)
        }
    }

    LoginScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var pickerQuery by remember { mutableStateOf("") }

    val phoneFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectedCountry = allCountries.find { it.code == state.countryCode }
        ?: allCountries.first()

    val filteredCountries = remember(pickerQuery) {
        if (pickerQuery.isBlank()) allCountries
        else allCountries.filter {
            it.name.contains(pickerQuery, ignoreCase = true) || it.code.contains(pickerQuery)
        }
    }

    // Show keyboard whenever picker state changes
    LaunchedEffect(showPicker) {
        if (showPicker) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            phoneFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(Unit) {
        visible = true
        phoneFocusRequester.requestFocus()
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
                Spacer(modifier = Modifier.height(64.dp))
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { -40 },
                ) {
                    Column {
                        Text(
                            text = "Welcome back.",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enter your phone number and we'll\nsend you a one-time code.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { 60 },
            ) {
                Column {
                    Text(
                        text = "PHONE NUMBER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phone row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Country chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { showPicker = !showPicker; pickerQuery = "" }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(text = selectedCountry.flag, fontSize = 18.sp)
                            Text(
                                text = selectedCountry.code,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Pick country",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(if (showPicker) 180f else 0f),
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        BasicTextField(
                            value = state.phoneNumber,
                            onValueChange = { v ->
                                if (v.all { it.isDigit() } && v.length <= 12) {
                                    onAction(LoginAction.OnPhoneNumberChange(v))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(phoneFocusRequester),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { onAction(LoginAction.OnSendOtpClick) },
                            ),
                            decorationBox = { inner ->
                                if (state.phoneNumber.isEmpty()) {
                                    Text(
                                        text = "000 000 0000",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            letterSpacing = 0.5.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.4f,
                                        ),
                                    )
                                }
                                inner()
                            },
                            singleLine = true,
                        )
                    }

                    AnimatedVisibility(
                        visible = showPicker,
                        enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(180)),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                    )
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        RoundedCornerShape(14.dp),
                                    ),
                            ) {
                                Column {
                                    // Search
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.5f,
                                            ),
                                            modifier = Modifier.size(18.dp),
                                        )
                                        BasicTextField(
                                            value = pickerQuery,
                                            onValueChange = { pickerQuery = it },
                                            modifier = Modifier
                                                .weight(1f)
                                                .focusRequester(searchFocusRequester),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Text,
                                                imeAction = ImeAction.Search,
                                            ),
                                            decorationBox = { inner ->
                                                if (pickerQuery.isEmpty()) {
                                                    Text(
                                                        text = "Search country…",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.4f,
                                                        ),
                                                    )
                                                }
                                                inner()
                                            },
                                            singleLine = true,
                                        )
                                    }

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                        thickness = 1.dp,
                                    )

                                    LazyColumn(modifier = Modifier.height(220.dp)) {
                                        items(
                                            items = filteredCountries,
                                            key = { "${it.name}_${it.code}" },
                                        ) { country ->
                                            val isSelected = country.name == selectedCountry.name

                                            val rowBg by animateColorAsState(
                                                targetValue = if (isSelected)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                else
                                                    MaterialTheme.colorScheme.surface,
                                                animationSpec = tween(150),
                                                label = "rowBg",
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(rowBg)
                                                    .clickable(
                                                        interactionSource = remember {
                                                            MutableInteractionSource()
                                                        },
                                                        indication = null,
                                                    ) {
                                                        onAction(
                                                            LoginAction.OnCountryCodeChange(country.code),
                                                        )
                                                        showPicker = false
                                                        pickerQuery = ""
                                                    }
                                                    .padding(
                                                        horizontal = 16.dp,
                                                        vertical = 13.dp,
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            ) {
                                                Text(text = country.flag, fontSize = 20.sp)
                                                Text(
                                                    text = country.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected)
                                                            FontWeight.SemiBold
                                                        else
                                                            FontWeight.Normal,
                                                    ),
                                                    color = if (isSelected)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    text = country.code,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Medium,
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }

                                            if (filteredCountries.last() != country) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    color = MaterialTheme.colorScheme.outline.copy(
                                                        alpha = 0.07f,
                                                    ),
                                                    thickness = 1.dp,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Error
                    AnimatedVisibility(visible = state.error != null) {
                        state.error?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
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
                        onClick = { onAction(LoginAction.OnSendOtpClick) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        enabled = !state.isLoading && state.phoneNumber.length in 7..12
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text = "Send Code",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "By continuing, you agree to our Terms of Service\nand Privacy Policy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 18.sp,
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TextNowCMPTheme {
        LoginScreen(state = LoginState(), onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFilled() {
    TextNowCMPTheme {
        LoginScreen(state = LoginState(phoneNumber = "9876543210"), onAction = {})
    }
}