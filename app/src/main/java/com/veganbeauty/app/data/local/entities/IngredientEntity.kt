package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.veganbeauty.app.data.local.ProductConverters

@Entity(tableName = "ingredients")
@TypeConverters(ProductConverters::class)
data class IngredientEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val scientificName: String,
    val image: String,
    val origin: String = "",
    val description: String,
    val uses: String,
    val types: List<String> = emptyList()
)
