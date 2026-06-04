package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "explore_videos")
data class YtVideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val description: String,
    val username: String,
    val avatarUrl: String?,
    val type: String,
    val likesCount: Int = (100..5000).random(),
    val commentsCount: Int = (10..500).random(),
    val shareCount: Int = (5..100).random()
)
