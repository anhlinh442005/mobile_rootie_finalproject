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
    val expiryDate: String, // datetime format: "yyyy-MM-dd HH:mm:ss"
    val status: String, // "Còn hạn", "Hôm nay", "Hết hạn"
    val giftType: String, // "voucher_discount", "voucher_freeship", "gift", "product"
    val code: String,
    val minOrderValue: Int = 0,
    val applicableProducts: String = "Tất cả sản phẩm",
    val offerType: String = "fixed_amount",
    val productId: String? = null,
    val discountValue: Int = 0,
    val acquiredTimestamp: Long = System.currentTimeMillis()
)
