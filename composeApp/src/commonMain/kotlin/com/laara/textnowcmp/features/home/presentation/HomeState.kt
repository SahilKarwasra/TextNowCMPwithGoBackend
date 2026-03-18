package com.laara.textnowcmp.features.home.presentation

import com.laara.textnowcmp.config.database.entity.ConversationEntity

data class HomeState(
    val conversations: List<ConversationEntity> = emptyList(),
    val isLoading: Boolean = true,
)

data class FilterChipItem(
    val id: String,
    val title: String
)