package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vouchers")
public class VoucherEntity {
    @PrimaryKey
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
    public void setId(@NonNull String id) { this.id = id; }
    @NonNull public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }
    @NonNull public String getDescription() { return description; }
    public void setDescription(@NonNull String description) { this.description = description; }
    @NonNull public String getCode() { return code; }
    public void setCode(@NonNull String code) { this.code = code; }
    @NonNull public String getCategory() { return category; }
    public void setCategory(@NonNull String category) { this.category = category; }
    @NonNull public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type; }
    @Nullable public String getBadge() { return badge; }
    public void setBadge(@Nullable String badge) { this.badge = badge; }
    @NonNull public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(@NonNull String expiryDate) { this.expiryDate = expiryDate; }
    @NonNull public String getOfferType() { return offerType; }
    public void setOfferType(@NonNull String offerType) { this.offerType = offerType; }
    public long getDiscountValue() { return discountValue; }
    public void setDiscountValue(long discountValue) { this.discountValue = discountValue; }
    public long getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(long minOrderValue) { this.minOrderValue = minOrderValue; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    @Nullable public Integer getQuantity() { return quantity; }
    public void setQuantity(@Nullable Integer quantity) { this.quantity = quantity; }
}
