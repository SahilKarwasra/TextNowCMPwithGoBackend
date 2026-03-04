package com.laara.textnowcmp.features.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SaveProfileRequest(
    val name: String,
    val email: String
)
