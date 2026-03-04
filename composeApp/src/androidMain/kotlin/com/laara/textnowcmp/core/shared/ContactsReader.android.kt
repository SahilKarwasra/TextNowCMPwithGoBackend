package com.laara.textnowcmp.core.shared

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ContactsReader(
    private val context: Context,
) {
    actual suspend fun getPhoneNumbers(): List<String> = withContext(Dispatchers.IO) {
        val phones = mutableSetOf<String>()

        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null,
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                while (it.moveToNext()) {
                    val raw = it.getString(numberIndex) ?: continue
                    val normalized = normalizePhone(raw)
                    if (normalized.isNotBlank()) {
                        phones.add(normalized)
                    }
                }
            }
        } catch (e: SecurityException) {
            // READ_CONTACTS permission not granted
            e.printStackTrace()
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
