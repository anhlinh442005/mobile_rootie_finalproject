package com.veganbeauty.app.features.home.adapter;

public class HomeBannerItem {
    private String imageUrl;
    private String linkUrl;

    private String title;
    private String actionText;
    private Integer imageRes;

    public HomeBannerItem(String imageUrl, String linkUrl) {
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
    }

    /** Constructor accepting a drawable resource id - used by HomeFragment */
    public HomeBannerItem(int imageRes) {
        this.imageRes = imageRes;
        this.imageUrl = null;
        this.linkUrl = null;
    }

    public HomeBannerItem(String imageUrl, String linkUrl, String title, String actionText) {
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.title = title;
        this.actionText = actionText;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getActionText() { return actionText; }
    public void setActionText(String actionText) { this.actionText = actionText; }
    public Integer getImageRes() { return imageRes; }
    public void setImageRes(Integer imageRes) { this.imageRes = imageRes; }
}
