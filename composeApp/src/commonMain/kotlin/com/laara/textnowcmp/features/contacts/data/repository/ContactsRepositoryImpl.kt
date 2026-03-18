package com.laara.textnowcmp.features.contacts.data.repository

import com.laara.textnowcmp.config.database.dao.ContactDao
import com.laara.textnowcmp.config.database.entity.ContactEntity
import com.laara.textnowcmp.config.network.DataError
import com.laara.textnowcmp.config.network.Result
import com.laara.textnowcmp.config.network.onSuccess
import com.laara.textnowcmp.config.network.safeCall
import com.laara.textnowcmp.core.shared.DeviceContact
import com.laara.textnowcmp.features.contacts.data.remote.ContactsApi
import com.laara.textnowcmp.features.contacts.data.remote.dto.CheckContactsResponse
import com.laara.textnowcmp.features.contacts.domain.repository.ContactsRepository
import kotlinx.coroutines.flow.Flow

class ContactsRepositoryImpl(
    private val contactsApi: ContactsApi,
    private val contactDao: ContactDao,
) : ContactsRepository {

    override suspend fun syncContacts(
        deviceContacts: List<DeviceContact>,
    ): Result<CheckContactsResponse, DataError.Remote> {
        val phones = deviceContacts.map { it.phone }
        val phoneToName = deviceContacts.associate { it.phone to it.name }

        val result = safeCall<CheckContactsResponse> {
            contactsApi.checkContacts(phones)
        }

        result.onSuccess { response ->
            val entities = mutableListOf<ContactEntity>()

            // Onboarded contacts
            response.onboarded.forEach { user ->
                entities.add(
                    ContactEntity(
                        phone = user.phone,
                        name = user.name.ifBlank { phoneToName[user.phone] ?: "" },
                        userId = user.userId,
                        email = user.email,
                        profilePic = user.profilePic,
                        isOnboarded = true,
                    )
                )
            }

            // Not-onboarded contacts — map device names
            response.notOnboarded.forEach { phone ->
                entities.add(
                    ContactEntity(
                        phone = phone,
                        name = phoneToName[phone] ?: "",
                        isOnboarded = false,
                    )
                )
            }

            contactDao.clearAll()
            contactDao.insertAll(entities)
        }

        return result
    }

    override fun getOnboardedContacts(): Flow<List<ContactEntity>> {
        return contactDao.getOnboardedContacts()
    }

    override fun getNotOnboardedContacts(): Flow<List<ContactEntity>> {
        return contactDao.getNotOnboardedContacts()
    }
}
