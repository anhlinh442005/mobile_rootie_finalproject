package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_points")
data class RewardPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: String,
    val points: Int,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
