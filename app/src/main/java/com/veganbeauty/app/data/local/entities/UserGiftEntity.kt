package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_gifts")
data class UserGiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val giftId: String,
    val title: String,
    val description: String,
    val cost: Int,
    val expiryDate: String,
    val status: String, // "Còn hạn", "Hôm nay", "Hết hạn"
    val giftType: String, // "voucher", "product"
    val code: String,
    val acquiredTimestamp: Long = System.currentTimeMillis()
)
