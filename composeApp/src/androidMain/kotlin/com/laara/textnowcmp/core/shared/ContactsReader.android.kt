package com.laara.textnowcmp.core.shared

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ContactsReader(
    private val context: Context,
) {
    actual suspend fun getContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        val contactsMap = mutableMapOf<String, String>() // phone → name

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC",
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                while (it.moveToNext()) {
                    val raw = it.getString(numberIndex) ?: continue
                    val name = it.getString(nameIndex) ?: ""
                    val normalized = normalizePhone(raw)
                    if (normalized.isNotBlank() && !contactsMap.containsKey(normalized)) {
                        contactsMap[normalized] = name
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
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
