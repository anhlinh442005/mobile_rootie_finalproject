package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

data class KeyIngredient(
    val name: String,
    val description: String
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String,
    val barcode: String = "",
    val price: Long,
    val originalPrice: Long? = null,
    val category: String,
    val brand: String = "",
    val stock: Int,
    val description: String = "",
    val mainImage: String = "",
    val suitableFor: String = "",
    val origin: String = "",
    val expiryDate: String = "",
    val isNew: Boolean = false,
    val categoryIds: String = "",
    
    // New fields for detail page
    val album: List<String> = emptyList(),
    val mainIngredientsSummary: String = "",
    val allergyInformation: String = "",
    val keyIngredients: List<KeyIngredient> = emptyList(),
    val detailedIngredients: List<String> = emptyList(),
    val storyDescription: String = "",
    val storyImage: String = "",
    val idealFor: List<String> = emptyList(),
    val benefits: List<String> = emptyList(),
    val usage: String = "",
    val usageAmount: String = "",
    val scent: String = "",
    val notes: String = "",
    val rating: Float = 0f,
    val sold: Int = 0
)

