package com.veganbeauty.app.data.local.entities

data class CommunityProduct(
    val id: String,
    val name: String,
    val mainImage: String,
    val price: Int,
    val originalPrice: Int? = null,
    val rating: Float = 0f,
    val sold: Int = 0
)
