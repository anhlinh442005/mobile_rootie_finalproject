package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reels")
public class ReelEntity {
    @PrimaryKey
    @NonNull
    private String videoId;
    @NonNull
    private String caption;
    @NonNull
    private String authorId;
    @NonNull
    private String authorUsername;
    @NonNull
    private String authorDisplayName;
    @Nullable
    private String authorAvatarUrl;
    private int likesCount;
    private int commentsCount;
    private int shareCount;
    @NonNull
    private String thumbnailUrl;

    public ReelEntity(@NonNull String videoId, @NonNull String caption, @NonNull String authorId, @NonNull String authorUsername, @NonNull String authorDisplayName, @Nullable String authorAvatarUrl, int likesCount, int commentsCount, int shareCount, @NonNull String thumbnailUrl) {
        this.videoId = videoId;
        this.caption = caption;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.authorDisplayName = authorDisplayName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.shareCount = shareCount;
        this.thumbnailUrl = thumbnailUrl;
    }

    @NonNull
    public String getVideoId() { return videoId; }
    public void setVideoId(@NonNull String videoId) { this.videoId = videoId; }

    @NonNull
    public String getCaption() { return caption; }
    public void setCaption(@NonNull String caption) { this.caption = caption; }

    @NonNull
    public String getAuthorId() { return authorId; }
    public void setAuthorId(@NonNull String authorId) { this.authorId = authorId; }

    @NonNull
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(@NonNull String authorUsername) { this.authorUsername = authorUsername; }

    @NonNull
    public String getAuthorDisplayName() { return authorDisplayName; }
    public void setAuthorDisplayName(@NonNull String authorDisplayName) { this.authorDisplayName = authorDisplayName; }

    @Nullable
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public void setAuthorAvatarUrl(@Nullable String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }

    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public int getShareCount() { return shareCount; }
    public void setShareCount(int shareCount) { this.shareCount = shareCount; }

    @NonNull
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(@NonNull String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}
