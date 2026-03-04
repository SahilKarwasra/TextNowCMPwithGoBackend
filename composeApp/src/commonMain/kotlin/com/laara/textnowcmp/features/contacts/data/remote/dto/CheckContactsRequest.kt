package com.laara.textnowcmp.features.contacts.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckContactsRequest(
    val phones: List<String>,
)
