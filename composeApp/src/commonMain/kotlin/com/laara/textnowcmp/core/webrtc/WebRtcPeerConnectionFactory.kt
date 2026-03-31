package com.laara.textnowcmp.core.webrtc

/**
 * Factory for creating [WebRtcPeerConnection] instances.
 * Platform-specific implementations inject the required native context.
 */
expect class WebRtcPeerConnectionFactory {
    /**
     * Create a new peer connection configured for the given call type.
     * @param isVideo true for video calls, false for voice-only
     */
    fun create(isVideo: Boolean): WebRtcPeerConnection
}
