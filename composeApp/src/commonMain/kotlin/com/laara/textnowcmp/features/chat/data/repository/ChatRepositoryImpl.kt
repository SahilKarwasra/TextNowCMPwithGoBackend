package com.laara.textnowcmp.features.chat.data.repository

import com.laara.textnowcmp.config.database.dao.ConversationDao
import com.laara.textnowcmp.config.database.dao.MessageDao
import com.laara.textnowcmp.config.database.entity.ConversationEntity
import com.laara.textnowcmp.config.database.entity.MessageEntity
import com.laara.textnowcmp.config.network.DataError
import com.laara.textnowcmp.config.network.Result
import com.laara.textnowcmp.config.network.onSuccess
import com.laara.textnowcmp.config.network.safeCall
import com.laara.textnowcmp.features.chat.data.remote.ConversationsApi
import com.laara.textnowcmp.features.chat.data.remote.WebSocketManager
import com.laara.textnowcmp.features.chat.data.remote.dto.CreateConversationResponse
import com.laara.textnowcmp.features.chat.data.remote.dto.ListConversationsResponse
import com.laara.textnowcmp.features.chat.data.remote.dto.WsMessage
import com.laara.textnowcmp.features.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private fun currentTimeMillis(): Long =
    kotlin.time.Clock.System.now().toEpochMilliseconds()

class ChatRepositoryImpl(
    private val conversationsApi: ConversationsApi,
    private val webSocketManager: WebSocketManager,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
) : ChatRepository {

    override suspend fun createOrGetConversation(
        userId: String,
    ): Result<CreateConversationResponse, DataError.Remote> {
        val result = safeCall<CreateConversationResponse> {
            conversationsApi.createConversation(userId)
        }

        // Cache the conversation locally
        result.onSuccess { response ->
            conversationDao.insertOrUpdate(
                ConversationEntity(
                    conversationId = response.conversationId,
                    recipientUserId = response.participant.userId,
                    recipientName = response.participant.name,
                    recipientPhone = response.participant.phone,
                    recipientProfilePic = response.participant.profilePic,
                    lastMessageTimestamp = currentTimeMillis(),
                )
            )
        }

        return result
    }

    override suspend fun fetchAndCacheConversations(): Result<Unit, DataError.Remote> {
        val result = safeCall<ListConversationsResponse> {
            conversationsApi.getConversations()
        }

        result.onSuccess { response ->
            response.conversations.forEach { dto ->
                val existing = conversationDao.getConversationById(dto.conversationId)
                if (existing == null) {
                    conversationDao.insertOrUpdate(
                        ConversationEntity(
                            conversationId = dto.conversationId,
                            recipientUserId = dto.participant.userId,
                            recipientName = dto.participant.name,
                            recipientPhone = dto.participant.phone,
                            recipientProfilePic = dto.participant.profilePic,
                            lastMessageTimestamp = currentTimeMillis(),
                        )
                    )
                }
            }
        }

        return when (result) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.error)
        }
    }

    override fun observeConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }

    override fun observeMessages(conversationId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        myUserId: String,
    ) {
        val messageId = Uuid.random().toString()
        val now = currentTimeMillis()

        // Save to DB first (optimistic)
        messageDao.insert(
            MessageEntity(
                messageId = messageId,
                conversationId = conversationId,
                senderId = myUserId,
                content = content,
                timestamp = now,
                isFromMe = true,
                status = "sent",
            )
        )

        // Update conversation's last message
        conversationDao.updateLastMessage(conversationId, content, now)

        // Send via WebSocket
        val wsMessage = WsMessage(
            type = WsMessage.TYPE_DIRECT_MESSAGE,
            messageId = messageId,
            conversationId = conversationId,
            content = content,
            timestamp = kotlin.time.Clock.System.now().toString(),
        )
        webSocketManager.send(wsMessage)
    }

    override fun observeIncoming(): SharedFlow<WsMessage> {
        return webSocketManager.incoming
    }

    override suspend fun handleIncomingMessage(message: WsMessage, myUserId: String) {
        when (message.type) {
            WsMessage.TYPE_DIRECT_MESSAGE -> {
                val conversationId = message.conversationId ?: return
                val senderId = message.senderId ?: return
                if (senderId == myUserId) return // our own echo

                messageDao.insert(
                    MessageEntity(
                        messageId = message.messageId ?: return,
                        conversationId = conversationId,
                        senderId = senderId,
                        senderName = message.senderName ?: "",
                        content = message.content ?: "",
                        timestamp = currentTimeMillis(),
                        isFromMe = false,
                    )
                )

                val existing = conversationDao.getConversationById(conversationId)
                if (existing != null) {
                    conversationDao.updateLastMessage(
                        conversationId,
                        message.content ?: "",
                        currentTimeMillis()
                    )
                    conversationDao.incrementUnread(conversationId)
                } else {
                    conversationDao.insertOrUpdate(
                        ConversationEntity(
                            conversationId = conversationId,
                            recipientUserId = senderId,
                            recipientName = message.senderName ?: "",
                            recipientPhone = message.senderPhone ?: "",
                            lastMessage = message.content ?: "",
                            lastMessageTimestamp = currentTimeMillis(),
                            unreadCount = 1,
                        )
                    )
                }
            }

            WsMessage.TYPE_ACK -> {
                val msgId = message.messageId ?: return
                messageDao.updateStatus(msgId, "delivered")
            }

            WsMessage.TYPE_DELIVERED -> {
                val msgId = message.messageId ?: return
                messageDao.updateStatus(msgId, "delivered")
            }

            WsMessage.TYPE_READ -> {
                val msgId = message.messageId ?: return
                messageDao.updateStatus(msgId, "read")
            }

            WsMessage.TYPE_CALL_LOG -> {
                val callId = message.callId ?: return
                val conversationId = message.conversationId ?: return
                val callType = message.callType ?: WsMessage.CALL_TYPE_VOICE
                val status = message.status ?: "unknown"
                val direction = message.direction ?: "incoming"
                val duration = message.duration ?: 0
                messageDao.insert(
                    MessageEntity(
                        messageId = callId,
                        conversationId = conversationId,
                        senderId = message.participantId ?: "",
                        content = "$callType|$status|$direction|$duration",
                        timestamp = currentTimeMillis(),
                        isFromMe = direction == "outgoing",
                        status = "delivered",
                        messageType = "call_log",
                    )
                )
            }
        }
    }

    override fun connectWebSocket() {
        webSocketManager.connect()
    }

    override fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }

    override suspend fun clearUnread(conversationId: String) {
        conversationDao.clearUnread(conversationId)
    }

    override suspend fun sendTypingIndicator(
        conversationId: String,
        recipientId: String,
        isTyping: Boolean,
    ) {
        val wsMessage = WsMessage(
            type = if (isTyping) WsMessage.TYPE_TYPING else WsMessage.TYPE_STOP_TYPING,
            conversationId = conversationId,
            recipientId = recipientId,
        )
        webSocketManager.send(wsMessage)
    }

    override suspend fun sendReadReceipt(messageId: String, recipientId: String) {
        val wsMessage = WsMessage(
            type = WsMessage.TYPE_READ,
            messageId = messageId,
            recipientId = recipientId,
        )
        webSocketManager.send(wsMessage)
        messageDao.updateStatus(messageId, "read")
    }

    override suspend fun getRecipientUserId(conversationId: String): String? {
        return conversationDao.getConversationById(conversationId)?.recipientUserId
    }

    override suspend fun sendCallSignal(message: WsMessage) {
        webSocketManager.send(message)
    }
}
