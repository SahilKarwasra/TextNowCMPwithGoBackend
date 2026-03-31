package com.laara.textnowcmp.core.webrtc

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS actual for WebRTC video rendering.
 * TODO: Implement using UIKitView with RTCMTLVideoView once iOS build compiles.
 * For now, this is a no-op placeholder.
 */
@Composable
actual fun WebRtcVideoView(
    peerConnection: WebRtcPeerConnection,
    isLocal: Boolean,
    modifier: Modifier,
) {
    // iOS video rendering will be implemented after delegate conflict is resolved
}
