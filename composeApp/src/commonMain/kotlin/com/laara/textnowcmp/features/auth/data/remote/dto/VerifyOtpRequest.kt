package com.laara.textnowcmp.features.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val otp: String
)
