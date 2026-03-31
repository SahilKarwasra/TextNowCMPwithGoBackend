package com.laara.textnowcmp.core.webrtc

import android.content.Context
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory

actual class WebRtcPeerConnectionFactory(
    private val context: Context,
) {
    private val eglBase = EglBase.create()

    init {
        PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
            .also { PeerConnectionFactory.initialize(it) }
    }

    private val factory: PeerConnectionFactory = PeerConnectionFactory.builder()
        .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
        .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
        .createPeerConnectionFactory()

    actual fun create(isVideo: Boolean): WebRtcPeerConnection {
        return WebRtcPeerConnection(
            context = context,
            factory = factory,
            isVideo = isVideo,
        )
    }
}
