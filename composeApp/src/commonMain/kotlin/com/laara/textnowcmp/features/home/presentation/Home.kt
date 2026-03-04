package com.laara.textnowcmp.features.home.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHomeWork
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laara.textnowcmp.core.theme.TextNowCMPTheme

@Composable
fun HomeRoot(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToNewChat: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToNewChat = onNavigateToNewChat,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
    onNavigateToNewChat: () -> Unit = {},
) {
    val filters = listOf(
        FilterChipItem("all", "All"),
        FilterChipItem("unread", "Unread"),
        FilterChipItem("groups", "Groups"),
        FilterChipItem("favorites", "Favorites")
    )
    var selectedFilter by remember {
        mutableStateOf("all")
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "TextNow",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                )
                TextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    placeholder = {
                        Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(46),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                FilterRow(
                    filters = filters,
                    selectedFilter = selectedFilter,
                    onFilterSelected = {
                        selectedFilter = it.id
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                val chatList = listOf(
                    ChatItem(
                        id = "1",
                        name = "Puchu",
                        lastMessage = "Are you coming tomorrow?",
                        time = "10:42 AM",
                        unreadCount = 2
                    ),
                    ChatItem(
                        id = "2",
                        name = "PPP",
                        lastMessage = "Send me the design file",
                        time = "Yesterday",
                        unreadCount = 0
                    )
                )
                LazyColumn {
                    items(chatList) { chat ->
                        HomeChatItem(
                            chat = chat,
                            onClick = { }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = onNavigateToNewChat,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)

            ) {
                Icon(
                    Icons.Default.AddHomeWork,
                    null
                )
            }
        }

    }
}

@Composable
fun HomeChatItem(
    chat: ChatItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = chat.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )

                    Text(
                        text = chat.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {

                        Spacer(Modifier.width(8.dp))

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {

                            Text(
                                text = chat.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(
                                    horizontal = 6.dp,
                                    vertical = 2.dp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterRow(
    filters: List<FilterChipItem>,
    selectedFilter: String,
    onFilterSelected: (FilterChipItem) -> Unit
) {

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items(filters) { filter ->

            FilterChip(
                title = filter.title,
                isSelected = filter.id == selectedFilter,
                onClick = { onFilterSelected(filter) },
            )

        }
    }
}

@Composable
fun FilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val containerColor by animateColorAsState(
        if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
    )

    val textColor by animateColorAsState(
        if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant
    )

    Surface(
        modifier = modifier,
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(22.dp)
    ) {

        Text(
            text = title,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    TextNowCMPTheme {
        HomeScreen(
            state = HomeState(),
            onAction = {}
        )
    }
}