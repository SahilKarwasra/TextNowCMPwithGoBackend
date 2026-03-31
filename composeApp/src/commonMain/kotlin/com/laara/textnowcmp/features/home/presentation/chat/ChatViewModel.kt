package com.laara.textnowcmp.features.home.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laara.textnowcmp.core.util.TokenProvider
import com.laara.textnowcmp.core.util.ui.UiEvent
import com.laara.textnowcmp.core.util.ui.UiEventController
import com.laara.textnowcmp.core.webrtc.WebRtcPeerConnection
import com.laara.textnowcmp.core.webrtc.WebRtcPeerConnectionFactory
import com.laara.textnowcmp.features.chat.data.remote.dto.WsMessage
import com.laara.textnowcmp.features.chat.domain.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val tokenProvider: TokenProvider,
    private val webRtcFactory: WebRtcPeerConnectionFactory,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: ""
    private val recipientName: String = savedStateHandle.get<String>("recipientName") ?: ""

    private val myUserId: String by lazy {
        tokenProvider.getAccessToken()?.let { token ->
            try {
                val parts = token.split(".")
                if (parts.size >= 2) {
                    val payload = parts[1]
                    val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
                    val decoded = kotlin.io.encoding.Base64.decode(padded)
                    val json = decoded.decodeToString()
                    val regex = """"(?:userId|sub)"\s*:\s*"([^"]+)"""".toRegex()
                    regex.find(json)?.groupValues?.get(1) ?: ""
                } else ""
            } catch (e: Exception) { "" }
        } ?: ""
    }

    private var recipientUserId: String = ""
    private var hasLoadedInitialData = false
    private var typingJob: Job? = null
    private var callTimerJob: Job? = null
    private var isTypingSent = false

    // WebRTC peer connection — created per call, destroyed when call ends
    /** Exposed for video rendering composables. */
    var peerConnection: WebRtcPeerConnection? = null
        private set

    // Buffer for SDP/ICE messages that arrive before peerConnection is ready
    private val pendingSignalingMessages = mutableListOf<WsMessage>()

    private val _state = MutableStateFlow(
        ChatState(conversationId = conversationId, recipientName = recipientName)
    )
    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                hasLoadedInitialData = true
                resolveRecipient()
                loadMessages()
                observeIncoming()
                chatRepository.clearUnread(conversationId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ChatState(conversationId = conversationId, recipientName = recipientName)
        )

    private val _events = Channel<ChatEvent>()
    val events = _events.receiveAsFlow()

    private fun resolveRecipient() {
        viewModelScope.launch {
            recipientUserId = chatRepository.getRecipientUserId(conversationId) ?: ""
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            chatRepository.observeMessages(conversationId).collect { messages ->
                _state.update { it.copy(messages = messages, isLoading = false) }
            }
        }
    }

    private fun observeIncoming() {
        viewModelScope.launch {
            chatRepository.observeIncoming().collect { message ->
                when (message.type) {
                    // ── Chat messages ──────────────────────────────────────────
                    WsMessage.TYPE_DIRECT_MESSAGE -> {
                        if (message.conversationId == conversationId) {
                            chatRepository.handleIncomingMessage(message, myUserId)
                            val msgId = message.messageId
                            val senderId = message.senderId
                            if (msgId != null && senderId != null && senderId != myUserId) {
                                chatRepository.sendReadReceipt(msgId, senderId)
                            }
                            chatRepository.clearUnread(conversationId)
                        }
                    }
                    WsMessage.TYPE_ACK,
                    WsMessage.TYPE_DELIVERED,
                    WsMessage.TYPE_READ -> {
                        chatRepository.handleIncomingMessage(message, myUserId)
                    }
                    WsMessage.TYPE_TYPING -> {
                        if (message.conversationId == conversationId ||
                            message.senderId == recipientUserId) {
                            _state.update { it.copy(isTyping = true) }
                        }
                    }
                    WsMessage.TYPE_STOP_TYPING -> {
                        if (message.conversationId == conversationId ||
                            message.senderId == recipientUserId) {
                            _state.update { it.copy(isTyping = false) }
                        }
                    }

                    // ── Call signaling ─────────────────────────────────────────
                    WsMessage.TYPE_CALL_INITIATE -> {
                        val isFromPeer = message.senderId == recipientUserId ||
                            message.conversationId == conversationId
                        if (!isFromPeer) return@collect
                        val callId = message.callId ?: return@collect
                        _state.update {
                            it.copy(
                                callScreenState = CallScreenState.Incoming(
                                    callId = callId,
                                    callType = message.callType ?: WsMessage.CALL_TYPE_VOICE,
                                    callerName = message.senderName ?: it.recipientName,
                                    callerPhone = message.senderPhone ?: "",
                                )
                            )
                        }
                    }

                    WsMessage.TYPE_CALL_RINGING -> {
                        val callId = message.callId ?: return@collect
                        val current = _state.value.callScreenState
                        if (current is CallScreenState.Outgoing) {
                            _state.update {
                                it.copy(callScreenState = current.copy(callId = callId))
                            }
                        }
                    }

                    WsMessage.TYPE_CALL_ANSWER -> {
                        // Remote peer answered our outgoing call → go Active + start WebRTC
                        val current = _state.value.callScreenState
                        if (current is CallScreenState.Outgoing) {
                            val callId = current.callId
                            _state.update {
                                it.copy(
                                    callScreenState = CallScreenState.Active(
                                        callId = callId,
                                        callType = current.callType,
                                        recipientName = current.recipientName,
                                    )
                                )
                            }
                            startCallTimer(callId)
                            // Caller initiates WebRTC: create offer → send SDP
                            startWebRtc(callId, current.callType, isCaller = true)
                        }
                    }

                    // ── WebRTC signaling ───────────────────────────────────────
                    WsMessage.TYPE_CALL_SDP_OFFER -> {
                        val sdp = message.sdpData ?: return@collect
                        val callId = message.callId ?: return@collect
                        println("[WebRTC] Received SDP offer (callId=$callId, pc=${peerConnection != null})")
                        val pc = peerConnection
                        if (pc == null) {
                            println("[WebRTC] PeerConnection not ready, buffering SDP offer")
                            pendingSignalingMessages.add(message)
                            return@collect
                        }
                        viewModelScope.launch {
                            try {
                                pc.setRemoteDescription("offer", sdp)
                                println("[WebRTC] Set remote SDP offer")
                                val answerSdp = pc.createAnswer()
                                pc.setLocalDescription("answer", answerSdp)
                                println("[WebRTC] Created and set local SDP answer")
                                chatRepository.sendCallSignal(
                                    WsMessage(
                                        type = WsMessage.TYPE_CALL_SDP_ANSWER,
                                        callId = callId,
                                        sdpData = answerSdp,
                                    )
                                )
                                println("[WebRTC] Sent SDP answer")
                            } catch (e: Exception) {
                                println("[WebRTC] SDP offer handling failed: ${e.message}")
                            }
                        }
                    }

                    WsMessage.TYPE_CALL_SDP_ANSWER -> {
                        val sdp = message.sdpData ?: return@collect
                        println("[WebRTC] Received SDP answer (pc=${peerConnection != null})")
                        val pc = peerConnection
                        if (pc == null) {
                            println("[WebRTC] PeerConnection not ready, buffering SDP answer")
                            pendingSignalingMessages.add(message)
                            return@collect
                        }
                        viewModelScope.launch {
                            try {
                                pc.setRemoteDescription("answer", sdp)
                                println("[WebRTC] Set remote SDP answer — connection establishing")
                            } catch (e: Exception) {
                                println("[WebRTC] SDP answer handling failed: ${e.message}")
                            }
                        }
                    }

                    WsMessage.TYPE_CALL_ICE -> {
                        val candidate = message.iceCandidate ?: return@collect
                        println("[WebRTC] Received ICE candidate (pc=${peerConnection != null})")
                        val pc = peerConnection
                        if (pc == null) {
                            println("[WebRTC] PeerConnection not ready, buffering ICE candidate")
                            pendingSignalingMessages.add(message)
                            return@collect
                        }
                        viewModelScope.launch {
                            try {
                                pc.addIceCandidate(candidate)
                                println("[WebRTC] Added ICE candidate")
                            } catch (e: Exception) {
                                println("[WebRTC] ICE candidate failed: ${e.message}")
                            }
                        }
                    }

                    WsMessage.TYPE_CALL_DECLINE -> {
                        stopCallTimer()
                        destroyWebRtc()
                        _state.update { it.copy(callScreenState = CallScreenState.Idle) }
                        showCallToast("Call declined")
                    }

                    WsMessage.TYPE_CALL_END -> {
                        stopCallTimer()
                        destroyWebRtc()
                        _state.update { it.copy(callScreenState = CallScreenState.Idle) }
                    }

                    WsMessage.TYPE_CALL_BUSY -> {
                        stopCallTimer()
                        destroyWebRtc()
                        _state.update { it.copy(callScreenState = CallScreenState.Idle) }
                        showCallToast("User is on another call")
                    }

                    WsMessage.TYPE_CALL_NO_ANSWER -> {
                        stopCallTimer()
                        destroyWebRtc()
                        _state.update { it.copy(callScreenState = CallScreenState.Idle) }
                        showCallToast("No answer")
                    }

                    WsMessage.TYPE_CALL_MISSED -> {
                        _state.update { it.copy(callScreenState = CallScreenState.Idle) }
                    }

                    WsMessage.TYPE_CALL_LOG -> {
                        chatRepository.handleIncomingMessage(message, myUserId)
                        stopCallTimer()
                        destroyWebRtc()
                        _state.update { it.copy(callScreenState = CallScreenState.Idle) }
                    }
                }
            }
        }
    }

    // ── WebRTC lifecycle ────────────────────────────────────────────────────

    private fun startWebRtc(callId: String, callType: String, isCaller: Boolean) {
        val isVideo = callType == WsMessage.CALL_TYPE_VIDEO
        val pc = webRtcFactory.create(isVideo)

        // Wire ICE candidate callback → relay via WebSocket
        pc.onIceCandidate = { candidateJson ->
            viewModelScope.launch {
                chatRepository.sendCallSignal(
                    WsMessage(
                        type = WsMessage.TYPE_CALL_ICE,
                        callId = callId,
                        iceCandidate = candidateJson,
                    )
                )
            }
        }

        pc.onConnectionStateChanged = { state ->
            println("[WebRTC] Connection state: $state")
            if (state == "failed" || state == "disconnected") {
                // Could auto-reconnect or show UI feedback
                println("[WebRTC] Connection $state — media may be interrupted")
            }
        }

        pc.onRemoteTrackReady = {
            _state.update { it.copy(remoteVideoReady = true) }
        }

        peerConnection = pc
        println("[WebRTC] PeerConnection created (isCaller=$isCaller, isVideo=$isVideo)")

        // Replay any buffered signaling messages
        replayPendingSignaling()

        // Apply current mute/speaker/camera state
        pc.setAudioEnabled(!_state.value.isMuted)
        pc.setVideoEnabled(_state.value.isCameraOn)
        if (_state.value.isSpeakerOn || isVideo) {
            pc.setSpeakerOn(true)
        }

        if (isCaller) {
            // Caller creates the SDP offer
            viewModelScope.launch {
                try {
                    val offerSdp = pc.createOffer()
                    pc.setLocalDescription("offer", offerSdp)
                    chatRepository.sendCallSignal(
                        WsMessage(
                            type = WsMessage.TYPE_CALL_SDP_OFFER,
                            callId = callId,
                            sdpData = offerSdp,
                        )
                    )
                    println("[WebRTC] Sent SDP offer")
                } catch (e: Exception) {
                    println("[WebRTC] Failed to create offer: ${e.message}")
                }
            }
        }
    }

    private fun destroyWebRtc() {
        peerConnection?.close()
        peerConnection = null
        pendingSignalingMessages.clear()
        _state.update { it.copy(remoteVideoReady = false) }
    }

    /**
     * Replay any SDP/ICE messages that arrived before the PeerConnection was ready.
     */
    private fun replayPendingSignaling() {
        if (pendingSignalingMessages.isEmpty()) return
        println("[WebRTC] Replaying ${pendingSignalingMessages.size} buffered signaling messages")
        val buffered = pendingSignalingMessages.toList()
        pendingSignalingMessages.clear()
        for (msg in buffered) {
            when (msg.type) {
                WsMessage.TYPE_CALL_SDP_OFFER -> {
                    val sdp = msg.sdpData ?: continue
                    val callId = msg.callId ?: continue
                    val pc = peerConnection ?: continue
                    viewModelScope.launch {
                        try {
                            pc.setRemoteDescription("offer", sdp)
                            val answerSdp = pc.createAnswer()
                            pc.setLocalDescription("answer", answerSdp)
                            chatRepository.sendCallSignal(
                                WsMessage(
                                    type = WsMessage.TYPE_CALL_SDP_ANSWER,
                                    callId = callId,
                                    sdpData = answerSdp,
                                )
                            )
                            println("[WebRTC] Replayed SDP offer → sent answer")
                        } catch (e: Exception) {
                            println("[WebRTC] Replay SDP offer failed: ${e.message}")
                        }
                    }
                }
                WsMessage.TYPE_CALL_SDP_ANSWER -> {
                    val sdp = msg.sdpData ?: continue
                    val pc = peerConnection ?: continue
                    viewModelScope.launch {
                        try {
                            pc.setRemoteDescription("answer", sdp)
                            println("[WebRTC] Replayed SDP answer")
                        } catch (e: Exception) {
                            println("[WebRTC] Replay SDP answer failed: ${e.message}")
                        }
                    }
                }
                WsMessage.TYPE_CALL_ICE -> {
                    val candidate = msg.iceCandidate ?: continue
                    val pc = peerConnection ?: continue
                    viewModelScope.launch {
                        try {
                            pc.addIceCandidate(candidate)
                            println("[WebRTC] Replayed ICE candidate")
                        } catch (e: Exception) {
                            println("[WebRTC] Replay ICE failed: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    // ── Call initiation ─────────────────────────────────────────────────────

    @OptIn(ExperimentalUuidApi::class)
    private fun initiateCall(callType: String) {
        if (recipientUserId.isBlank()) return
        if (_state.value.callScreenState !is CallScreenState.Idle) return

        _state.update {
            it.copy(
                callScreenState = CallScreenState.Outgoing(
                    callId = "",
                    callType = callType,
                    recipientName = it.recipientName,
                )
            )
        }

        viewModelScope.launch {
            chatRepository.sendCallSignal(
                WsMessage(
                    type = WsMessage.TYPE_CALL_INITIATE,
                    messageId = Uuid.random().toString(),
                    recipientId = recipientUserId,
                    callType = callType,
                    conversationId = conversationId,
                )
            )
        }
    }

    private fun answerCall() {
        val incoming = _state.value.callScreenState as? CallScreenState.Incoming ?: return
        _state.update {
            it.copy(
                callScreenState = CallScreenState.Active(
                    callId = incoming.callId,
                    callType = incoming.callType,
                    recipientName = incoming.callerName,
                )
            )
        }
        startCallTimer(incoming.callId)

        // Callee answers → tell server
        viewModelScope.launch {
            chatRepository.sendCallSignal(
                WsMessage(
                    type = WsMessage.TYPE_CALL_ANSWER,
                    callId = incoming.callId,
                )
            )
        }

        // Callee starts WebRTC (will receive SDP offer from caller)
        startWebRtc(incoming.callId, incoming.callType, isCaller = false)
    }

    private fun declineCall() {
        val incoming = _state.value.callScreenState as? CallScreenState.Incoming ?: return
        _state.update { it.copy(callScreenState = CallScreenState.Idle) }
        viewModelScope.launch {
            chatRepository.sendCallSignal(
                WsMessage(
                    type = WsMessage.TYPE_CALL_DECLINE,
                    callId = incoming.callId,
                )
            )
        }
    }

    private fun endCall() {
        val callId = when (val cs = _state.value.callScreenState) {
            is CallScreenState.Outgoing -> cs.callId
            is CallScreenState.Active -> cs.callId
            else -> null
        }
        stopCallTimer()
        destroyWebRtc()
        _state.update { it.copy(callScreenState = CallScreenState.Idle) }
        if (!callId.isNullOrBlank()) {
            viewModelScope.launch {
                chatRepository.sendCallSignal(
                    WsMessage(
                        type = WsMessage.TYPE_CALL_END,
                        callId = callId,
                    )
                )
            }
        }
    }

    // ── Call timer ──────────────────────────────────────────────────────────

    private fun startCallTimer(callId: String) {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val cs = _state.value.callScreenState
                if (cs is CallScreenState.Active && cs.callId == callId) {
                    _state.update {
                        it.copy(callScreenState = cs.copy(durationSeconds = cs.durationSeconds + 1))
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun stopCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = null
    }

    private fun showCallToast(message: String) {
        viewModelScope.launch {
            UiEventController.send(UiEvent.Snackbar(message = message))
        }
    }

    // ── Typing helpers ──────────────────────────────────────────────────────

    private fun sendTyping(isTyping: Boolean) {
        if (recipientUserId.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendTypingIndicator(conversationId, recipientUserId, isTyping)
        }
    }

    // ── Action dispatcher ───────────────────────────────────────────────────

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.OnInputChange -> {
                _state.update { it.copy(inputText = action.text) }
                if (action.text.isNotBlank()) {
                    if (!isTypingSent) {
                        sendTyping(true)
                        isTypingSent = true
                    }
                    typingJob?.cancel()
                    typingJob = viewModelScope.launch {
                        delay(2_000L)
                        sendTyping(false)
                        isTypingSent = false
                    }
                } else {
                    typingJob?.cancel()
                    if (isTypingSent) {
                        sendTyping(false)
                        isTypingSent = false
                    }
                }
            }

            ChatAction.OnSendClick -> {
                val text = _state.value.inputText.trim()
                if (text.isBlank()) return
                _state.update { it.copy(inputText = "") }
                typingJob?.cancel()
                if (isTypingSent) {
                    sendTyping(false)
                    isTypingSent = false
                }
                viewModelScope.launch {
                    chatRepository.sendMessage(conversationId, text, myUserId)
                }
            }

            ChatAction.OnBackClick -> {
                typingJob?.cancel()
                if (isTypingSent) {
                    sendTyping(false)
                    isTypingSent = false
                }
                viewModelScope.launch { _events.send(ChatEvent.NavigateBack) }
            }

            ChatAction.OnVoiceCallClick -> {
                _state.update { it.copy(pendingCallType = WsMessage.CALL_TYPE_VOICE) }
            }
            ChatAction.OnVideoCallClick -> {
                _state.update { it.copy(pendingCallType = WsMessage.CALL_TYPE_VIDEO) }
            }
            ChatAction.OnCallPermissionsGranted -> {
                val callType = _state.value.pendingCallType ?: return
                _state.update { it.copy(pendingCallType = null) }
                initiateCall(callType)
            }
            ChatAction.OnCallPermissionsDenied -> {
                _state.update { it.copy(pendingCallType = null) }
                showCallToast("Camera/mic permission required for calls")
            }
            ChatAction.OnCallAnswer -> answerCall()
            ChatAction.OnCallDecline -> declineCall()
            ChatAction.OnCallEnd -> endCall()

            ChatAction.OnCallMuteToggle -> {
                val newMuted = !_state.value.isMuted
                _state.update { it.copy(isMuted = newMuted) }
                peerConnection?.setAudioEnabled(!newMuted)
            }

            ChatAction.OnCallSpeakerToggle -> {
                val newSpeaker = !_state.value.isSpeakerOn
                _state.update { it.copy(isSpeakerOn = newSpeaker) }
                peerConnection?.setSpeakerOn(newSpeaker)
            }

            ChatAction.OnCallCameraToggle -> {
                val newCamera = !_state.value.isCameraOn
                _state.update { it.copy(isCameraOn = newCamera) }
                peerConnection?.setVideoEnabled(newCamera)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
        callTimerJob?.cancel()
        destroyWebRtc()
    }
}
