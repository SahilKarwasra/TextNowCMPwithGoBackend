package com.laara.textnowcmp.core.webrtc

import cocoapods.WebRTC_SDK.RTCDefaultVideoDecoderFactory
import cocoapods.WebRTC_SDK.RTCDefaultVideoEncoderFactory
import cocoapods.WebRTC_SDK.RTCPeerConnectionFactory
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class WebRtcPeerConnectionFactory {

    private val factory: RTCPeerConnectionFactory

    init {
        RTCPeerConnectionFactory.initialize()
        val encoderFactory = RTCDefaultVideoEncoderFactory()
        val decoderFactory = RTCDefaultVideoDecoderFactory()
        factory = RTCPeerConnectionFactory(
            encoderFactory = encoderFactory,
            decoderFactory = decoderFactory,
        )
    }

    actual fun create(isVideo: Boolean): WebRtcPeerConnection {
        return WebRtcPeerConnection(
            factory = factory,
            isVideo = isVideo,
        )
    }
}
