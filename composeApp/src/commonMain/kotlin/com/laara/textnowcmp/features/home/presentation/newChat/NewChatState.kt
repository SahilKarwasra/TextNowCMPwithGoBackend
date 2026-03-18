package com.laara.textnowcmp.features.home.presentation.newChat

import com.laara.textnowcmp.config.database.entity.ContactEntity

data class NewChatState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val onboardedContacts: List<ContactEntity> = emptyList(),
    val notOnboardedContacts: List<ContactEntity> = emptyList(),
    val error: String? = null,
) {
    val filteredOnboarded: List<ContactEntity>
        get() = if (searchQuery.isBlank()) onboardedContacts
        else onboardedContacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery)
        }

    val filteredNotOnboarded: List<ContactEntity>
        get() = if (searchQuery.isBlank()) notOnboardedContacts
        else notOnboardedContacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery)
        }
}