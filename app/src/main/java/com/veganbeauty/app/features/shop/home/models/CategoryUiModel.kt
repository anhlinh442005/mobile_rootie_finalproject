package com.veganbeauty.app.features.shop.home.models

import androidx.annotation.DrawableRes

data class CategoryUiModel(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int,
    val productCount: Int
)
