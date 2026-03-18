package com.laara.textnowcmp.features.contacts.domain.repository

import com.laara.textnowcmp.config.database.entity.ContactEntity
import com.laara.textnowcmp.config.network.DataError
import com.laara.textnowcmp.config.network.Result
import com.laara.textnowcmp.core.shared.DeviceContact
import com.laara.textnowcmp.features.contacts.data.remote.dto.CheckContactsResponse
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {
    suspend fun syncContacts(deviceContacts: List<DeviceContact>): Result<CheckContactsResponse, DataError.Remote>
    fun getOnboardedContacts(): Flow<List<ContactEntity>>
    fun getNotOnboardedContacts(): Flow<List<ContactEntity>>
}
