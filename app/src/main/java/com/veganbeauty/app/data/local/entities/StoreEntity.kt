package com.veganbeauty.app.data.local.entities

data class StoreEntity(
    val id: String,
    val storeCode: String,
    val storeName: String,
    val address: String,
    val province: String,
    val openHours: String,
    val imageUrl: String,
    val distance: Double // mocked distance in km
)
