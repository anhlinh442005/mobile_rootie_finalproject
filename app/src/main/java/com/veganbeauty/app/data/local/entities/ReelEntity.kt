package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reels")
data class ReelEntity(
    @PrimaryKey val videoId: String,
    val caption: String,
    val authorId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String?,
    val likesCount: Int,
    val commentsCount: Int,
    val shareCount: Int,
    val thumbnailUrl: String
)
