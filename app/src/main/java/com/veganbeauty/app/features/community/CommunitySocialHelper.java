package com.veganbeauty.app.features.community;

import android.content.Context;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.utils.RootieBrandHelper;

public final class CommunitySocialHelper {

    private CommunitySocialHelper() {
    }

    public static String resolveUserId(Context context) {
        if (context == null) {
            return "";
        }
        return com.veganbeauty.app.utils.ProfileSessionHelper.getEffectiveUserId(context);
    }

    public static String resolveFollowTargetId(String authorId, String authorName) {
        if (RootieBrandHelper.isRootieUser(authorId, authorName)) {
            return RootieBrandHelper.USER_ID_VN;
        }
        return authorId != null ? authorId.trim() : "";
    }

    public static boolean isFollowingUser(java.util.Set<String> followingIds, String authorId, String authorName) {
        if (followingIds == null || followingIds.isEmpty()) {
            return false;
        }
        String targetId = resolveFollowTargetId(authorId, authorName);
        if (followingIds.contains(targetId)) {
            return true;
        }
        return authorId != null && followingIds.contains(authorId.trim());
    }

    public static void applyFollowChange(Context context, String actorUserId, String targetUserId, boolean follow) {
        if (context == null || actorUserId == null || targetUserId == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            LocalJsonReader reader = new LocalJsonReader(appContext);
            if (!reader.applyFollowChange(actorUserId, targetUserId, follow)) {
                return;
            }
            new FirestoreService().applyFollowChange(actorUserId, targetUserId, follow);
        }).start();
    }
}
