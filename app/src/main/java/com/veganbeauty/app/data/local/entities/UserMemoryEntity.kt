package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_memory")
data class UserMemoryEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val actionType: String, // e.g., "FOLLOW", "FOLLOWED_BY", "LIKE"
    val targetUserId: String,
    val targetUsername: String,
    val targetAvatar: String,
    val content: String, // e.g., "bắt đầu theo dõi bạn"
    val timestamp: Long = System.currentTimeMillis()
)
