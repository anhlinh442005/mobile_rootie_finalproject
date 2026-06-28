package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "explore_videos")
public class YtVideoEntity {
    @PrimaryKey
    @NonNull
    private String id;
    @NonNull
    private String title;
    @NonNull
    private String url;
    @NonNull
    private String description;
    @NonNull
    private String username;
    @Nullable
    private String avatarUrl;
    @NonNull
    private String type;
    private int likesCount;
    private int commentsCount;
    private int shareCount;

    @Ignore
    private String hashtags = "";
    @Ignore
    private String keywords = "";

    public YtVideoEntity(@NonNull String id, @NonNull String title, @NonNull String url, @NonNull String description, @NonNull String username, @Nullable String avatarUrl, @NonNull String type, int likesCount, int commentsCount, int shareCount) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.description = description;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.type = type;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.shareCount = shareCount;
    }

    public YtVideoEntity() {
        this.id = "";
        this.title = "";
        this.url = "";
        this.description = "";
        this.username = "";
        this.type = "";
    }

    @Ignore
    public YtVideoEntity(String id, String title, String url, String description, String username, String avatar, String type) {
        this(id, title, url, description, username, avatar, type, 0, 0, 0);
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    @NonNull public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }
    @NonNull public String getUrl() { return url; }
    public void setUrl(@NonNull String url) { this.url = url; }
    @NonNull public String getDescription() { return description; }
    public void setDescription(@NonNull String description) { this.description = description; }
    @NonNull public String getUsername() { return username; }
    public void setUsername(@NonNull String username) { this.username = username; }
    @Nullable public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(@Nullable String avatarUrl) { this.avatarUrl = avatarUrl; }
    @NonNull public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type; }
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
    public int getShareCount() { return shareCount; }
    public void setShareCount(int shareCount) { this.shareCount = shareCount; }
    
    public String getHashtags() { return hashtags; }
    public void setHashtags(String hashtags) { this.hashtags = hashtags; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
}
