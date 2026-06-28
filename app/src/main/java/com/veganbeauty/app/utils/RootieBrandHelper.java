package com.veganbeauty.app.utils;

import java.util.Locale;

public final class RootieBrandHelper {

    public static final String AVATAR_URL =
            "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1781257994/favicon_r7kqwf.png";
    public static final String USER_ID_VN = "rootie_vn";
    public static final String USER_ID_OFFICIAL = "rootie_official";

    private RootieBrandHelper() {
    }

    public static boolean isRootieUser(String userId) {
        return USER_ID_VN.equals(userId) || USER_ID_OFFICIAL.equals(userId);
    }

    public static boolean isRootieUser(String userId, String name) {
        if (isRootieUser(userId)) {
            return true;
        }
        if (name == null || name.isEmpty()) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("rootie vietnam") || lower.contains("rootie viet nam");
    }

    public static String resolveAvatar(String userId, String avatarUrl) {
        if (isRootieUser(userId)) {
            return AVATAR_URL;
        }
        return avatarUrl != null ? avatarUrl : "";
    }
}
