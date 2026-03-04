package com.laara.textnowcmp.features.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SaveProfileResponse(
    val email: String,
    val name: String,
    val profilePic: String,
    val userId: String
)
