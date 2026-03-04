package com.laara.textnowcmp.features.home.presentation.newChat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.laara.textnowcmp.core.theme.TextNowCMPTheme
import com.laara.textnowcmp.core.util.ObserveAsEvents
import com.laara.textnowcmp.features.contacts.data.remote.dto.OnboardedUserDto
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NewChatRoot(
    viewModel: NewChatViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            NewChatEvent.NavigateBack -> onNavigateBack()
            is NewChatEvent.NavigateToChat -> onNavigateToChat(event.userId)
            is NewChatEvent.ShareInviteLink -> {
                // TODO: Share intent for inviting user
            }
        }
    }

    NewChatScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun NewChatScreen(
    state: NewChatState,
    onAction: (NewChatAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onAction(NewChatAction.OnBackClick) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Select contact",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (!state.isLoading) {
                        Text(
                            text = "${state.onboardedContacts.size + state.notOnboardedPhones.size} contacts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Search Bar ──
            TextField(
                value = state.searchQuery,
                onValueChange = { onAction(NewChatAction.OnSearchQueryChange(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        text = "Search name or number",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = state.searchQuery.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        IconButton(onClick = {
                            onAction(NewChatAction.OnSearchQueryChange(""))
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Content ──
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                        )
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // ── Quick Actions ──
                        if (state.searchQuery.isBlank()) {
                            item {
                                QuickActionRow(
                                    icon = Icons.Rounded.GroupAdd,
                                    title = "New group",
                                    onClick = { onAction(NewChatAction.OnNewGroupClick) },
                                )
                            }
                        }

                        // ── Onboarded Contacts ──
                        if (state.filteredOnboarded.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Contacts on TextNow")
                            }

                            items(
                                items = state.filteredOnboarded,
                                key = { it.userId },
                            ) { user ->
                                OnboardedContactItem(
                                    user = user,
                                    onClick = {
                                        onAction(NewChatAction.OnContactClick(user.userId))
                                    },
                                )
                            }
                        }

                        // ── Not Onboarded Contacts ──
                        if (state.filteredNotOnboarded.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Invite to TextNow")
                            }

                            items(
                                items = state.filteredNotOnboarded,
                                key = { it },
                            ) { phone ->
                                NotOnboardedContactItem(
                                    phone = phone,
                                    onInvite = {
                                        onAction(NewChatAction.OnInviteClick(phone))
                                    },
                                )
                            }
                        }

                        // Bottom spacing
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

// ── Quick action row (New group, etc.) ──
@Composable
private fun QuickActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ── Section header ──
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

// ── Onboarded contact row ──
@Composable
private fun OnboardedContactItem(
    user: OnboardedUserDto,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        if (user.profilePic.isNotBlank()) {
            AsyncImage(
                model = user.profilePic,
                contentDescription = user.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user.name
                        .split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                        .joinToString(""),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name.ifBlank { user.phone },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.name.isNotBlank()) {
                Text(
                    text = user.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Not onboarded contact row with invite button ──
@Composable
private fun NotOnboardedContactItem(
    phone: String,
    onInvite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Gray avatar with PersonAdd icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = phone,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )

        OutlinedButton(
            onClick = onInvite,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                text = "Invite",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

// ── Previews ──

@Preview(showBackground = true)
@Composable
private fun PreviewLoading() {
    TextNowCMPTheme {
        NewChatScreen(
            state = NewChatState(isLoading = true),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewWithContacts() {
    TextNowCMPTheme {
        NewChatScreen(
            state = NewChatState(
                isLoading = false,
                onboardedContacts = listOf(
                    OnboardedUserDto("1", "Sahil Karwasra", "9812408030", "sahil@email.com", ""),
                    OnboardedUserDto("2", "Rahul Sharma", "9876543210", "", ""),
                ),
                notOnboardedPhones = listOf("9123456789", "0000000000"),
            ),
            onAction = {},
        )
    }
}