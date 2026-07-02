package com.veganbeauty.app.utils;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.features.community.beauty_hub.CommunityNewsFragment;
import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;

public final class ComProfileNavigator {

    private ComProfileNavigator() {
    }

    public static void openProfile(Context context, String userId, String avatarUrl, String userName) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        FragmentManager fm = resolveFragmentManager(context);
        if (fm == null) {
            return;
        }

        if (RootieBrandHelper.isRootieUser(userId, userName)) {
            fm.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.main_container, new CommunityNewsFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
            return;
        }

        CommunityProfileFragment profileFragment = CommunityProfileFragment.newInstance(userId.trim());
        Bundle args = profileFragment.getArguments();
        if (args != null) {
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                args.putString("AVATAR_URL", avatarUrl);
            }
            if (userName != null && !userName.isEmpty()) {
                args.putString("USER_NAME", userName);
            }
        }

        fm.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, profileFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public static void openProfile(Context context, String userId) {
        openProfile(context, userId, null, null);
    }

    private static FragmentManager resolveFragmentManager(Context context) {
        if (context instanceof FragmentActivity) {
            return ((FragmentActivity) context).getSupportFragmentManager();
        }
        return null;
    }
}
