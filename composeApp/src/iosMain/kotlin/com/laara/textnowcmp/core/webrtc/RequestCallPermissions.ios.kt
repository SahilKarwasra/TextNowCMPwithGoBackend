package com.laara.textnowcmp.core.webrtc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * iOS handles camera/mic permissions via Info.plist keys.
 * The system automatically prompts when WebRTC accesses the camera/mic.
 * We just invoke onGranted immediately; if a user denied at the system level,
 * WebRTC will silently skip that stream.
 */
@Composable
actual fun RequestCallPermissions(
    isVideo: Boolean,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    LaunchedEffect(Unit) {
        // iOS shows the system permission prompt automatically when the
        // camera/mic is accessed by WebRTC. No need for explicit request here.
        onGranted()
    }
}
