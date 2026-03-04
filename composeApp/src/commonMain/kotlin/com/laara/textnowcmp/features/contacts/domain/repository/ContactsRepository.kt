package com.laara.textnowcmp.features.contacts.domain.repository

import com.laara.textnowcmp.config.network.DataError
import com.laara.textnowcmp.config.network.Result
import com.laara.textnowcmp.features.contacts.data.remote.dto.CheckContactsResponse

interface ContactsRepository {
    suspend fun checkContacts(phones: List<String>): Result<CheckContactsResponse, DataError.Remote>
}
