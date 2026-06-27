package com.veganbeauty.app.features.shop.home.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

public class BannerUiModel {
    @NonNull
    private String id;
    
    @DrawableRes
    private int imageRes;
    
    @Nullable
    private String actionUrl;

    public BannerUiModel(@NonNull String id, @DrawableRes int imageRes, @Nullable String actionUrl) {
        this.id = id;
        this.imageRes = imageRes;
        this.actionUrl = actionUrl;
    }

    public BannerUiModel(@NonNull String id, @DrawableRes int imageRes) {
        this(id, imageRes, null);
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @DrawableRes
    public int getImageRes() { return imageRes; }
    public void setImageRes(@DrawableRes int imageRes) { this.imageRes = imageRes; }

    @Nullable
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(@Nullable String actionUrl) { this.actionUrl = actionUrl; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BannerUiModel that = (BannerUiModel) o;
        return imageRes == that.imageRes && id.equals(that.id) && Objects.equals(actionUrl, that.actionUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, imageRes, actionUrl);
    }

    @Override
    public String toString() {
        return "BannerUiModel{" + "id='" + id + '\'' + ", imageRes=" + imageRes + ", actionUrl='" + actionUrl + '\'' + '}';
    }
}
