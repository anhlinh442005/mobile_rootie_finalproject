package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_memory")
public class UserMemoryEntity {
    @PrimaryKey
    @NonNull
    private String id;
    @NonNull
    private String actionType;
    @NonNull
    private String targetUserId;
    @NonNull
    private String targetUsername;
    @NonNull
    private String targetAvatar;
    @NonNull
    private String content;
    private long timestamp;

    public UserMemoryEntity(@NonNull String id, @NonNull String actionType, @NonNull String targetUserId, @NonNull String targetUsername, @NonNull String targetAvatar, @NonNull String content, long timestamp) {
        this.id = id;
        this.actionType = actionType;
        this.targetUserId = targetUserId;
        this.targetUsername = targetUsername;
        this.targetAvatar = targetAvatar;
        this.content = content;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getActionType() { return actionType; }
    public void setActionType(@NonNull String actionType) { this.actionType = actionType; }

    @NonNull
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(@NonNull String targetUserId) { this.targetUserId = targetUserId; }

    @NonNull
    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(@NonNull String targetUsername) { this.targetUsername = targetUsername; }

    @NonNull
    public String getTargetAvatar() { return targetAvatar; }
    public void setTargetAvatar(@NonNull String targetAvatar) { this.targetAvatar = targetAvatar; }

    @NonNull
    public String getContent() { return content; }
    public void setContent(@NonNull String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
