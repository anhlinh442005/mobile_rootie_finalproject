package com.veganbeauty.app.features.shop.home.models

import androidx.annotation.DrawableRes

data class BannerUiModel(
    val id: String,
    @DrawableRes val imageRes: Int,
    val actionUrl: String? = null
)
