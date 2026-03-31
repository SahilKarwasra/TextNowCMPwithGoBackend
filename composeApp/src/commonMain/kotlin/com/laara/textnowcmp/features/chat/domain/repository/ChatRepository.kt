package com.laara.textnowcmp.features.chat.domain.repository

import com.laara.textnowcmp.config.database.entity.ConversationEntity
import com.laara.textnowcmp.config.database.entity.MessageEntity
import com.laara.textnowcmp.config.network.DataError
import com.laara.textnowcmp.config.network.Result
import com.laara.textnowcmp.features.chat.data.remote.dto.CreateConversationResponse
import com.laara.textnowcmp.features.chat.data.remote.dto.WsMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ChatRepository {
    suspend fun createOrGetConversation(userId: String): Result<CreateConversationResponse, DataError.Remote>
    suspend fun fetchAndCacheConversations(): Result<Unit, DataError.Remote>
    fun observeConversations(): Flow<List<ConversationEntity>>
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>
    suspend fun sendMessage(conversationId: String, content: String, myUserId: String)
    fun observeIncoming(): SharedFlow<WsMessage>
    suspend fun handleIncomingMessage(message: WsMessage, myUserId: String)
    suspend fun sendTypingIndicator(conversationId: String, recipientId: String, isTyping: Boolean)
    suspend fun sendReadReceipt(messageId: String, recipientId: String)
    fun connectWebSocket()
    fun disconnectWebSocket()
    suspend fun clearUnread(conversationId: String)
    suspend fun getRecipientUserId(conversationId: String): String?
    suspend fun sendCallSignal(message: WsMessage)
}
