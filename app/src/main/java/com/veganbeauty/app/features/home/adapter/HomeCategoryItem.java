package com.veganbeauty.app.features.home.adapter;

import androidx.annotation.NonNull;

import java.util.Objects;

public class HomeCategoryItem {
    @NonNull private String name;
    private int iconResId;

    public HomeCategoryItem(@NonNull String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public HomeCategoryItem(@NonNull String name) {
        this(name, com.veganbeauty.app.R.drawable.ic_grid);
    }

    @NonNull public String getName() { return name; }
    public int getIconResId() { return iconResId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HomeCategoryItem that = (HomeCategoryItem) o;
        return iconResId == that.iconResId && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, iconResId);
    }
}
