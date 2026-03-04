package com.laara.textnowcmp.features.contacts.data.repository

import com.laara.textnowcmp.config.network.DataError
import com.laara.textnowcmp.config.network.Result
import com.laara.textnowcmp.config.network.safeCall
import com.laara.textnowcmp.features.contacts.data.remote.ContactsApi
import com.laara.textnowcmp.features.contacts.data.remote.dto.CheckContactsResponse
import com.laara.textnowcmp.features.contacts.domain.repository.ContactsRepository

class ContactsRepositoryImpl(
    private val contactsApi: ContactsApi,
) : ContactsRepository {

    override suspend fun checkContacts(
        phones: List<String>,
    ): Result<CheckContactsResponse, DataError.Remote> {
        return safeCall {
            contactsApi.checkContacts(phones)
        }
    }
}
