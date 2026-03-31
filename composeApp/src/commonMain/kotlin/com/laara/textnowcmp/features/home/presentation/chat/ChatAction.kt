package com.laara.textnowcmp.features.home.presentation.chat

sealed interface ChatAction {
    data class OnInputChange(val text: String) : ChatAction
    data object OnSendClick : ChatAction
    data object OnBackClick : ChatAction
    // Call actions
    data object OnVoiceCallClick : ChatAction
    data object OnVideoCallClick : ChatAction
    data object OnCallAnswer : ChatAction
    data object OnCallDecline : ChatAction
    data object OnCallEnd : ChatAction
    data object OnCallMuteToggle : ChatAction
    data object OnCallSpeakerToggle : ChatAction
    data object OnCallCameraToggle : ChatAction
    // Permission callbacks
    data object OnCallPermissionsGranted : ChatAction
    data object OnCallPermissionsDenied : ChatAction
}
