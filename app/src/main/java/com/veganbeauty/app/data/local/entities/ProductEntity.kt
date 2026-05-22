package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String,
    val price: Long,
    val category: String,
    val categoryIds: String = "",
    val stock: Int,
    val description: String,
    val mainImage: String,
    val suitableFor: String,
    val origin: String,
    val expiryDate: String,
    val isNew: Boolean = false
)
