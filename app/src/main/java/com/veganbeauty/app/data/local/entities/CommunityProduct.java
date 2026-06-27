package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

public class CommunityProduct {
    @NonNull
    private String id;
    @NonNull
    private String name;
    @NonNull
    private String mainImage;
    private int price;
    @Nullable
    private Integer originalPrice;
    private float rating;
    private int sold;

    public CommunityProduct(@NonNull String id, @NonNull String name, @NonNull String mainImage, int price, @Nullable Integer originalPrice, float rating, int sold) {
        this.id = id;
        this.name = name;
        this.mainImage = mainImage;
        this.price = price;
        this.originalPrice = originalPrice;
        this.rating = rating;
        this.sold = sold;
    }

    public CommunityProduct(@NonNull String id, @NonNull String name, @NonNull String mainImage, int price) {
        this(id, name, mainImage, price, null, 0f, 0);
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    @NonNull
    public String getMainImage() { return mainImage; }
    public void setMainImage(@NonNull String mainImage) { this.mainImage = mainImage; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    @Nullable
    public Integer getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(@Nullable Integer originalPrice) { this.originalPrice = originalPrice; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommunityProduct that = (CommunityProduct) o;
        return price == that.price && Float.compare(that.rating, rating) == 0 && sold == that.sold && id.equals(that.id) && name.equals(that.name) && mainImage.equals(that.mainImage) && Objects.equals(originalPrice, that.originalPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, mainImage, price, originalPrice, rating, sold);
    }

    @Override
    public String toString() {
        return "CommunityProduct{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", mainImage='" + mainImage + '\'' + ", price=" + price + ", originalPrice=" + originalPrice + ", rating=" + rating + ", sold=" + sold + '}';
    }
}
