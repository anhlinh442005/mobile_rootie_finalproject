package com.veganbeauty.app.utils;

import android.content.Context;
import androidx.annotation.Nullable;
import com.veganbeauty.app.data.local.ProfileSession;

public class IdentityMapper {

    @Nullable
    public static String mapName(Context context, @Nullable String originalId, @Nullable String originalName) {
        if (originalId == null)
            return originalName;
        String currentUserId = ProfileSession.getCurrentUserId(context);
        if (currentUserId != null && currentUserId.equals(originalId)) {
            String localName = ProfileSession.getFullName(context);
            if (localName == null || localName.trim().isEmpty()) {
                localName = ProfileSession.getUsername(context);
            }
            if (localName != null && !localName.trim().isEmpty()) {
                return localName;
            }
        }
        return originalName;
    }

    @Nullable
    public static String mapAvatar(Context context, @Nullable String originalId, @Nullable String originalAvatar) {
        if (originalId == null)
            return originalAvatar;
        String currentUserId = ProfileSession.getCurrentUserId(context);
        if (currentUserId != null && currentUserId.equals(originalId)) {
            String localAvatar = ProfileSessionHelper.getDisplayAvatarUrl(context);
            if (localAvatar != null && !localAvatar.trim().isEmpty()) {
                return localAvatar;
            }
        }
        return originalAvatar;
    }
}
