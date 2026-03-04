package com.laara.textnowcmp.core.shared

/**
 * Platform-specific contact reader.
 * Returns a list of phone numbers from the device's contact list.
 */
expect class ContactsReader {
    suspend fun getPhoneNumbers(): List<String>
}
