package com.laara.textnowcmp.core.webrtc

import androidx.compose.runtime.Composable

/**
 * Platform-specific composable that requests camera and microphone permissions
 * before launching the call. When permissions are granted, [onGranted] is invoked.
 * If denied, [onDenied] is invoked.
 */
@Composable
expect fun RequestCallPermissions(
    isVideo: Boolean,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
)
