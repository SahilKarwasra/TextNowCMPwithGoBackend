package com.laara.textnowcmp.core.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Contacts.CNContactFetchRequest
import platform.Contacts.CNContactGivenNameKey
import platform.Contacts.CNContactFamilyNameKey
import platform.Contacts.CNContactPhoneNumbersKey
import platform.Contacts.CNContactStore
import platform.Contacts.CNLabeledValue
import platform.Contacts.CNPhoneNumber

actual class ContactsReader {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, String>() // phone → name

        try {
            val store = CNContactStore()
            val keysToFetch = listOf(
                CNContactPhoneNumbersKey,
                CNContactGivenNameKey,
                CNContactFamilyNameKey,
            )
            val request = CNContactFetchRequest(keysToFetch = keysToFetch)

            store.enumerateContactsWithFetchRequest(request, error = null) { contact, _ ->
                if (contact != null) {
                    val fullName = listOfNotNull(
                        contact.givenName.takeIf { it.isNotBlank() },
                        contact.familyName.takeIf { it.isNotBlank() },
                    ).joinToString(" ")

                    val phoneNumbers = contact.phoneNumbers as List<CNLabeledValue>
                    for (labeledValue in phoneNumbers) {
                        val phoneNumber = labeledValue.value as? CNPhoneNumber
                        val raw = phoneNumber?.stringValue ?: continue
                        val normalized = normalizePhone(raw)
                        if (normalized.isNotBlank() && !contactsMap.containsKey(normalized)) {
                            contactsMap[normalized] = fullName
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        contactsMap.map { (phone, name) -> DeviceContact(phone = phone, name = name) }
    }

    private fun normalizePhone(raw: String): String {
        val digitsOnly = raw.replace(Regex("[^0-9]"), "")
        return if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else digitsOnly
    }
}
