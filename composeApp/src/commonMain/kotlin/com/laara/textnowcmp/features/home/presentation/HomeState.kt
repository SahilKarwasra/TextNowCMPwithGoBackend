package com.laara.textnowcmp.features.home.presentation

data class HomeState(
    val paramOne: String = "default",
    val paramTwo: List<String> = emptyList(),
)

data class FilterChipItem(
    val id: String,
    val title: String
)

data class ChatItem(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0
)