package com.laara.textnowcmp.features.home.presentation.newChat

import com.laara.textnowcmp.features.contacts.data.remote.dto.OnboardedUserDto

data class NewChatState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val onboardedContacts: List<OnboardedUserDto> = emptyList(),
    val notOnboardedPhones: List<String> = emptyList(),
    val error: String? = null,
) {
    val filteredOnboarded: List<OnboardedUserDto>
        get() = if (searchQuery.isBlank()) onboardedContacts
        else onboardedContacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery)
        }

    val filteredNotOnboarded: List<String>
        get() = if (searchQuery.isBlank()) notOnboardedPhones
        else notOnboardedPhones.filter { it.contains(searchQuery) }
}