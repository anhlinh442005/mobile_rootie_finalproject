package com.veganbeauty.app.features.community;

import android.content.Context;

import com.veganbeauty.app.data.local.ProfileSession;

public final class CommunitySocialHelper {

    private static final String FALLBACK_USER_ID = "test_001";

    private CommunitySocialHelper() {
    }

    public static String resolveUserId(Context context) {
        if (context == null) {
            return FALLBACK_USER_ID;
        }
        String userId = ProfileSession.getUserId(context);
        if (userId == null || userId.trim().isEmpty()) {
            return FALLBACK_USER_ID;
        }
        return userId.trim();
    }
}
