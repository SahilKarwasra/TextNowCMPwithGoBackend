package com.laara.textnowcmp.features.auth.presentation.login.otpVerificationScreen.personalDetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.laara.textnowcmp.core.shared.rememberImagePickerLauncher
import com.laara.textnowcmp.core.theme.TextNowCMPTheme
import com.laara.textnowcmp.core.util.ObserveAsEvents

@Composable
fun PersonalDetailsRoot(
    viewModel: PersonalDetailsViewModel = viewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) {
        when (it) {
            PersonalDetailsEvent.NavigateToHome -> onNavigateToHome()
            PersonalDetailsEvent.NavigateBack -> onNavigateBack()
        }
    }

    PersonalDetailsScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun PersonalDetailsScreen(
    state: PersonalDetailsState,
    onAction: (PersonalDetailsAction) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val nameFocusRequester = remember { FocusRequester() }

    val pickImage = rememberImagePickerLauncher { uri ->
        onAction(PersonalDetailsAction.OnProfilePicSelected(uri))
    }

    LaunchedEffect(Unit) {
        visible = true
        nameFocusRequester.requestFocus()
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
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                IconButton(onClick = { onAction(PersonalDetailsAction.OnBackClick) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
            ) {

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { -40 },
                ) {
                    Column {
                        Text(
                            text = "Tell us about\nyourself.",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                lineHeight = 44.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Add your details to personalise your experience.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 24.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { 30 },
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AvatarPicker(
                            uri = state.profilePicUri,
                            onPickImage = { pickImage() },
                            onRemove = { onAction(PersonalDetailsAction.OnRemoveProfilePic) },
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (state.profilePicUri == null) "Add profile photo" else "Change photo",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Text(
                            text = "Optional",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { 60 },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        StyledInputField(
                            value = state.name,
                            onValueChange = { onAction(PersonalDetailsAction.OnNameChange(it)) },
                            label = "FULL NAME",
                            placeholder = "Your name",
                            isError = state.isNameError,
                            leadingIcon = Icons.Rounded.Person,
                            focusRequester = nameFocusRequester,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            ),
                        )

                        StyledInputField(
                            value = state.email,
                            onValueChange = { onAction(PersonalDetailsAction.OnEmailChange(it)) },
                            label = "EMAIL ADDRESS",
                            placeholder = "you@example.com",
                            isError = state.isEmailError,
                            leadingEmoji = "✉",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    onAction(PersonalDetailsAction.OnContinueClick)
                                },
                            ),
                        )
                    }
                }

                // Global error
                AnimatedVisibility(visible = state.error != null) {
                    state.error?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { 80 },
                ) {
                    Column {
                        Button(
                            onClick = { onAction(PersonalDetailsAction.OnContinueClick) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            enabled = !state.isLoading,
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = "Continue",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Name and email are required to set up your account.",
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
}

@Composable
private fun AvatarPicker(
    uri: String?,
    onPickImage: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(100.dp),
    ) {
        // Circle
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 2.dp,
                    color = if (uri != null)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onPickImage() },
            contentAlignment = Alignment.Center,
        ) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(38.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { if (uri != null) onRemove() else onPickImage() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (uri != null) Icons.Rounded.Close else Icons.Rounded.Add,
                contentDescription = if (uri != null) "Remove photo" else "Add photo",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun StyledInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean,
    leadingIcon: ImageVector? = null,
    leadingEmoji: String? = null,
    focusRequester: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            isFocused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "fieldBorder",
    )

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (isError)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .border(
                    width = if (isFocused || isError) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                leadingIcon != null -> Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isError)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp),
                )
                leadingEmoji != null -> Text(
                    text = leadingEmoji,
                    fontSize = 16.sp,
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (focusRequester != null) Modifier.focusRequester(focusRequester)
                        else Modifier,
                    )
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(
                    if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                    }
                    inner()
                },
                singleLine = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewEmpty() {
    TextNowCMPTheme {
        PersonalDetailsScreen(state = PersonalDetailsState(), onAction = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewErrors() {
    TextNowCMPTheme {
        PersonalDetailsScreen(
            state = PersonalDetailsState(
                isNameError = true,
                isEmailError = true,
                error = "Please enter your name and a valid email.",
            ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFilled() {
    TextNowCMPTheme {
        PersonalDetailsScreen(
            state = PersonalDetailsState(name = "Alex Johnson", email = "alex@example.com"),
            onAction = {},
        )
    }
}