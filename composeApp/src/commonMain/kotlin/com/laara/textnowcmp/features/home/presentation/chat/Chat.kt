package com.laara.textnowcmp.features.home.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneMissed
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laara.textnowcmp.config.database.entity.MessageEntity
import com.laara.textnowcmp.core.util.ObserveAsEvents
import com.laara.textnowcmp.core.webrtc.RequestCallPermissions
import com.laara.textnowcmp.core.webrtc.WebRtcPeerConnection
import com.laara.textnowcmp.core.webrtc.WebRtcVideoView
import com.laara.textnowcmp.features.chat.data.remote.dto.WsMessage
import kotlinx.coroutines.delay
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

// ── Entry point ──────────────────────────────────────────────────────────────

@Composable
fun ChatRoot(
    viewModel: ChatViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ChatEvent.NavigateBack -> onNavigateBack()
        }
    }

    ChatScreen(
        state = state,
        onAction = viewModel::onAction,
        peerConnection = viewModel.peerConnection,
    )
}

// ── Chat Screen ───────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    peerConnection: WebRtcPeerConnection? = null,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Main chat content ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Top bar
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onAction(ChatAction.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }

                    // Avatar
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        val initials = state.recipientName
                            .split(" ")
                            .take(2)
                            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                            .joinToString("")
                        Text(
                            initials.ifBlank { "?" },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            state.recipientName.ifBlank { "Chat" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AnimatedVisibility(state.isTyping, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                "typing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // Call buttons (only when idle)
                    if (state.callScreenState is CallScreenState.Idle) {
                        IconButton(onClick = { onAction(ChatAction.OnVoiceCallClick) }) {
                            Icon(
                                Icons.Rounded.Phone,
                                contentDescription = "Voice call",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        IconButton(onClick = { onAction(ChatAction.OnVideoCallClick) }) {
                            Icon(
                                Icons.Rounded.Videocam,
                                contentDescription = "Video call",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Messages
            if (state.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                }
            } else if (state.messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👋", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Say hello!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Send your first message to start the conversation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    item { Spacer(Modifier.height(12.dp)) }
                    items(state.messages, key = { it.messageId }) { message ->
                        if (message.messageType == "call_log") {
                            CallLogBubble(message = message)
                        } else {
                            MessageBubble(message = message)
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                TextField(
                    value = state.inputText,
                    onValueChange = { onAction(ChatAction.OnInputChange(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Message", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ),
                )
                Spacer(Modifier.width(8.dp))
                val hasText = state.inputText.isNotBlank()
                Surface(
                    onClick = { if (hasText) onAction(ChatAction.OnSendClick) },
                    shape = CircleShape,
                    color = if (hasText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = if (hasText) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        // ── Permission request (triggered when user taps call button) ────
        state.pendingCallType?.let { callType ->
            RequestCallPermissions(
                isVideo = callType == WsMessage.CALL_TYPE_VIDEO,
                onGranted = { onAction(ChatAction.OnCallPermissionsGranted) },
                onDenied = { onAction(ChatAction.OnCallPermissionsDenied) },
            )
        }

        // ── Call overlays ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.callScreenState is CallScreenState.Incoming,
            modifier = Modifier.fillMaxSize(),
            enter = slideInVertically(tween(350)) { it } + fadeIn(tween(350)),
            exit = slideOutVertically(tween(350)) { it } + fadeOut(tween(350)),
        ) {
            (state.callScreenState as? CallScreenState.Incoming)?.let { incoming ->
                IncomingCallScreen(
                    incoming = incoming,
                    onAnswer = { onAction(ChatAction.OnCallAnswer) },
                    onDecline = { onAction(ChatAction.OnCallDecline) },
                )
            }
        }

        AnimatedVisibility(
            visible = state.callScreenState is CallScreenState.Outgoing,
            modifier = Modifier.fillMaxSize(),
            enter = slideInVertically(tween(350)) { it } + fadeIn(tween(350)),
            exit = slideOutVertically(tween(350)) { it } + fadeOut(tween(350)),
        ) {
            (state.callScreenState as? CallScreenState.Outgoing)?.let { outgoing ->
                OutgoingCallScreen(
                    outgoing = outgoing,
                    onEnd = { onAction(ChatAction.OnCallEnd) },
                )
            }
        }

        AnimatedVisibility(
            visible = state.callScreenState is CallScreenState.Active,
            modifier = Modifier.fillMaxSize(),
            enter = slideInVertically(tween(350)) { it } + fadeIn(tween(350)),
            exit = slideOutVertically(tween(350)) { it } + fadeOut(tween(350)),
        ) {
            (state.callScreenState as? CallScreenState.Active)?.let { active ->
                ActiveCallScreen(
                    active = active,
                    isMuted = state.isMuted,
                    isSpeakerOn = state.isSpeakerOn,
                    isCameraOn = state.isCameraOn,
                    peerConnection = peerConnection,
                    remoteVideoReady = state.remoteVideoReady,
                    onEnd = { onAction(ChatAction.OnCallEnd) },
                    onMuteToggle = { onAction(ChatAction.OnCallMuteToggle) },
                    onSpeakerToggle = { onAction(ChatAction.OnCallSpeakerToggle) },
                    onCameraToggle = { onAction(ChatAction.OnCallCameraToggle) },
                )
            }
        }
    }
}

// ── Incoming Call Screen ─────────────────────────────────────────────────────

@Composable
private fun IncomingCallScreen(
    incoming: CallScreenState.Incoming,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
) {
    val isVideo = incoming.callType == WsMessage.CALL_TYPE_VIDEO
    val callLabel = if (isVideo) "Incoming video call" else "Incoming voice call"

    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring1 by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "r1",
    )
    val ring2 by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(900, 200, FastOutSlowInEasing), RepeatMode.Reverse),
        label = "r2",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.15f))

            // Call type label
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.15f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.Phone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(callLabel, color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(40.dp))

            // Pulsing avatar rings
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(120.dp)
                        .scale(ring2)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                )
                Box(
                    Modifier
                        .size(100.dp)
                        .scale(ring1)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                )
                // Avatar
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        incoming.callerName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                incoming.callerName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (incoming.callerPhone.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    incoming.callerPhone,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.6f)),
                )
            }

            Spacer(Modifier.weight(1f))

            // Accept / Decline buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CallActionButton(
                    icon = { Icon(Icons.Rounded.CallEnd, "Decline", tint = Color.White, modifier = Modifier.size(28.dp)) },
                    color = Color(0xFFE53935),
                    label = "Decline",
                    onClick = onDecline,
                )
                CallActionButton(
                    icon = { Icon(Icons.Rounded.Phone, "Accept", tint = Color.White, modifier = Modifier.size(28.dp)) },
                    color = Color(0xFF43A047),
                    label = "Accept",
                    onClick = onAnswer,
                )
            }
        }
    }
}

