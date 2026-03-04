package com.laara.textnowcmp.features.auth.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequest(
    val phone: String
)
