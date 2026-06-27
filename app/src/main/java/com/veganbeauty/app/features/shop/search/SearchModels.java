package com.veganbeauty.app.features.shop.search;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import java.util.Collections;
import java.util.List;

public class SearchModels {

    public enum ContentSuggestionType {
        CATEGORY, VIDEO, BLOG, POST
    }

    public static class ContentSuggestion {
        @NonNull private String label;
        @NonNull private ContentSuggestionType type;
        @Nullable private String categoryName;
        @Nullable private String subcategoryName;
        @Nullable private String videoUrl;

        public ContentSuggestion(@NonNull String label, @NonNull ContentSuggestionType type, @Nullable String categoryName, @Nullable String subcategoryName, @Nullable String videoUrl) {
            this.label = label;
            this.type = type;
            this.categoryName = categoryName;
            this.subcategoryName = subcategoryName;
            this.videoUrl = videoUrl;
        }

        @NonNull public String getLabel() { return label; }
        @NonNull public ContentSuggestionType getType() { return type; }
        @Nullable public String getCategoryName() { return categoryName; }
        @Nullable public String getSubcategoryName() { return subcategoryName; }
        @Nullable public String getVideoUrl() { return videoUrl; }
    }

    public static class SearchSuggestions {
        @NonNull private List<String> productNames;
        @NonNull private List<ContentSuggestion> contentItems;
        @NonNull private List<ProductEntity> previewProducts;

        public static final SearchSuggestions EMPTY = new SearchSuggestions(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        public SearchSuggestions(@NonNull List<String> productNames, @NonNull List<ContentSuggestion> contentItems, @NonNull List<ProductEntity> previewProducts) {
            this.productNames = productNames;
            this.contentItems = contentItems;
            this.previewProducts = previewProducts;
        }

        @NonNull public List<String> getProductNames() { return productNames; }
        @NonNull public List<ContentSuggestion> getContentItems() { return contentItems; }
        @NonNull public List<ProductEntity> getPreviewProducts() { return previewProducts; }
    }
}