// ── Outgoing Call Screen ─────────────────────────────────────────────────────

@Composable
private fun OutgoingCallScreen(
    outgoing: CallScreenState.Outgoing,
    onEnd: () -> Unit,
) {
    val isVideo = outgoing.callType == WsMessage.CALL_TYPE_VIDEO

    // Animated "Ringing..." dots
    var dots by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dots = when (dots.length) { 0 -> "." 1 -> ".." 2 -> "..." else -> "" }
        }
    }

    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by shimmer.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)))
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.2f))

            // Avatar
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    outgoing.recipientName.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                outgoing.recipientName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                (if (isVideo) "Video calling" else "Calling") + dots,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = shimmerAlpha),
                ),
            )

            Spacer(Modifier.weight(1f))

            // End call
            CallActionButton(
                icon = { Icon(Icons.Rounded.CallEnd, "End call", tint = Color.White, modifier = Modifier.size(28.dp)) },
                color = Color(0xFFE53935),
                label = "End call",
                onClick = onEnd,
                modifier = Modifier.padding(bottom = 56.dp),
            )
        }
    }
}

// ── Active Call Screen ───────────────────────────────────────────────────────

@Composable
private fun ActiveCallScreen(
    active: CallScreenState.Active,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isCameraOn: Boolean,
    peerConnection: WebRtcPeerConnection?,
    remoteVideoReady: Boolean,
    onEnd: () -> Unit,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onCameraToggle: () -> Unit,
) {
    val isVideo = active.callType == WsMessage.CALL_TYPE_VIDEO

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D1B2A), Color(0xFF1B2838), Color(0xFF243447)))
            ),
    ) {
        // ── Full-screen remote video (if video call) ──
        if (isVideo && peerConnection != null && remoteVideoReady) {
            key(remoteVideoReady) {
                WebRtcVideoView(
                    peerConnection = peerConnection,
                    isLocal = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // ── Local camera PiP (top right corner) ──
        if (isVideo && isCameraOn && peerConnection != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .size(width = 120.dp, height = 160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
            ) {
                WebRtcVideoView(
                    peerConnection = peerConnection,
                    isLocal = true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // ── Overlay controls ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.18f))

            if (!isVideo || peerConnection == null) {
                // Avatar for voice calls
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        active.recipientName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                active.recipientName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(8.dp))

            // Duration
            Text(
                formatCallDuration(active.durationSeconds),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium,
                ),
            )

            Spacer(Modifier.weight(1f))

            // Control row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CallControlButton(
                    icon = {
                        Icon(
                            if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = if (isMuted) "Unmute" else "Mute",
                    active = isMuted,
                    onClick = onMuteToggle,
                )
                CallControlButton(
                    icon = {
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = if (isSpeakerOn) "Speaker on" else "Speaker off",
                            tint = if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = "Speaker",
                    active = isSpeakerOn,
                    onClick = onSpeakerToggle,
                )
                if (isVideo) {
                    CallControlButton(
                        icon = {
                            Icon(
                                if (isCameraOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                                contentDescription = if (isCameraOn) "Camera on" else "Camera off",
                                tint = if (!isCameraOn) Color.White else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        label = if (isCameraOn) "Camera" else "No camera",
                        active = !isCameraOn,
                        onClick = onCameraToggle,
                    )
                }
            }

            // End call button
            CallActionButton(
                icon = { Icon(Icons.Rounded.CallEnd, "End call", tint = Color.White, modifier = Modifier.size(28.dp)) },
                color = Color(0xFFE53935),
                label = "End call",
                onClick = onEnd,
                modifier = Modifier.padding(bottom = 48.dp),
            )
        }
    }
}

// ── Shared call UI components ─────────────────────────────────────────────────

@Composable
private fun CallActionButton(
    icon: @Composable () -> Unit,
    color: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(68.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.8f)),
        )
    }
}

@Composable
private fun CallControlButton(
    icon: @Composable () -> Unit,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (active) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { icon() }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.65f)),
        )
    }
}

