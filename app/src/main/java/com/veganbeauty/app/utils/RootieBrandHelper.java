package com.veganbeauty.app.utils;

import java.util.Locale;

public final class RootieBrandHelper {

    public static final String AVATAR_URL =
            "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1781257994/favicon_r7kqwf.png";
    public static final String COVER_URL =
            "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780843940/bbc9ba9c-790c-481c-9600-ad6736337cba_mszen2.png";
    public static final String FANPAGE_URL =
            "https://www.facebook.com/RootieVietnamOfficial";
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

    public static boolean isAdminUser(String userId) {
        if (userId == null) return false;
        java.util.Set<String> adminIds = new java.util.HashSet<>(java.util.Arrays.asList(
            "test_001", "39751498", "87962440", "68751659", "85097162", "48228004"
        ));
        return adminIds.contains(userId.trim());
    }

    public static String resolveAvatar(String userId, String avatarUrl) {
        if (isRootieUser(userId)) {
            return AVATAR_URL;
        }
        return avatarUrl != null ? avatarUrl : "";
    }

    public static void applyVerifiedBadge(android.widget.TextView textView, String userId, String name) {
        if (textView == null) {
            return;
        }
        if (isRootieUser(userId, name)) {
            android.graphics.drawable.Drawable verifiedIcon = androidx.core.content.ContextCompat.getDrawable(
                    textView.getContext(),
                    com.veganbeauty.app.R.drawable.ic_verified
            );
            if (verifiedIcon != null) {
                int size = (int) (14 * textView.getResources().getDisplayMetrics().density);
                verifiedIcon.setBounds(0, 0, size, size);
                textView.setCompoundDrawables(null, null, verifiedIcon, null);
                textView.setCompoundDrawablePadding((int) (4 * textView.getResources().getDisplayMetrics().density));
            }
        } else {
            textView.setCompoundDrawables(null, null, null, null);
            textView.setCompoundDrawablePadding(0);
        }
    }
}
