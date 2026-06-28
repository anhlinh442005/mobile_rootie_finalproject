package com.veganbeauty.app.features.shop.search;

import java.util.List;
import com.veganbeauty.app.data.local.entities.ProductEntity;

public class SearchSuggestions {
    private List<String> productNames;
    private List<ContentSuggestion> contentItems;
    private List<ProductEntity> previewProducts;

    public static final SearchSuggestions EMPTY = new SearchSuggestions(java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyList());

    public SearchSuggestions(List<String> productNames, List<ContentSuggestion> contentItems, List<ProductEntity> previewProducts) {
        this.productNames = productNames;
        this.contentItems = contentItems;
        this.previewProducts = previewProducts;
    }

    public List<String> getProductNames() { return productNames; }
    public List<ContentSuggestion> getContentItems() { return contentItems; }
    public List<ProductEntity> getPreviewProducts() { return previewProducts; }
}
