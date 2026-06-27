package com.veganbeauty.app.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class CommunityModels {

    public static class User {
        @NonNull private String userId;
        @NonNull private String username;
        @Nullable private String avatarUrl;

        public User(@NonNull String userId, @NonNull String username, @Nullable String avatarUrl) {
            this.userId = userId;
            this.username = username;
            this.avatarUrl = avatarUrl;
        }
        @NonNull public String getUserId() { return userId; }
        @NonNull public String getUsername() { return username; }
        @Nullable public String getAvatarUrl() { return avatarUrl; }
    }

    public static class CommunityPost {
        @NonNull private String postId;
        @NonNull private Author author;
        @NonNull private String content;
        @NonNull private String createdAt;
        private int likesCount;
        private int commentsCount;
        @Nullable private String skinType;
        @Nullable private String concern;
        @NonNull private List<String> mediaUrls;

        public CommunityPost(@NonNull String postId, @NonNull Author author, @NonNull String content, @NonNull String createdAt, int likesCount, int commentsCount, @Nullable String skinType, @Nullable String concern, @NonNull List<String> mediaUrls) {
            this.postId = postId; this.author = author; this.content = content; this.createdAt = createdAt; this.likesCount = likesCount; this.commentsCount = commentsCount; this.skinType = skinType; this.concern = concern; this.mediaUrls = mediaUrls;
        }
        @NonNull public String getPostId() { return postId; }
        @NonNull public Author getAuthor() { return author; }
        @NonNull public String getContent() { return content; }
        @NonNull public String getCreatedAt() { return createdAt; }
        public int getLikesCount() { return likesCount; }
        public int getCommentsCount() { return commentsCount; }
        @Nullable public String getSkinType() { return skinType; }
        @Nullable public String getConcern() { return concern; }
        @NonNull public List<String> getMediaUrls() { return mediaUrls; }
    }

    public static class Author {
        @NonNull private String userId;
        @NonNull private String username;
        @NonNull private String displayName;
        @Nullable private String avatarUrl;

        public Author(@NonNull String userId, @NonNull String username, @NonNull String displayName, @Nullable String avatarUrl) {
            this.userId = userId; this.username = username; this.displayName = displayName; this.avatarUrl = avatarUrl;
        }
        @NonNull public String getUserId() { return userId; }
        @NonNull public String getUsername() { return username; }
        @NonNull public String getDisplayName() { return displayName; }
        @Nullable public String getAvatarUrl() { return avatarUrl; }
    }

    public static class Reel {
        @NonNull private String videoId;
        @NonNull private String caption;
        @NonNull private Author author;
        private int likesCount;
        private int commentsCount;
        private int shareCount;
        @NonNull private String thumbnailUrl;

        public Reel(@NonNull String videoId, @NonNull String caption, @NonNull Author author, int likesCount, int commentsCount, int shareCount, @NonNull String thumbnailUrl) {
            this.videoId = videoId; this.caption = caption; this.author = author; this.likesCount = likesCount; this.commentsCount = commentsCount; this.shareCount = shareCount; this.thumbnailUrl = thumbnailUrl;
        }
        @NonNull public String getVideoId() { return videoId; }
        @NonNull public String getCaption() { return caption; }
        @NonNull public Author getAuthor() { return author; }
        public int getLikesCount() { return likesCount; }
        public int getCommentsCount() { return commentsCount; }
        public int getShareCount() { return shareCount; }
        @NonNull public String getThumbnailUrl() { return thumbnailUrl; }
    }
}
