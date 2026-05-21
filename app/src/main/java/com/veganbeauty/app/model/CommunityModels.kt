package com.veganbeauty.app.model

data class User(
    val userId: String,
    val username: String,
    val avatarUrl: String?
)

data class CommunityPost(
    val postId: String,
    val author: Author,
    val content: String,
    val createdAt: String,
    val likesCount: Int,
    val commentsCount: Int,
    val skinType: String?,
    val concern: String?,
    val mediaUrls: List<String>
)

data class Author(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?
)

data class Reel(
    val videoId: String,
    val caption: String,
    val author: Author,
    val likesCount: Int,
    val commentsCount: Int,
    val shareCount: Int,
    val thumbnailUrl: String
)
