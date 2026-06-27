package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_product_expiry")
public class UserProductExpiryEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull private String userId;
    @NonNull private String productId;
    @NonNull private String name;
    @NonNull private String mainImage;
    @NonNull private String brand;
    private long price;
    @NonNull private String category;
    @NonNull private String sku;
    private int stock;
    @NonNull private String expiryDate;

    public UserProductExpiryEntity(int id, @NonNull String userId, @NonNull String productId, @NonNull String name, @NonNull String mainImage, @NonNull String brand, long price, @NonNull String category, @NonNull String sku, int stock, @NonNull String expiryDate) {
        this.id = id; this.userId = userId; this.productId = productId; this.name = name; this.mainImage = mainImage; this.brand = brand; this.price = price; this.category = category; this.sku = sku; this.stock = stock; this.expiryDate = expiryDate;
    }

    public ProductEntity toProductEntity() {
        return new ProductEntity(
            this.productId, this.name, this.sku, "", this.price, null, this.category, "", this.brand, this.stock, "", this.mainImage, "", "", this.expiryDate, false, java.util.Collections.emptyList(), "", "", java.util.Collections.emptyList(), java.util.Collections.emptyList(), "", "", java.util.Collections.emptyList(), java.util.Collections.emptyList(), "", "", "", "", 0f, 0
        ); // Partial conversion as original data class allowed defaults
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    @NonNull public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }
    @NonNull public String getProductId() { return productId; }
    public void setProductId(@NonNull String productId) { this.productId = productId; }
    @NonNull public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }
    @NonNull public String getMainImage() { return mainImage; }
    public void setMainImage(@NonNull String mainImage) { this.mainImage = mainImage; }
    @NonNull public String getBrand() { return brand; }
    public void setBrand(@NonNull String brand) { this.brand = brand; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    @NonNull public String getCategory() { return category; }
    public void setCategory(@NonNull String category) { this.category = category; }
    @NonNull public String getSku() { return sku; }
    public void setSku(@NonNull String sku) { this.sku = sku; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    @NonNull public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(@NonNull String expiryDate) { this.expiryDate = expiryDate; }
}
