package com.laara.textnowcmp.features.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendOtpResponse(
    val expiresIn: String,
    val phone: String
)
