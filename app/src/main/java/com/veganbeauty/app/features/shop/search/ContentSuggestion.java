package com.veganbeauty.app.features.shop.search;

public class ContentSuggestion {
    private String label;
    private ContentSuggestionType type;
    private String parentCategory;
    private String categoryName;
    private String videoUrl;

    public ContentSuggestion(String label, ContentSuggestionType type, String parentCategory, String categoryName, String videoUrl) {
        this.label = label;
        this.type = type;
        this.parentCategory = parentCategory;
        this.categoryName = categoryName;
        this.videoUrl = videoUrl;
    }

    public String getLabel() { return label; }
    public ContentSuggestionType getType() { return type; }
    public String getParentCategory() { return parentCategory; }
    public String getCategoryName() { return categoryName; }
    public String getVideoUrl() { return videoUrl; }
}
