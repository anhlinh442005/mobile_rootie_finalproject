package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "community_posts")
public class CommunityPostEntity {

    @PrimaryKey
    @NonNull
    private String postId;

    @NonNull
    private String authorId;

    @NonNull
    private String authorUsername;

    @NonNull
    private String authorDisplayName;

    @Nullable
    private String authorAvatarUrl;

    @NonNull
    private String content;

    @NonNull
    private String createdAt;

    private int likesCount;
    private int commentsCount;
    private int reupsCount;

    @Nullable
    private String skinType;

    @Nullable
    private String concern;

    @NonNull
    private String mediaUrlsString; // Comma separated URLs

    @Nullable
    private String type; // "Routine", "Review", "Hỏi đáp", etc.

    @Nullable
    private String linkedProductIds;

    public CommunityPostEntity(@NonNull String postId, @NonNull String authorId, @NonNull String authorUsername, @NonNull String authorDisplayName, @Nullable String authorAvatarUrl, @NonNull String content, @NonNull String createdAt, int likesCount, int commentsCount, int reupsCount, @Nullable String skinType, @Nullable String concern, @NonNull String mediaUrlsString, @Nullable String type, @Nullable String linkedProductIds) {
        this.postId = postId;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.authorDisplayName = authorDisplayName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.content = content;
        this.createdAt = createdAt;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.reupsCount = reupsCount;
        this.skinType = skinType;
        this.concern = concern;
        this.mediaUrlsString = mediaUrlsString;
        this.type = type;
        this.linkedProductIds = linkedProductIds;
    }

    @NonNull
    public String getPostId() {
        return postId;
    }

    public void setPostId(@NonNull String postId) {
        this.postId = postId;
    }

    @NonNull
    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(@NonNull String authorId) {
        this.authorId = authorId;
    }

    @NonNull
    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(@NonNull String authorUsername) {
        this.authorUsername = authorUsername;
    }

    @NonNull
    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public void setAuthorDisplayName(@NonNull String authorDisplayName) {
        this.authorDisplayName = authorDisplayName;
    }

    @Nullable
    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(@Nullable String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    public void setContent(@NonNull String content) {
        this.content = content;
    }

    @NonNull
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull String createdAt) {
        this.createdAt = createdAt;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public int getReupsCount() {
        return reupsCount;
    }

    public void setReupsCount(int reupsCount) {
        this.reupsCount = reupsCount;
    }

    @Nullable
    public String getSkinType() {
        return skinType;
    }

    public void setSkinType(@Nullable String skinType) {
        this.skinType = skinType;
    }

    @Nullable
    public String getConcern() {
        return concern;
    }

    public void setConcern(@Nullable String concern) {
        this.concern = concern;
    }

    @NonNull
    public String getMediaUrlsString() {
        return mediaUrlsString;
    }

    public void setMediaUrlsString(@NonNull String mediaUrlsString) {
        this.mediaUrlsString = mediaUrlsString;
    }

    @Nullable
    public String getType() {
        return type;
    }

    public void setType(@Nullable String type) {
        this.type = type;
    }

    @Nullable
    public String getLinkedProductIds() {
        return linkedProductIds;
    }

    public void setLinkedProductIds(@Nullable String linkedProductIds) {
        this.linkedProductIds = linkedProductIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommunityPostEntity that = (CommunityPostEntity) o;
        return likesCount == that.likesCount && commentsCount == that.commentsCount && reupsCount == that.reupsCount && postId.equals(that.postId) && authorId.equals(that.authorId) && authorUsername.equals(that.authorUsername) && authorDisplayName.equals(that.authorDisplayName) && Objects.equals(authorAvatarUrl, that.authorAvatarUrl) && content.equals(that.content) && createdAt.equals(that.createdAt) && Objects.equals(skinType, that.skinType) && Objects.equals(concern, that.concern) && mediaUrlsString.equals(that.mediaUrlsString) && Objects.equals(type, that.type) && Objects.equals(linkedProductIds, that.linkedProductIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, authorId, authorUsername, authorDisplayName, authorAvatarUrl, content, createdAt, likesCount, commentsCount, reupsCount, skinType, concern, mediaUrlsString, type, linkedProductIds);
    }

    @Override
    public String toString() {
        return "CommunityPostEntity{" +
                "postId='" + postId + '\'' +
                ", authorId='" + authorId + '\'' +
                ", authorUsername='" + authorUsername + '\'' +
                ", authorDisplayName='" + authorDisplayName + '\'' +
                ", authorAvatarUrl='" + authorAvatarUrl + '\'' +
                ", content='" + content + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", likesCount=" + likesCount +
                ", commentsCount=" + commentsCount +
                ", reupsCount=" + reupsCount +
                ", skinType='" + skinType + '\'' +
                ", concern='" + concern + '\'' +
                ", mediaUrlsString='" + mediaUrlsString + '\'' +
                ", type='" + type + '\'' +
                ", linkedProductIds='" + linkedProductIds + '\'' +
                '}';
    }
}
