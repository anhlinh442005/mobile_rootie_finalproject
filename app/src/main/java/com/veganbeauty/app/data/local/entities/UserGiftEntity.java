package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_gifts")
public class UserGiftEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull private String giftId;
    @NonNull private String title;
    @NonNull private String description;
    private int cost;
    @NonNull private String expiryDate; // datetime format: "yyyy-MM-dd HH:mm:ss"
    @NonNull private String status; // "Còn hạn", "Hôm nay", "Hết hạn"
    @NonNull private String giftType; // "voucher_discount", "voucher_freeship", "gift", "product"
    @NonNull private String code;
    private int minOrderValue;
    @NonNull private String applicableProducts;
    @NonNull private String offerType;
    @Nullable private String productId;
    private int discountValue;
    private long acquiredTimestamp;

    public UserGiftEntity(int id, @NonNull String giftId, @NonNull String title, @NonNull String description, int cost, @NonNull String expiryDate, @NonNull String status, @NonNull String giftType, @NonNull String code, int minOrderValue, @NonNull String applicableProducts, @NonNull String offerType, @Nullable String productId, int discountValue, long acquiredTimestamp) {
        this.id = id; this.giftId = giftId; this.title = title; this.description = description; this.cost = cost; this.expiryDate = expiryDate; this.status = status; this.giftType = giftType; this.code = code; this.minOrderValue = minOrderValue; this.applicableProducts = applicableProducts; this.offerType = offerType; this.productId = productId; this.discountValue = discountValue; this.acquiredTimestamp = acquiredTimestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    @NonNull public String getGiftId() { return giftId; }
    public void setGiftId(@NonNull String giftId) { this.giftId = giftId; }
    @NonNull public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }
    @NonNull public String getDescription() { return description; }
    public void setDescription(@NonNull String description) { this.description = description; }
    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
    @NonNull public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(@NonNull String expiryDate) { this.expiryDate = expiryDate; }
    @NonNull public String getStatus() { return status; }
    public void setStatus(@NonNull String status) { this.status = status; }
    @NonNull public String getGiftType() { return giftType; }
    public void setGiftType(@NonNull String giftType) { this.giftType = giftType; }
    @NonNull public String getCode() { return code; }
    public void setCode(@NonNull String code) { this.code = code; }
    public int getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(int minOrderValue) { this.minOrderValue = minOrderValue; }
    @NonNull public String getApplicableProducts() { return applicableProducts; }
    public void setApplicableProducts(@NonNull String applicableProducts) { this.applicableProducts = applicableProducts; }
    @NonNull public String getOfferType() { return offerType; }
    public void setOfferType(@NonNull String offerType) { this.offerType = offerType; }
    @Nullable public String getProductId() { return productId; }
    public void setProductId(@Nullable String productId) { this.productId = productId; }
    public int getDiscountValue() { return discountValue; }
    public void setDiscountValue(int discountValue) { this.discountValue = discountValue; }
    public long getAcquiredTimestamp() { return acquiredTimestamp; }
    public void setAcquiredTimestamp(long acquiredTimestamp) { this.acquiredTimestamp = acquiredTimestamp; }
}
