package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "products")
public class ProductEntity {
    @PrimaryKey
    @NonNull private String id;
    @NonNull private String name;
    @NonNull private String sku;
    @NonNull private String barcode;
    private long price;
    @Nullable private Long originalPrice;
    @NonNull private String category;
    @NonNull private String brand;
    private int stock;
    @NonNull private String description;
    @NonNull private String mainImage;
    @NonNull private String suitableFor;
    @NonNull private String origin;
    @NonNull private String expiryDate;
    private boolean isNew;
    @NonNull private String categoryIds;
    
    @NonNull private List<String> album;
    @NonNull private String mainIngredientsSummary;
    @NonNull private String allergyInformation;
    @NonNull private List<KeyIngredient> keyIngredients;
    @NonNull private List<String> detailedIngredients;
    @NonNull private String storyDescription;
    @NonNull private String storyImage;
    @NonNull private List<String> idealFor;
    @NonNull private List<String> benefits;
    @NonNull private String usage;
    @NonNull private String usageAmount;
    @NonNull private String scent;
    @NonNull private String notes;
    private float rating;
    private int sold;

    public ProductEntity(@NonNull String id, @NonNull String name, @NonNull String sku, @NonNull String barcode, long price, @Nullable Long originalPrice, @NonNull String category, @NonNull String brand, int stock, @NonNull String description, @NonNull String mainImage, @NonNull String suitableFor, @NonNull String origin, @NonNull String expiryDate, boolean isNew, @NonNull String categoryIds, @NonNull List<String> album, @NonNull String mainIngredientsSummary, @NonNull String allergyInformation, @NonNull List<KeyIngredient> keyIngredients, @NonNull List<String> detailedIngredients, @NonNull String storyDescription, @NonNull String storyImage, @NonNull List<String> idealFor, @NonNull List<String> benefits, @NonNull String usage, @NonNull String usageAmount, @NonNull String scent, @NonNull String notes, float rating, int sold) {
        this.id = id; this.name = name; this.sku = sku; this.barcode = barcode; this.price = price; this.originalPrice = originalPrice; this.category = category; this.brand = brand; this.stock = stock; this.description = description; this.mainImage = mainImage; this.suitableFor = suitableFor; this.origin = origin; this.expiryDate = expiryDate; this.isNew = isNew; this.categoryIds = categoryIds; this.album = album; this.mainIngredientsSummary = mainIngredientsSummary; this.allergyInformation = allergyInformation; this.keyIngredients = keyIngredients; this.detailedIngredients = detailedIngredients; this.storyDescription = storyDescription; this.storyImage = storyImage; this.idealFor = idealFor; this.benefits = benefits; this.usage = usage; this.usageAmount = usageAmount; this.scent = scent; this.notes = notes; this.rating = rating; this.sold = sold;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getName() { return name; }
    @NonNull public String getSku() { return sku; }
    @NonNull public String getBarcode() { return barcode; }
    public long getPrice() { return price; }
    @Nullable public Long getOriginalPrice() { return originalPrice; }
    @NonNull public String getCategory() { return category; }
    @NonNull public String getBrand() { return brand; }
    public int getStock() { return stock; }
    @NonNull public String getDescription() { return description; }
    @NonNull public String getMainImage() { return mainImage; }
    @NonNull public String getSuitableFor() { return suitableFor; }
    @NonNull public String getOrigin() { return origin; }
    @NonNull public String getExpiryDate() { return expiryDate; }
    public boolean isNew() { return isNew; }
    @NonNull public String getCategoryIds() { return categoryIds; }
    @NonNull public List<String> getAlbum() { return album; }
    @NonNull public String getMainIngredientsSummary() { return mainIngredientsSummary; }
    @NonNull public String getAllergyInformation() { return allergyInformation; }
    @NonNull public List<KeyIngredient> getKeyIngredients() { return keyIngredients; }
    @NonNull public List<String> getDetailedIngredients() { return detailedIngredients; }
    @NonNull public String getStoryDescription() { return storyDescription; }
    @NonNull public String getStoryImage() { return storyImage; }
    @NonNull public List<String> getIdealFor() { return idealFor; }
    @NonNull public List<String> getBenefits() { return benefits; }
    @NonNull public String getUsage() { return usage; }
    @NonNull public String getUsageAmount() { return usageAmount; }
    @NonNull public String getScent() { return scent; }
    @NonNull public String getNotes() { return notes; }
    public float getRating() { return rating; }
    public int getSold() { return sold; }
}
