package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "community_blogs")
data class CommunityBlogEntity(
    @PrimaryKey val id: String,
    val title: String,
    val shortDescription: String,
    val imageUrl: String,
    val publishedAt: String
)
