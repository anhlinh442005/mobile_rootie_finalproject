package com.veganbeauty.app.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.UserEntity;

import java.util.ArrayList;
import java.util.List;

public final class UserAvatarHelper {

    private static List<String> pinterestPool;

    private UserAvatarHelper() {}

    private static void ensurePool(Context context) {
        if (pinterestPool != null && !pinterestPool.isEmpty()) {
            return;
        }
        pinterestPool = new ArrayList<>();
        for (UserEntity user : new LocalJsonReader(context.getApplicationContext()).getUsers()) {
            String avatar = user.getAvatar();
            if (isPinterestAvatar(avatar) && !pinterestPool.contains(avatar)) {
                pinterestPool.add(avatar);
            }
        }
        if (pinterestPool.isEmpty()) {
            pinterestPool.add(ProfileSessionHelper.DEFAULT_AVATAR_URL);
        }
    }

    public static boolean isPinterestAvatar(@Nullable String url) {
        return url != null && !url.trim().isEmpty() && url.toLowerCase().contains("pinimg.com");
    }

    public static boolean needsReplacement(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return true;
        }
        if (isPinterestAvatar(url)) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.contains("dicebear")
                || lower.contains("cloudinary")
                || lower.contains("favicon")
                || lower.contains("1ad84b9ab4a1e2ab17c7aab37fcff0a5");
    }

    public static String resolve(Context context, @Nullable String userId, @Nullable String rawAvatar, @Nullable UserEntity user) {
        ensurePool(context);
        if (isPinterestAvatar(rawAvatar)) {
            return rawAvatar.trim();
        }
        if (user != null && isPinterestAvatar(user.getAvatar())) {
            return user.getAvatar().trim();
        }
        String seed = userId != null && !userId.isEmpty() ? userId : String.valueOf(rawAvatar != null ? rawAvatar.hashCode() : 0);
        int index = Math.abs(seed.hashCode()) % pinterestPool.size();
        return pinterestPool.get(index);
    }
}
