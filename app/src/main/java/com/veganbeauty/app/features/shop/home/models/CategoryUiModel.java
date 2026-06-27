package com.veganbeauty.app.features.shop.home.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import java.util.Objects;

public class CategoryUiModel {
    @NonNull
    private String id;
    @NonNull
    private String name;
    @DrawableRes
    private int iconRes;
    private int productCount;

    public CategoryUiModel(@NonNull String id, @NonNull String name, @DrawableRes int iconRes, int productCount) {
        this.id = id;
        this.name = name;
        this.iconRes = iconRes;
        this.productCount = productCount;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    @DrawableRes
    public int getIconRes() { return iconRes; }
    public void setIconRes(@DrawableRes int iconRes) { this.iconRes = iconRes; }

    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryUiModel that = (CategoryUiModel) o;
        return iconRes == that.iconRes && productCount == that.productCount && id.equals(that.id) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, iconRes, productCount);
    }

    @Override
    public String toString() {
        return "CategoryUiModel{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", iconRes=" + iconRes + ", productCount=" + productCount + '}';
    }
}
