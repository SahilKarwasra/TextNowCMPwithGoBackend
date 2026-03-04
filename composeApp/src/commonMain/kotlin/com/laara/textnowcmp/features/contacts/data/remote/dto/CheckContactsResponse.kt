package com.laara.textnowcmp.features.contacts.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckContactsResponse(
    val onboarded: List<OnboardedUserDto>,
    val onboardedCount: Int,
    val notOnboarded: List<String>,
    val notOnboardedCount: Int,
    val total: Int,
)

@Serializable
data class OnboardedUserDto(
    val userId: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val profilePic: String = "",
)
