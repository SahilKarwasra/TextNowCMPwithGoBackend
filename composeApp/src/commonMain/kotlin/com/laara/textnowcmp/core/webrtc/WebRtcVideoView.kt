package com.laara.textnowcmp.core.webrtc

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific composable that renders a live WebRTC video track from
 * the given [peerConnection]. When [isLocal] is true, renders the local camera
 * preview; when false, renders the remote peer's video.
 */
@Composable
expect fun WebRtcVideoView(
    peerConnection: WebRtcPeerConnection,
    isLocal: Boolean,
    modifier: Modifier = Modifier,
)
