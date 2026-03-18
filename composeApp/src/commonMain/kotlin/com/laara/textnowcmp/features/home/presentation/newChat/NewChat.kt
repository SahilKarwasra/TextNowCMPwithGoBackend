package com.laara.textnowcmp.features.home.presentation.newChat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laara.textnowcmp.config.database.entity.ContactEntity
import com.laara.textnowcmp.core.util.ObserveAsEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NewChatRoot(
    viewModel: NewChatViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToChat: (conversationId: String, recipientName: String) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            NewChatEvent.NavigateBack -> onNavigateBack()
            is NewChatEvent.NavigateToChat -> onNavigateToChat(event.conversationId, event.recipientName)
            is NewChatEvent.ShareInviteLink -> {
                // TODO: Share intent
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
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        ) {
            // ── Top Bar ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onAction(NewChatAction.OnBackClick) }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Select contact", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
                    if (!state.isLoading) {
                        Text(
                            "${state.onboardedContacts.size + state.notOnboardedContacts.size} contacts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Search ──
            TextField(
                value = state.searchQuery,
                onValueChange = { onAction(NewChatAction.OnSearchQueryChange(it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Search name or number", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                trailingIcon = {
                    AnimatedVisibility(state.searchQuery.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        IconButton(onClick = { onAction(NewChatAction.OnSearchQueryChange("")) }) {
                            Icon(Icons.Rounded.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

            Spacer(Modifier.height(8.dp))

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                    }
                }
                state.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.error, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (state.searchQuery.isBlank()) {
                            item {
                                QuickActionRow(icon = Icons.Rounded.GroupAdd, title = "New group", onClick = { onAction(NewChatAction.OnNewGroupClick) })
                            }
                        }
                        if (state.filteredOnboarded.isNotEmpty()) {
                            item { SectionHeader("Contacts on TextNow") }
                            items(state.filteredOnboarded, key = { it.phone }) { contact ->
                                ContactRow(
                                    contact = contact,
                                    onClick = { contact.userId?.let { onAction(NewChatAction.OnContactClick(it)) } },
                                )
                            }
                        }
                        if (state.filteredNotOnboarded.isNotEmpty()) {
                            item { SectionHeader("Invite to TextNow") }
                            items(state.filteredNotOnboarded, key = { it.phone }) { contact ->
                                ContactRow(
                                    contact = contact,
                                    showInvite = true,
                                    onInvite = { onAction(NewChatAction.OnInviteClick(contact.phone)) },
                                )
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun ContactRow(
    contact: ContactEntity,
    onClick: (() -> Unit)? = null,
    showInvite: Boolean = false,
    onInvite: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        val bgColor = if (contact.isOnboarded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        val textColor = if (contact.isOnboarded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        Box(Modifier.size(48.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
            if (contact.name.isNotBlank()) {
                Text(
                    contact.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString(""),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                )
            } else {
                Icon(Icons.Rounded.PersonAdd, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(
                contact.name.ifBlank { contact.phone },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (contact.name.isNotBlank()) {
                Text(contact.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (showInvite) {
            OutlinedButton(
                onClick = { onInvite?.invoke() },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Invite", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}