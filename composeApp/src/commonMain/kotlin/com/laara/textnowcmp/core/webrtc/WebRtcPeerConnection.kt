package com.laara.textnowcmp.core.webrtc

/**
 * Platform-agnostic WebRTC peer connection abstraction.
 * Each platform provides its own `actual` implementation using native WebRTC APIs.
 *
 * Lifecycle:
 *   1. Create via [WebRtcPeerConnectionFactory.create]
 *   2. Set callbacks: [onIceCandidate], [onConnectionStateChanged]
 *   3. Caller: [createOffer] → [setLocalDescription]
 *   4. Callee: [setRemoteDescription] → [createAnswer] → [setLocalDescription]
 *   5. Both: exchange ICE candidates via [addIceCandidate]
 *   6. Media controls: [setAudioEnabled], [setVideoEnabled], [setSpeakerOn]
 *   7. Cleanup: [close]
 */
expect class WebRtcPeerConnection {

    /** Callback fired when a local ICE candidate is generated. JSON string. */
    var onIceCandidate: ((String) -> Unit)?

    /** Callback fired when connection state changes (e.g. "connected", "disconnected", "failed"). */
    var onConnectionStateChanged: ((String) -> Unit)?

    /** Callback fired when a remote video track is received from the peer. */
    var onRemoteTrackReady: (() -> Unit)?

    /** Returns the platform-native local video track object for rendering. Null for voice-only calls. */
    fun getLocalVideoTrackNative(): Any?

    /** Returns the platform-native remote video track object for rendering. Null until remote track arrives. */
    fun getRemoteVideoTrackNative(): Any?

    /** Create an SDP offer (caller side). Returns the SDP string. */
    suspend fun createOffer(): String

    /** Create an SDP answer (callee side). Returns the SDP string. */
    suspend fun createAnswer(): String

    /** Set the local SDP description (offer or answer). */
    suspend fun setLocalDescription(type: String, sdp: String)

    /** Set the remote SDP description (offer or answer). */
    suspend fun setRemoteDescription(type: String, sdp: String)

    /** Add a remote ICE candidate (JSON string from the other peer). */
    suspend fun addIceCandidate(candidateJson: String)

    /** Enable or disable the local audio track (mute/unmute microphone). */
    fun setAudioEnabled(enabled: Boolean)

    /** Enable or disable the local video track (camera on/off). */
    fun setVideoEnabled(enabled: Boolean)

    /** Route audio to speaker or earpiece. */
    fun setSpeakerOn(enabled: Boolean)

    /** Switch between front and back camera. */
    fun switchCamera()

    /** Release all resources. Must be called when the call ends. */
    fun close()
}
