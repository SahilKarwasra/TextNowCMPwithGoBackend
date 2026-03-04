package com.laara.textnowcmp.features.contacts.data.remote

import com.laara.textnowcmp.features.contacts.data.remote.dto.CheckContactsRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ContactsApi(
    private val baseUrl: String,
) : KoinComponent {

    private val authenticatedClient: HttpClient by inject()

    suspend fun checkContacts(phones: List<String>): HttpResponse {
        return authenticatedClient.post("$baseUrl/contacts/check") {
            contentType(ContentType.Application.Json)
            setBody(CheckContactsRequest(phones = phones))
        }
    }
}
