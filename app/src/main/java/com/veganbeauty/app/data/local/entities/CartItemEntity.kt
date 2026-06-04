package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val id: String, // product ID
    val name: String,
    val image: String,
    val price: Long,
    val quantity: Int,
    val isSelected: Boolean = true
) : Serializable

