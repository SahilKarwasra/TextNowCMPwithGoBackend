package com.laara.textnowcmp.core.webrtc

import cocoapods.WebRTC_SDK.RTCAudioSession
import cocoapods.WebRTC_SDK.RTCAudioSource
import cocoapods.WebRTC_SDK.RTCAudioTrack
import cocoapods.WebRTC_SDK.RTCCameraVideoCapturer
import cocoapods.WebRTC_SDK.RTCConfiguration
import cocoapods.WebRTC_SDK.RTCDataChannel
import cocoapods.WebRTC_SDK.RTCIceCandidate
import cocoapods.WebRTC_SDK.RTCIceConnectionState
import cocoapods.WebRTC_SDK.RTCIceGatheringState
import cocoapods.WebRTC_SDK.RTCIceServer
import cocoapods.WebRTC_SDK.RTCMediaConstraints
import cocoapods.WebRTC_SDK.RTCMediaStream
import cocoapods.WebRTC_SDK.RTCPeerConnection
import cocoapods.WebRTC_SDK.RTCPeerConnectionDelegateProtocol
import cocoapods.WebRTC_SDK.RTCPeerConnectionFactory
import cocoapods.WebRTC_SDK.RTCPeerConnectionState
import cocoapods.WebRTC_SDK.RTCSessionDescription
import cocoapods.WebRTC_SDK.RTCSignalingState
import cocoapods.WebRTC_SDK.RTCSdpType
import cocoapods.WebRTC_SDK.RTCVideoSource
import cocoapods.WebRTC_SDK.RTCVideoTrack
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionModeVoiceChat
import platform.AVFAudio.AVAudioSessionPortOverrideSpeaker
import platform.AVFAudio.AVAudioSessionPortOverrideNone
import platform.AVFAudio.setActive
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.position
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual class WebRtcPeerConnection(
    private val factory: RTCPeerConnectionFactory,
    private val isVideo: Boolean,
) {
    actual var onIceCandidate: ((String) -> Unit)? = null
    actual var onConnectionStateChanged: ((String) -> Unit)? = null
    actual var onRemoteTrackReady: (() -> Unit)? = null

    private val iceServers = listOf(
        RTCIceServer(uRLStrings = listOf("stun:stun.l.google.com:19302")),
        RTCIceServer(uRLStrings = listOf("stun:stun1.l.google.com:19302")),
        // TURN relay servers for NAT traversal
        RTCIceServer(
            uRLStrings = listOf("turn:openrelay.metered.ca:80"),
            username = "openrelayproject",
            credential = "openrelayproject",
        ),
        RTCIceServer(
            uRLStrings = listOf("turn:openrelay.metered.ca:443"),
            username = "openrelayproject",
            credential = "openrelayproject",
        ),
        RTCIceServer(
            uRLStrings = listOf("turn:openrelay.metered.ca:443?transport=tcp"),
            username = "openrelayproject",
            credential = "openrelayproject",
        ),
    )

    private val audioSession = AVAudioSession.sharedInstance()

    // Media
    private val audioSource: RTCAudioSource = factory.audioSourceWithConstraints(null)
    private val localAudioTrack: RTCAudioTrack = factory.audioTrackWithSource(audioSource, trackId = "audio0")

    private var videoCapturer: RTCCameraVideoCapturer? = null
    private var videoSource: RTCVideoSource? = null
    private var localVideoTrack: RTCVideoTrack? = null
    private var remoteVideoTrack: RTCVideoTrack? = null

    private val delegate = PeerConnectionDelegate(
        onIce = { candidate ->
            val json = """{"sdpMLineIndex":${candidate.sdpMLineIndex},"sdpMid":"${candidate.sdpMid ?: ""}","candidate":"${candidate.sdp}"}"""
            onIceCandidate?.invoke(json)
        },
        onStateChanged = { state ->
            onConnectionStateChanged?.invoke(state)
        }
    )

    private val peerConnection: RTCPeerConnection

    init {
        // Configure audio session
        try {
            audioSession.setCategory(
                AVAudioSessionCategoryPlayAndRecord,
                withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker or AVAudioSessionCategoryOptionAllowBluetooth,
                error = null
            )
            audioSession.setMode(AVAudioSessionModeVoiceChat, error = null)
            audioSession.setActive(true, error = null)
        } catch (_: Exception) {}

        if (isVideo) {
            videoSource = factory.videoSource()
            videoCapturer = RTCCameraVideoCapturer(delegate = videoSource!!)
            startCamera()
            localVideoTrack = factory.videoTrackWithSource(videoSource!!, trackId = "video0")
        }

        val config = RTCConfiguration()
        // ICE servers and SDP semantics are set via the configuration object

        @Suppress("UNCHECKED_CAST")
        peerConnection = factory.peerConnectionWithConfiguration(
            config,
            constraints = RTCMediaConstraints(
                mandatoryConstraints = null,
                optionalConstraints = null,
            ),
            delegate = delegate as RTCPeerConnectionDelegateProtocol,
        )!!

        // Add tracks
        peerConnection.addTrack(localAudioTrack, streamIds = listOf("stream0"))
        localVideoTrack?.let { peerConnection.addTrack(it, streamIds = listOf("stream0")) }
    }

    actual suspend fun createOffer(): String = suspendCancellableCoroutine { cont ->
        val constraints = RTCMediaConstraints(
            mandatoryConstraints = mapOf(
                "OfferToReceiveAudio" to "true",
                "OfferToReceiveVideo" to isVideo.toString(),
            ),
            optionalConstraints = null,
        )
        peerConnection.offerForConstraints(constraints) { sdp, error ->
            if (sdp != null) {
                cont.resume(sdp.sdp)
            } else {
                cont.resumeWithException(Exception("createOffer failed: ${error?.localizedDescription}"))
            }
        }
    }

    actual suspend fun createAnswer(): String = suspendCancellableCoroutine { cont ->
        val constraints = RTCMediaConstraints(
            mandatoryConstraints = mapOf(
                "OfferToReceiveAudio" to "true",
                "OfferToReceiveVideo" to isVideo.toString(),
            ),
            optionalConstraints = null,
        )
        peerConnection.answerForConstraints(constraints) { sdp, error ->
            if (sdp != null) {
                cont.resume(sdp.sdp)
            } else {
                cont.resumeWithException(Exception("createAnswer failed: ${error?.localizedDescription}"))
            }
        }
    }

    actual suspend fun setLocalDescription(type: String, sdp: String): Unit =
        suspendCancellableCoroutine { cont ->
            val sdpType = if (type == "offer") RTCSdpType.RTCSdpTypeOffer else RTCSdpType.RTCSdpTypeAnswer
            val sessionDesc = RTCSessionDescription(type = sdpType, sdp = sdp)
            peerConnection.setLocalDescription(sessionDesc) { error ->
                if (error == null) {
                    cont.resume(Unit)
                } else {
                    cont.resumeWithException(Exception("setLocal failed: ${error.localizedDescription}"))
                }
            }
        }

    actual suspend fun setRemoteDescription(type: String, sdp: String): Unit =
        suspendCancellableCoroutine { cont ->
            val sdpType = if (type == "offer") RTCSdpType.RTCSdpTypeOffer else RTCSdpType.RTCSdpTypeAnswer
            val sessionDesc = RTCSessionDescription(type = sdpType, sdp = sdp)
            peerConnection.setRemoteDescription(sessionDesc) { error ->
                if (error == null) {
                    cont.resume(Unit)
                } else {
                    cont.resumeWithException(Exception("setRemote failed: ${error.localizedDescription}"))
                }
            }
        }

    actual suspend fun addIceCandidate(candidateJson: String) {
        try {
            val json = Json.parseToJsonElement(candidateJson) as JsonObject
            val sdpMid = json["sdpMid"]?.jsonPrimitive?.content ?: ""
            val sdpMLineIndex = json["sdpMLineIndex"]?.jsonPrimitive?.intOrNull ?: 0
            val candidate = json["candidate"]?.jsonPrimitive?.content ?: ""
            val iceCandidate = RTCIceCandidate(sdp = candidate, sdpMLineIndex = sdpMLineIndex.toInt(), sdpMid = sdpMid)
            peerConnection.addIceCandidate(iceCandidate)
        } catch (e: Exception) {
            println("[WebRTC] Failed to add ICE candidate: ${e.message}")
        }
    }

    actual fun getLocalVideoTrackNative(): Any? = localVideoTrack
    actual fun getRemoteVideoTrackNative(): Any? = remoteVideoTrack

    actual fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack.setIsEnabled(enabled)
    }

    actual fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setIsEnabled(enabled)
    }

    actual fun setSpeakerOn(enabled: Boolean) {
        try {
            audioSession.overrideOutputAudioPort(
                if (enabled) AVAudioSessionPortOverrideSpeaker else AVAudioSessionPortOverrideNone,
                error = null,
            )
        } catch (_: Exception) {}
    }

    actual fun switchCamera() {
        // On iOS, switching camera requires stopping and restarting with a different device
        val capturer = videoCapturer ?: return
        @Suppress("UNCHECKED_CAST")
        val devices = RTCCameraVideoCapturer.captureDevices() as? List<AVCaptureDevice> ?: return
        if (devices.size < 2) return

        // Find next device (toggle front/back)
        val nextDevice = devices.firstOrNull { device ->
            device.position != (devices.firstOrNull()?.position ?: AVCaptureDevicePositionFront)
        } ?: return

        capturer.startCaptureWithDevice(nextDevice, format = nextDevice.activeFormat, fps = 30)
    }

    actual fun close() {
        try {
            videoCapturer?.stopCapture()
            localAudioTrack.setIsEnabled(false)
            localVideoTrack?.setIsEnabled(false)
            peerConnection.close()
            audioSession.setActive(false, error = null)
        } catch (e: Exception) {
            println("[WebRTC] Close error: ${e.message}")
        }
    }

    private fun startCamera() {
        val capturer = videoCapturer ?: return
        @Suppress("UNCHECKED_CAST")
        val devices = RTCCameraVideoCapturer.captureDevices() as? List<AVCaptureDevice> ?: return
        // Prefer front camera
        val device = devices.firstOrNull { it.position == AVCaptureDevicePositionFront }
            ?: devices.firstOrNull() ?: return

        capturer.startCaptureWithDevice(device, format = device.activeFormat, fps = 30)
    }
}
// RTCPeerConnectionDelegate for iOS
// NOTE: We cannot directly implement RTCPeerConnectionDelegateProtocol due to
// Kotlin/Native ObjC interop limitations with conflicting overloaded selectors.
// The ObjC runtime uses duck typing — matching methods by selector name — so
// this still works correctly as a delegate without typed protocol conformance.
@OptIn(ExperimentalForeignApi::class)
private class PeerConnectionDelegate(
    private val onIce: (RTCIceCandidate) -> Unit,
    private val onStateChanged: (String) -> Unit,
) : NSObject() {

    // peerConnection:didGenerateIceCandidate:
    @Suppress("UNUSED_PARAMETER")
    fun peerConnection(peerConnection: RTCPeerConnection, didGenerateIceCandidate: RTCIceCandidate) {
        onIce(didGenerateIceCandidate)
    }

    // peerConnection:didChangeConnectionState:
    @Suppress("UNUSED_PARAMETER")
    fun peerConnection(peerConnection: RTCPeerConnection, didChangeConnectionState: RTCPeerConnectionState) {
        val state = when (didChangeConnectionState) {
            RTCPeerConnectionState.RTCPeerConnectionStateConnected -> "connected"
            RTCPeerConnectionState.RTCPeerConnectionStateDisconnected -> "disconnected"
            RTCPeerConnectionState.RTCPeerConnectionStateFailed -> "failed"
            RTCPeerConnectionState.RTCPeerConnectionStateClosed -> "closed"
            RTCPeerConnectionState.RTCPeerConnectionStateNew -> "new"
            RTCPeerConnectionState.RTCPeerConnectionStateConnecting -> "connecting"
            else -> "unknown"
        }
        onStateChanged(state)
    }

    // peerConnection:didChangeSignalingState:
    @Suppress("UNUSED_PARAMETER")
    fun peerConnection(peerConnection: RTCPeerConnection, didChangeSignalingState: RTCSignalingState) {}

    // peerConnectionShouldNegotiate:
    @Suppress("UNUSED_PARAMETER")
    fun peerConnectionShouldNegotiate(peerConnection: RTCPeerConnection) {}

    // peerConnection:didChangeIceConnectionState:
    @Suppress("UNUSED_PARAMETER")
    fun peerConnection(peerConnection: RTCPeerConnection, didChangeIceConnectionState: RTCIceConnectionState) {}

    // peerConnection:didChangeIceGatheringState:
    @Suppress("UNUSED_PARAMETER")
    fun peerConnection(peerConnection: RTCPeerConnection, didChangeIceGatheringState: RTCIceGatheringState) {}

    // peerConnection:didRemoveIceCandidates:
    @Suppress("UNUSED_PARAMETER")
    fun peerConnection(peerConnection: RTCPeerConnection, didRemoveIceCandidates: List<*>) {}
}
