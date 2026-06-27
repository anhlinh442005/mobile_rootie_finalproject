package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_blogs")
public class CommunityBlogEntity {
    @PrimaryKey
    @NonNull
    private String id;
    @NonNull
    private String title;
    @NonNull
    private String shortDescription;
    @NonNull
    private String imageUrl;
    @NonNull
    private String publishedAt;

    public CommunityBlogEntity(@NonNull String id, @NonNull String title, @NonNull String shortDescription, @NonNull String imageUrl, @NonNull String publishedAt) {
        this.id = id;
        this.title = title;
        this.shortDescription = shortDescription;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }

    @NonNull
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(@NonNull String shortDescription) { this.shortDescription = shortDescription; }

    @NonNull
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(@NonNull String imageUrl) { this.imageUrl = imageUrl; }

    @NonNull
    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(@NonNull String publishedAt) { this.publishedAt = publishedAt; }
}
