package com.veganbeauty.app.features.shop.search

import com.veganbeauty.app.data.local.entities.ProductEntity

enum class ContentSuggestionType {
    CATEGORY, VIDEO, BLOG, POST
}

data class ContentSuggestion(
    val label: String,
    val type: ContentSuggestionType,
    val categoryName: String? = null,
    val subcategoryName: String? = null,
    val videoUrl: String? = null
)

data class SearchSuggestions(
    val productNames: List<String>,
    val contentItems: List<ContentSuggestion>,
    val previewProducts: List<ProductEntity>
) {
    companion object {
        val EMPTY = SearchSuggestions(emptyList(), emptyList(), emptyList())
    }
}