// ── Message bubbles ───────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: MessageEntity) {
    val isFromMe = message.isFromMe
    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isFromMe) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isFromMe) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isFromMe) 16.dp else 4.dp,
        bottomEnd = if (isFromMe) 4.dp else 16.dp,
    )

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        contentAlignment = alignment,
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            modifier = Modifier.widthIn(min = 60.dp, max = 280.dp),
            shadowElevation = 0.5.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.content,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        color = textColor.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    )
                    if (isFromMe) {
                        Spacer(Modifier.width(4.dp))
                        val statusColor = when (message.status) {
                            "read" -> Color(0xFF53BDEB)
                            "delivered" -> textColor.copy(alpha = 0.85f)
                            else -> textColor.copy(alpha = 0.45f)
                        }
                        Text("✓", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = statusColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallLogBubble(message: MessageEntity) {
    // content format: "callType|status|direction|durationSeconds"
    val parts = message.content.split("|")
    val callType = parts.getOrNull(0) ?: "voice"
    val status = parts.getOrNull(1) ?: "unknown"
    val direction = parts.getOrNull(2) ?: "incoming"
    val duration = parts.getOrNull(3)?.toIntOrNull() ?: 0

    val isVideo = callType == WsMessage.CALL_TYPE_VIDEO
    val isMissed = status == "missed" || status == "no_answer"
    val isOutgoing = direction == "outgoing"

    val icon = if (isMissed) Icons.Rounded.PhoneMissed
               else if (isVideo) Icons.Rounded.Videocam
               else Icons.Rounded.Phone
    val iconTint = if (isMissed) MaterialTheme.colorScheme.error
                   else MaterialTheme.colorScheme.primary

    val label = buildString {
        if (isOutgoing) append("Outgoing ") else append("Incoming ")
        append(if (isVideo) "video call" else "voice call")
        if (isMissed) {
            if (!isOutgoing) append(" · Missed")
        } else if (duration > 0) {
            append(" · ${formatCallDuration(duration)}")
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isMissed) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

// ── Formatters ────────────────────────────────────────────────────────────────

private fun formatTimestamp(epochMillis: Long): String {
    return try {
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis)
        val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
        val local = instant.toLocalDateTime(tz)
        val hour = local.hour.toString().padStart(2, '0')
        val minute = local.minute.toString().padStart(2, '0')
        "$hour:$minute"
    } catch (e: Exception) { "" }
}

private fun formatCallDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}
