package com.laara.textnowcmp.features.chat.data.remote

import com.laara.textnowcmp.features.chat.data.remote.dto.CreateConversationRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ConversationsApi(
    private val baseUrl: String,
) : KoinComponent {

    private val authenticatedClient: HttpClient by inject()

    suspend fun createConversation(userId: String): HttpResponse {
        return authenticatedClient.post("$baseUrl/conversations") {
            contentType(ContentType.Application.Json)
            setBody(CreateConversationRequest(userId = userId))
        }
    }

    suspend fun getConversations(): HttpResponse {
        return authenticatedClient.get("$baseUrl/conversations")
    }
}
