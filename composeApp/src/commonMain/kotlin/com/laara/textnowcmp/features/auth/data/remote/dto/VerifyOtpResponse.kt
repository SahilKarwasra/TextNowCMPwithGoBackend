package com.laara.textnowcmp.features.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyOtpResponse(
    val accessToken: String,
    val expiresIn: Int,
    val isNewUser: Boolean,
    val refreshToken: String,
    val tokenType: String,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: String,
    val phone: String
)
