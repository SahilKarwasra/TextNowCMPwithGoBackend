package com.laara.textnowcmp.core.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Contacts.CNContactFetchRequest
import platform.Contacts.CNContactPhoneNumbersKey
import platform.Contacts.CNContactStore
import platform.Contacts.CNLabeledValue
import platform.Contacts.CNPhoneNumber

actual class ContactsReader {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getPhoneNumbers(): List<String> = withContext(Dispatchers.IO) {
        val phones = mutableSetOf<String>()

        try {
            val store = CNContactStore()
            val keysToFetch = listOf(CNContactPhoneNumbersKey)
            val request = CNContactFetchRequest(keysToFetch = keysToFetch)

            store.enumerateContactsWithFetchRequest(request, error = null) { contact, _ ->
                if (contact != null) {
                    val phoneNumbers = contact.phoneNumbers as List<CNLabeledValue>
                    for (labeledValue in phoneNumbers) {
                        val phoneNumber = labeledValue.value as? CNPhoneNumber
                        val raw = phoneNumber?.stringValue ?: continue
                        val normalized = normalizePhone(raw)
                        if (normalized.isNotBlank()) {
                            phones.add(normalized)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        phones.toList()
    }

    private fun normalizePhone(raw: String): String {
        val digitsOnly = raw.replace(Regex("[^0-9]"), "")
        return if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else digitsOnly
    }
}
