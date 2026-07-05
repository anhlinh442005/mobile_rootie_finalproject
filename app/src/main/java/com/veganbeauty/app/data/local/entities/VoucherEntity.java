package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VoucherEntity {
    @NonNull private String id;
    @NonNull private String title;
    @NonNull private String description;
    @NonNull private String code;
    @NonNull private String category;
    @NonNull private String type;
    @Nullable private String badge;
    @NonNull private String expiryDate;
    @NonNull private String offerType;
    private long discountValue;
    private long minOrderValue;
    private boolean active;
    private int sortOrder;
    @Nullable private Integer quantity;

    public VoucherEntity() {
        id = "";
        title = "";
        description = "";
        code = "";
        category = "";
        type = "discount";
        expiryDate = "";
        offerType = "fixed_amount";
        active = true;
    }

    public VoucherEntity(@NonNull String id, @NonNull String title, @NonNull String description,
                         @NonNull String code, @NonNull String category, @NonNull String type,
                         @Nullable String badge, @NonNull String expiryDate, @NonNull String offerType,
                         long discountValue, long minOrderValue, boolean active, int sortOrder,
                         @Nullable Integer quantity) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.code = code;
        this.category = category;
        this.type = type;
        this.badge = badge;
        this.expiryDate = expiryDate;
        this.offerType = offerType;
        this.discountValue = discountValue;
        this.minOrderValue = minOrderValue;
        this.active = active;
        this.sortOrder = sortOrder;
        this.quantity = quantity;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getTitle() { return title; }
    @NonNull public String getDescription() { return description; }
    @NonNull public String getCode() { return code; }
    @NonNull public String getCategory() { return category; }
    @NonNull public String getType() { return type; }
    @Nullable public String getBadge() { return badge; }
    @NonNull public String getExpiryDate() { return expiryDate; }
    @NonNull public String getOfferType() { return offerType; }
    public long getDiscountValue() { return discountValue; }
    public long getMinOrderValue() { return minOrderValue; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
    @Nullable public Integer getQuantity() { return quantity; }
}
