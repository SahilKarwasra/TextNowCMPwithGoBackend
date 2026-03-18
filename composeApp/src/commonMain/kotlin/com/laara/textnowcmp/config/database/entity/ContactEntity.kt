package com.laara.textnowcmp.config.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val phone: String,
    val name: String,
    val userId: String? = null,
    val email: String = "",
    val profilePic: String = "",
    val isOnboarded: Boolean = false,
)
