package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val user_id: String,
    val username: String,
    val full_name: String,
    val email: String,
    val phone: String,
    val password: String,
    val avatar: String? = null
)
