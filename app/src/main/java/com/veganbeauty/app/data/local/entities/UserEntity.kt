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
    val avatar: String? = null,
    val primary_image: String? = null
) {
    @androidx.room.Ignore
    var mutualCount: Int = 0
    @androidx.room.Ignore
    var firstMutualFriendName: String? = null
    @androidx.room.Ignore
    var mutualFriendAvatars: List<String> = emptyList()
}
