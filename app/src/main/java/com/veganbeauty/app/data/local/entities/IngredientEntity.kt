package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val scientificName: String,
    val image: String,
    val description: String,
    val uses: String
)
