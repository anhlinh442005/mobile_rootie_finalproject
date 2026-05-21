package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "community_posts")
data class CommunityPostEntity(
    @PrimaryKey val postId: String,
    val authorId: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String?,
    val content: String,
    val createdAt: String,
    val likesCount: Int,
    val commentsCount: Int,
    val skinType: String?,
    val concern: String?,
    val mediaUrlsString: String // Comma separated URLs
)
