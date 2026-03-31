package com.laara.textnowcmp.core.webrtc

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
actual fun WebRtcVideoView(
    peerConnection: WebRtcPeerConnection,
    isLocal: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val eglContext = remember { peerConnection.eglBase.eglBaseContext }

    val videoTrack = if (isLocal) {
        peerConnection.getLocalVideoTrackNative() as? VideoTrack
    } else {
        peerConnection.getRemoteVideoTrackNative() as? VideoTrack
    }

    val renderer = remember {
        SurfaceViewRenderer(context).apply {
            init(eglContext, null)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            setEnableHardwareScaler(true)
            if (isLocal) setMirror(true) // Mirror front camera
        }
    }

    DisposableEffect(videoTrack) {
        videoTrack?.addSink(renderer)
        onDispose {
            videoTrack?.removeSink(renderer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            renderer.release()
        }
    }

    AndroidView(
        factory = { renderer },
        modifier = modifier,
    )
}
