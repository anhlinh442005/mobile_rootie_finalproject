package com.veganbeauty.app.features.account.expiry;

import com.veganbeauty.app.data.local.entities.ProductEntity;

public class ExpiryProductUiModel {
    private ProductEntity product;
    private int remainingDays;
    private String durationText;
    private int progressPercent;
    private boolean isUrgent;

    public ExpiryProductUiModel(ProductEntity product, int remainingDays, String durationText, int progressPercent, boolean isUrgent) {
        this.product = product;
        this.remainingDays = remainingDays;
        this.durationText = durationText;
        this.progressPercent = progressPercent;
        this.isUrgent = isUrgent;
    }

    public ProductEntity getProduct() { return product; }
    public int getRemainingDays() { return remainingDays; }
    public String getDurationText() { return durationText; }
    public int getProgressPercent() { return progressPercent; }
    public boolean isUrgent() { return isUrgent; }
}
