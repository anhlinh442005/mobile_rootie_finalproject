package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;

public class MemberInfoEntity {
    @NonNull private String name;
    @NonNull private String avatar;

    public MemberInfoEntity(@NonNull String name, @NonNull String avatar) {
        this.name = name;
        this.avatar = avatar;
    }

    @NonNull public String getName() { return name; }
    @NonNull public String getAvatar() { return avatar; }
    public void setAvatar(@NonNull String avatar) { this.avatar = avatar; }
}
