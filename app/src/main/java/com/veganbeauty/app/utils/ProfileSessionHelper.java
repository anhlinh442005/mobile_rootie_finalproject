package com.veganbeauty.app.utils;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.UserEntity;

import java.io.File;
import java.util.List;

public final class ProfileSessionHelper {

    public static final String DEFAULT_AVATAR_URL =
            "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg";

    private ProfileSessionHelper() {}

    @Nullable
    public static UserEntity findCurrentUser(Context context) {
        RootieDatabase db = RootieDatabase.getDatabase(context.getApplicationContext());
        String userId = ProfileSession.getUserId(context);
        UserEntity user = db.userDao().getUserByIdSync(userId);
        if (user != null) {
            return user;
        }

        String email = ProfileSession.getEmail(context);
        if (email != null && !email.trim().isEmpty()) {
            user = db.userDao().getUserByEmailSync(email.trim());
            if (user != null) {
                return user;
            }
        }

        LocalJsonReader reader = new LocalJsonReader(context.getApplicationContext());
        List<UserEntity> users = reader.getUsers();
        for (UserEntity candidate : users) {
            if (userId != null && userId.equals(candidate.getUser_id())) {
                return candidate;
            }
            if (email != null && email.equalsIgnoreCase(candidate.getEmail())) {
                return candidate;
            }
        }
        return null;
    }

    public static void syncSessionFromUser(Context context, UserEntity user) {
        if (user == null) {
            return;
        }
        ProfileSession.setUserId(context, user.getUser_id());
        ProfileSession.setFullName(context, user.getFull_name());
        ProfileSession.setEmail(context, user.getEmail());
        ProfileSession.setPhone(context, user.getPhone());
        ProfileSession.setUsername(context, user.getUsername());

        String avatarUrl = resolveAvatarUrl(user);
        if (isUsableAvatarUrl(avatarUrl)) {
            ProfileSession.setAvatar(context, avatarUrl.trim());
        }
        if (user.getPrimary_image() != null && !user.getPrimary_image().trim().isEmpty()) {
            ProfileSession.setPrimaryImage(context, user.getPrimary_image().trim());
        }
        if (user.getBio() != null && !user.getBio().trim().isEmpty()) {
            ProfileSession.setBio(context, user.getBio().trim());
        }
    }

    public static String resolveAvatarUrl(@Nullable UserEntity user) {
        if (user == null) {
            return "";
        }
        if (RootieBrandHelper.isRootieUser(user.getUser_id())) {
            return RootieBrandHelper.AVATAR_URL;
        }
        String avatar = user.getAvatar();
        if (avatar != null && !avatar.trim().isEmpty()) {
            return avatar.trim();
        }
        return "";
    }

    public static String resolveEffectiveAvatarUrl(Context context) {
        String sessionAvatar = ProfileSession.getAvatar(context);
        if (isUsableAvatarUrl(sessionAvatar)) {
            return sessionAvatar.trim();
        }

        String assetsAvatar = lookupAvatarFromAssets(context, getEffectiveUserId(context));
        if (isUsableAvatarUrl(assetsAvatar)) {
            return assetsAvatar.trim();
        }

        return DEFAULT_AVATAR_URL;
    }

    public static String resolveEffectiveAvatarUrl(Context context, @Nullable UserEntity user) {
        if (user != null) {
            String fromUser = resolveAvatarUrl(user);
            if (isUsableAvatarUrl(fromUser)) {
                return fromUser.trim();
            }
        }
        return resolveEffectiveAvatarUrl(context);
    }

    public static boolean isUsableAvatarUrl(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String value = url.trim();
        if (value.startsWith("file://")) {
            try {
                String path = Uri.parse(value).getPath();
                return path != null && new File(path).exists();
            } catch (Exception ignored) {
                return false;
            }
        }
        return value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("content://");
    }

    private static String lookupAvatarFromAssets(Context context, @Nullable String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "";
        }
        LocalJsonReader reader = new LocalJsonReader(context.getApplicationContext());
        for (UserEntity candidate : reader.getUsers()) {
            if (userId.equals(candidate.getUser_id())) {
                return resolveAvatarUrl(candidate);
            }
        }
        return "";
    }

    public static void ensureCurrentUserInDatabase(Context context) {
        UserEntity user = findCurrentUser(context);
        if (user == null) {
            return;
        }
        RootieDatabase db = RootieDatabase.getDatabase(context.getApplicationContext());
        db.userDao().insertUserSync(user);
    }

    public static String getEffectiveUserId(Context context) {
        if (!ProfileSession.isLoggedIn(context)) {
            return "";
        }
        String sessionUserId = ProfileSession.getUserId(context);
        if (sessionUserId != null && !sessionUserId.trim().isEmpty()) {
            return sessionUserId.trim();
        }
        return ProfileSession.getCurrentUserId(context);
    }

    public static String getDisplayAvatarUrl(Context context) {
        return resolveEffectiveAvatarUrl(context, findCurrentUser(context));
    }
}
