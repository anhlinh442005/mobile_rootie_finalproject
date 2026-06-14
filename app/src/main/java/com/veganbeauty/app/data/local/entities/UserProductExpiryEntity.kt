package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_product_expiry")
data class UserProductExpiryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val productId: String,
    val name: String,
    val mainImage: String,
    val brand: String = "",
    val price: Long = 0,
    val category: String = "",
    val sku: String = "",
    val stock: Int = 0,
    val expiryDate: String
) {
    fun toProductEntity(): ProductEntity {
        return ProductEntity(
            id = this.productId,
            name = this.name,
            sku = this.sku,
            price = this.price,
            category = this.category,
            brand = this.brand,
            stock = this.stock,
            mainImage = this.mainImage,
            expiryDate = this.expiryDate
        )
    }
}
