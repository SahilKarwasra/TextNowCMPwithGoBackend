package com.laara.textnowcmp.core.webrtc

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import org.webrtc.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class WebRtcPeerConnection(
    private val context: Context,
    private val factory: PeerConnectionFactory,
    private val isVideo: Boolean,
) {
    actual var onIceCandidate: ((String) -> Unit)? = null
    actual var onConnectionStateChanged: ((String) -> Unit)? = null
    actual var onRemoteTrackReady: (() -> Unit)? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        // TURN relay servers for NAT traversal (required for emulators and restrictive networks)
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer(),
    )

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Media
    private val audioSource: AudioSource = factory.createAudioSource(MediaConstraints())
    private val localAudioTrack: AudioTrack = factory.createAudioTrack("audio0", audioSource)

    private var videoCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    val eglBase = EglBase.create()

    private val peerConnection: PeerConnection

    init {
        if (isVideo) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            videoSource = factory.createVideoSource(false)
            videoCapturer = createCameraCapturer()
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
            videoCapturer?.startCapture(1280, 720, 30)
            localVideoTrack = factory.createVideoTrack("video0", videoSource)
        }

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val json = """{"sdpMLineIndex":${candidate.sdpMLineIndex},"sdpMid":"${candidate.sdpMid}","candidate":"${candidate.sdp}"}"""
                onIceCandidate?.invoke(json)
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                onConnectionStateChanged?.invoke(newState.name.lowercase())
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                // Capture remote video track
                receiver?.track()?.let { track ->
                    if (track is VideoTrack) {
                        remoteVideoTrack = track
                        println("[WebRTC] Remote video track received")
                        onRemoteTrackReady?.invoke()
                    }
                }
            }
        })!!

        // Add local tracks
        peerConnection.addTrack(localAudioTrack, listOf("stream0"))
        localVideoTrack?.let { peerConnection.addTrack(it, listOf("stream0")) }

        // Enable audio
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    actual suspend fun createOffer(): String = suspendCancellableCoroutine { cont ->
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", isVideo.toString()))
        }
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                cont.resume(sdp.description)
            }
            override fun onCreateFailure(error: String?) {
                cont.resumeWithException(Exception("createOffer failed: $error"))
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    actual suspend fun createAnswer(): String = suspendCancellableCoroutine { cont ->
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", isVideo.toString()))
        }
        peerConnection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                cont.resume(sdp.description)
            }
            override fun onCreateFailure(error: String?) {
                cont.resumeWithException(Exception("createAnswer failed: $error"))
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    actual suspend fun setLocalDescription(type: String, sdp: String): Unit =
        suspendCancellableCoroutine { cont ->
            val sdpType = if (type == "offer") SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
            peerConnection.setLocalDescription(object : SdpObserver {
                override fun onSetSuccess() { cont.resume(Unit) }
                override fun onSetFailure(error: String?) {
                    cont.resumeWithException(Exception("setLocal failed: $error"))
                }
                override fun onCreateSuccess(sdp: SessionDescription?) {}
                override fun onCreateFailure(error: String?) {}
            }, SessionDescription(sdpType, sdp))
        }

    actual suspend fun setRemoteDescription(type: String, sdp: String): Unit =
        suspendCancellableCoroutine { cont ->
            val sdpType = if (type == "offer") SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
            peerConnection.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() { cont.resume(Unit) }
                override fun onSetFailure(error: String?) {
                    cont.resumeWithException(Exception("setRemote failed: $error"))
                }
                override fun onCreateSuccess(sdp: SessionDescription?) {}
                override fun onCreateFailure(error: String?) {}
            }, SessionDescription(sdpType, sdp))
        }

    actual suspend fun addIceCandidate(candidateJson: String) {
        try {
            val json = Json.parseToJsonElement(candidateJson) as JsonObject
            val sdpMid = json["sdpMid"]?.jsonPrimitive?.content ?: ""
            val sdpMLineIndex = json["sdpMLineIndex"]?.jsonPrimitive?.intOrNull ?: 0
            val candidate = json["candidate"]?.jsonPrimitive?.content ?: ""
            peerConnection.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
        } catch (e: Exception) {
            println("[WebRTC] Failed to add ICE candidate: ${e.message}")
        }
    }

    actual fun getLocalVideoTrackNative(): Any? = localVideoTrack
    actual fun getRemoteVideoTrackNative(): Any? = remoteVideoTrack

    actual fun setAudioEnabled(enabled: Boolean) {
        localAudioTrack.setEnabled(enabled)
    }

    actual fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    actual fun setSpeakerOn(enabled: Boolean) {
        audioManager.isSpeakerphoneOn = enabled
    }

    actual fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    actual fun close() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoSource?.dispose()
            surfaceTextureHelper?.dispose()
            localAudioTrack.dispose()
            localVideoTrack?.dispose()
            audioSource.dispose()
            peerConnection.close()
            peerConnection.dispose()
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
        } catch (e: Exception) {
            println("[WebRTC] Close error: ${e.message}")
        }
    }

    private fun createCameraCapturer(): CameraVideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        // Prefer front camera
        for (name in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                return enumerator.createCapturer(name, null)
            }
        }
        // Fallback to any camera
        for (name in enumerator.deviceNames) {
            return enumerator.createCapturer(name, null)
        }
        return null
    }
}
