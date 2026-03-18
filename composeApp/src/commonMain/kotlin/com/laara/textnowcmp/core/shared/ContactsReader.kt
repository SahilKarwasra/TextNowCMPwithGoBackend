package com.laara.textnowcmp.core.shared

import kotlinx.serialization.Serializable

@Serializable
data class DeviceContact(
    val phone: String,
    val name: String,
)

expect class ContactsReader {
    suspend fun getContacts(): List<DeviceContact>
}
