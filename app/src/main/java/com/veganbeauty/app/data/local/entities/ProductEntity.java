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

    public ProductEntity() {
    }

    public ProductEntity(@NonNull String id, @NonNull String name, @NonNull String sku, @NonNull String barcode, long price, @Nullable Long originalPrice, @NonNull String category, @NonNull String brand, int stock, @NonNull String description, @NonNull String mainImage, @NonNull String suitableFor, @NonNull String origin, @NonNull String expiryDate, boolean isNew, @NonNull String categoryIds, @NonNull List<String> album, @NonNull String mainIngredientsSummary, @NonNull String allergyInformation, @NonNull List<KeyIngredient> keyIngredients, @NonNull List<String> detailedIngredients, @NonNull String storyDescription, @NonNull String storyImage, @NonNull List<String> idealFor, @NonNull List<String> benefits, @NonNull String usage, @NonNull String usageAmount, @NonNull String scent, @NonNull String notes, float rating, int sold) {
        this.id = id; this.name = name; this.sku = sku; this.barcode = barcode; this.price = price; this.originalPrice = originalPrice; this.category = category; this.brand = brand; this.stock = stock; this.description = description; this.mainImage = mainImage; this.suitableFor = suitableFor; this.origin = origin; this.expiryDate = expiryDate; this.isNew = isNew; this.categoryIds = categoryIds; this.album = album; this.mainIngredientsSummary = mainIngredientsSummary; this.allergyInformation = allergyInformation; this.keyIngredients = keyIngredients; this.detailedIngredients = detailedIngredients; this.storyDescription = storyDescription; this.storyImage = storyImage; this.idealFor = idealFor; this.benefits = benefits; this.usage = usage; this.usageAmount = usageAmount; this.scent = scent; this.notes = notes; this.rating = rating; this.sold = sold;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    @NonNull public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }
    @NonNull public String getSku() { return sku; }
    public void setSku(@NonNull String sku) { this.sku = sku; }
    @NonNull public String getBarcode() { return barcode; }
    public void setBarcode(@NonNull String barcode) { this.barcode = barcode; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    @Nullable public Long getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(@Nullable Long originalPrice) { this.originalPrice = originalPrice; }
    @NonNull public String getCategory() { return category; }
    public void setCategory(@NonNull String category) { this.category = category; }
    @NonNull public String getBrand() { return brand; }
    public void setBrand(@NonNull String brand) { this.brand = brand; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    @NonNull public String getDescription() { return description; }
    public void setDescription(@NonNull String description) { this.description = description; }
    @NonNull public String getMainImage() { return mainImage; }
    public void setMainImage(@NonNull String mainImage) { this.mainImage = mainImage; }
    @NonNull public String getSuitableFor() { return suitableFor; }
    public void setSuitableFor(@NonNull String suitableFor) { this.suitableFor = suitableFor; }
    @NonNull public String getOrigin() { return origin; }
    public void setOrigin(@NonNull String origin) { this.origin = origin; }
    @NonNull public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(@NonNull String expiryDate) { this.expiryDate = expiryDate; }
    public boolean isNew() { return isNew; }
    public void setNew(boolean aNew) { isNew = aNew; }
    @NonNull public String getCategoryIds() { return categoryIds; }
    public void setCategoryIds(@NonNull String categoryIds) { this.categoryIds = categoryIds; }
    @NonNull public List<String> getAlbum() { return album; }
    public void setAlbum(@NonNull List<String> album) { this.album = album; }
    @NonNull public String getMainIngredientsSummary() { return mainIngredientsSummary; }
    public void setMainIngredientsSummary(@NonNull String mainIngredientsSummary) { this.mainIngredientsSummary = mainIngredientsSummary; }
    @NonNull public String getAllergyInformation() { return allergyInformation; }
    public void setAllergyInformation(@NonNull String allergyInformation) { this.allergyInformation = allergyInformation; }
    @NonNull public List<KeyIngredient> getKeyIngredients() { return keyIngredients; }
    public void setKeyIngredients(@NonNull List<KeyIngredient> keyIngredients) { this.keyIngredients = keyIngredients; }
    @NonNull public List<String> getDetailedIngredients() { return detailedIngredients; }
    public void setDetailedIngredients(@NonNull List<String> detailedIngredients) { this.detailedIngredients = detailedIngredients; }
    @NonNull public String getStoryDescription() { return storyDescription; }
    public void setStoryDescription(@NonNull String storyDescription) { this.storyDescription = storyDescription; }
    @NonNull public String getStoryImage() { return storyImage; }
    public void setStoryImage(@NonNull String storyImage) { this.storyImage = storyImage; }
    @NonNull public List<String> getIdealFor() { return idealFor; }
    public void setIdealFor(@NonNull List<String> idealFor) { this.idealFor = idealFor; }
    @NonNull public List<String> getBenefits() { return benefits; }
    public void setBenefits(@NonNull List<String> benefits) { this.benefits = benefits; }
    @NonNull public String getUsage() { return usage; }
    public void setUsage(@NonNull String usage) { this.usage = usage; }
    @NonNull public String getUsageAmount() { return usageAmount; }
    public void setUsageAmount(@NonNull String usageAmount) { this.usageAmount = usageAmount; }
    @NonNull public String getScent() { return scent; }
    public void setScent(@NonNull String scent) { this.scent = scent; }
    @NonNull public String getNotes() { return notes; }
    public void setNotes(@NonNull String notes) { this.notes = notes; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }
}
