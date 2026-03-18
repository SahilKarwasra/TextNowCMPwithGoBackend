package com.laara.textnowcmp.features.home.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laara.textnowcmp.core.util.TokenProvider
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

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val tokenProvider: TokenProvider,
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
    private var isTypingSent = false

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
                    WsMessage.TYPE_DIRECT_MESSAGE -> {
                        if (message.conversationId == conversationId) {
                            chatRepository.handleIncomingMessage(message, myUserId)
                            // Send read receipt immediately since user is viewing this chat
                            val msgId = message.messageId
                            val senderId = message.senderId
                            if (msgId != null && senderId != null && senderId != myUserId) {
                                chatRepository.sendReadReceipt(msgId, senderId)
                            }
                            // Keep unread at 0 since user is actively viewing
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
                }
            }
        }
    }

    private fun sendTyping(isTyping: Boolean) {
        if (recipientUserId.isBlank()) return
        viewModelScope.launch {
            chatRepository.sendTypingIndicator(conversationId, recipientUserId, isTyping)
        }
    }

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
                        delay(2000) // 2 seconds of no typing → stop
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
                viewModelScope.launch {
                    _events.send(ChatEvent.NavigateBack)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
    }
}