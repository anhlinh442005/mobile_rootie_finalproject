package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Collections;
import java.util.List;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    private String user_id;
    @NonNull
    private String username;
    @NonNull
    private String full_name;
    @NonNull
    private String email;
    @NonNull
    private String phone;
    @NonNull
    private String password;
    @Nullable
    private String avatar;
    @Nullable
    private String primary_image;

    @Ignore
    private int mutualCount = 0;
    @Ignore
    @Nullable
    private String firstMutualFriendName = null;
    @Ignore
    @NonNull
    private List<String> mutualFriendAvatars = Collections.emptyList();
    @Ignore
    private String bio = "";
    @Ignore
    private String skinType = "";
    @Ignore
    private String concerns = "";

    public UserEntity(@NonNull String user_id, @NonNull String username, @NonNull String full_name, @NonNull String email, @NonNull String phone, @NonNull String password, @Nullable String avatar, @Nullable String primary_image) {
        this.user_id = user_id;
        this.username = username;
        this.full_name = full_name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.avatar = avatar;
        this.primary_image = primary_image;
    }

    @Ignore
    public UserEntity(@NonNull String user_id, @NonNull String username, @NonNull String full_name, @NonNull String email, @NonNull String phone, @NonNull String password, @Nullable String avatar) {
        this(user_id, username, full_name, email, phone, password, avatar, null);
    }

    @Ignore
    public UserEntity(@NonNull String user_id, @NonNull String username, @NonNull String full_name, @NonNull String email, @NonNull String phone, @NonNull String password) {
        this(user_id, username, full_name, email, phone, password, null, null);
    }

    public UserEntity() {
        this.user_id = "";
        this.username = "";
        this.full_name = "";
        this.email = "";
        this.phone = "";
        this.password = "";
    }

    @NonNull public String getUser_id() { return user_id; }
    public void setUser_id(@NonNull String user_id) { this.user_id = user_id; }
    @NonNull public String getUsername() { return username; }
    public void setUsername(@NonNull String username) { this.username = username; }
    @NonNull public String getFull_name() { return full_name; }
    public void setFull_name(@NonNull String full_name) { this.full_name = full_name; }
    @NonNull public String getEmail() { return email; }
    public void setEmail(@NonNull String email) { this.email = email; }
    @NonNull public String getPhone() { return phone; }
    public void setPhone(@NonNull String phone) { this.phone = phone; }
    @NonNull public String getPassword() { return password; }
    public void setPassword(@NonNull String password) { this.password = password; }
    @Nullable public String getAvatar() { return avatar; }
    public void setAvatar(@Nullable String avatar) { this.avatar = avatar; }
    @Nullable public String getPrimary_image() { return primary_image; }
    public void setPrimary_image(@Nullable String primary_image) { this.primary_image = primary_image; }

    public int getMutualCount() { return mutualCount; }
    public void setMutualCount(int mutualCount) { this.mutualCount = mutualCount; }
    @Nullable public String getFirstMutualFriendName() { return firstMutualFriendName; }
    public void setFirstMutualFriendName(@Nullable String firstMutualFriendName) { this.firstMutualFriendName = firstMutualFriendName; }
    @NonNull public List<String> getMutualFriendAvatars() { return mutualFriendAvatars; }
    public void setMutualFriendAvatars(@NonNull List<String> mutualFriendAvatars) { this.mutualFriendAvatars = mutualFriendAvatars; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getSkinType() { return skinType; }
    public void setSkinType(String skinType) { this.skinType = skinType; }
    public String getConcerns() { return concerns; }
    public void setConcerns(String concerns) { this.concerns = concerns; }
}
