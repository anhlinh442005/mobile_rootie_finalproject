package com.veganbeauty.app.data.local.entities

data class YtVideoEntity(
    val id: String,
    val title: String,
    val url: String,
    val description: String,
    val username: String,
    val avatarUrl: String?,
    val type: List<String>,
    val likesCount: Int = (100..5000).random(),
    val commentsCount: Int = (10..500).random(),
    val shareCount: Int = (5..100).random()
)
