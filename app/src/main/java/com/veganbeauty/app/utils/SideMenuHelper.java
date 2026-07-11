package com.veganbeauty.app.utils;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import android.os.Bundle;
import android.widget.Toast;

import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;
import com.veganbeauty.app.features.community.profile.CommunityEditProfileFragment;
import com.veganbeauty.app.features.community.com_feed.ExploreSearchFragment;
import com.veganbeauty.app.features.community.com_feed.CommunityDiscoverPeopleFragment;

public final class SideMenuHelper {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String TAG_EXPANDABLE_BOUND = "side_menu_expandable_bound";

    private SideMenuHelper() {
    }

    public static void bindCurrentUser(@NonNull View menuRoot) {
        Context context = menuRoot.getContext().getApplicationContext();
        View contentRoot = resolveMenuContentRoot(menuRoot);
        setupExpandableSections(contentRoot);
        applyUserInfo(contentRoot, context, null);

        EXECUTOR.execute(() -> {
            UserEntity user = ProfileSessionHelper.findCurrentUser(context);
            contentRoot.post(() -> applyUserInfo(contentRoot, context, user));
        });
    }

    @NonNull
    private static View resolveMenuContentRoot(@NonNull View menuRoot) {
        View sideMenuRoot = menuRoot.findViewById(R.id.sideMenuRoot);
        if (sideMenuRoot != null) {
            return sideMenuRoot;
        }
        View avatar = menuRoot.findViewById(R.id.ivSideMenuAvatar);
        if (avatar != null) {
            return menuRoot;
        }
        if (menuRoot.getRootView() != null) {
            sideMenuRoot = menuRoot.getRootView().findViewById(R.id.sideMenuRoot);
            if (sideMenuRoot != null) {
                return sideMenuRoot;
            }
        }
        return menuRoot;
    }

    private static void setupExpandableSections(@NonNull View menuRoot) {
        if (TAG_EXPANDABLE_BOUND.equals(menuRoot.getTag())) {
            return;
        }
        menuRoot.setTag(TAG_EXPANDABLE_BOUND);

        bindExpandableGroup(menuRoot, R.id.llMenuSettingsHeader, R.id.llMenuSettingsChildren, R.id.ivMenuSettingsChevron);
        bindExpandableGroup(menuRoot, R.id.llMenuAccountHeader, R.id.llMenuAccountChildren, R.id.ivMenuAccountChevron);
        bindExpandableGroup(menuRoot, R.id.llMenuShowcaseHeader, R.id.llMenuShowcaseChildren, R.id.ivMenuShowcaseChevron);
    }

    private static void bindExpandableGroup(@NonNull View menuRoot, int headerId, int childrenId, int chevronId) {
        View header = menuRoot.findViewById(headerId);
        View children = menuRoot.findViewById(childrenId);
        ImageView chevron = menuRoot.findViewById(chevronId);
        if (header == null || children == null) {
            return;
        }

        children.setVisibility(View.VISIBLE);
        if (chevron != null) {
            chevron.setRotation(0f);
        }

        header.setOnClickListener(v -> {
            boolean expanded = children.getVisibility() == View.VISIBLE;
            children.setVisibility(expanded ? View.GONE : View.VISIBLE);
            if (chevron != null) {
                chevron.animate().rotation(expanded ? -90f : 0f).setDuration(180).start();
            }
        });
    }

    private static void applyUserInfo(@NonNull View menuRoot, Context context, @Nullable UserEntity user) {
        ImageView ivAvatar = menuRoot.findViewById(R.id.ivSideMenuAvatar);
        TextView tvDisplayName = menuRoot.findViewById(R.id.tvSideMenuDisplayName);
        TextView tvUsername = menuRoot.findViewById(R.id.tvSideMenuUsername);

        String displayName = ProfileSession.getFullName(context);
        String username = ProfileSession.getUsername(context);
        String avatarUrl = ProfileSessionHelper.getAccountProfileAvatarUrl(context);

        if (displayName == null || displayName.trim().isEmpty()) {
            if (user != null && user.getFull_name() != null && !user.getFull_name().trim().isEmpty()) {
                displayName = user.getFull_name().trim();
            }
        }
        if (username == null || username.trim().isEmpty()) {
            if (user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
                username = user.getUsername().trim();
            }
        }
        if (!ProfileSessionHelper.isUsableAvatarUrl(avatarUrl) && user != null) {
            avatarUrl = ProfileSessionHelper.resolveEffectiveAvatarUrl(context, user);
        }

        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "Người dùng";
        }

        if (tvDisplayName != null) {
            tvDisplayName.setText(displayName);
            tvDisplayName.setTextColor(Color.WHITE);
        }
        if (tvUsername != null) {
            tvUsername.setText(formatHandle(username));
            tvUsername.setTextColor(Color.parseColor("#E6FFFFFF"));
        }
        if (ivAvatar != null) {
            AvatarLoader.loadAvatar(ivAvatar, avatarUrl);
        }
    }

    @NonNull
    public static String formatHandle(@Nullable String username) {
        if (username == null || username.trim().isEmpty()) {
            return "@user";
        }
        String handle = username.trim();
        if (handle.startsWith("@")) {
            handle = handle.substring(1);
        }
        return "@" + handle.toLowerCase().replace(" ", "_");
    }

    public static void setupMenuNavigation(@NonNull View menuRoot, @NonNull FragmentManager fm, @Nullable DrawerLayout drawer) {
        View contentRoot = resolveMenuContentRoot(menuRoot);
        Context context = contentRoot.getContext();

        // Helper to navigate and close drawer
        android.view.View.OnClickListener navListener = v -> {
            Fragment targetFragment = null;
            if (v.getId() == R.id.llMenuFriends) {
                targetFragment = new CommunityDiscoverPeopleFragment();
            } else if (v.getId() == R.id.llMenuSaved || v.getId() == R.id.llMenuFavorites) {
                targetFragment = new ExploreSearchFragment();
                Bundle args = new Bundle();
                args.putBoolean("SAVED_MODE", true);
                targetFragment.setArguments(args);
            } else if (v.getId() == R.id.llMenuProfile) {
                targetFragment = new CommunityProfileFragment();
            } else if (v.getId() == R.id.llMenuProfileEdit) {
                targetFragment = new CommunityEditProfileFragment();
            }

            if (targetFragment != null) {
                if (drawer != null) {
                    drawer.closeDrawer(menuRoot);
                }
                fm.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, targetFragment)
                        .addToBackStack(null)
                        .commit();
            }
        };

        // Helper for "under development" Toast
        android.view.View.OnClickListener devListener = v -> {
            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
                if (drawer != null) {
                    drawer.closeDrawer(menuRoot);
                }
        };

        // Bind implemented features
        View llMenuFriends = contentRoot.findViewById(R.id.llMenuFriends);
        if (llMenuFriends != null) llMenuFriends.setOnClickListener(navListener);
        
        View llMenuSaved = contentRoot.findViewById(R.id.llMenuSaved);
        if (llMenuSaved != null) llMenuSaved.setOnClickListener(navListener);
        
        View llMenuFavorites = contentRoot.findViewById(R.id.llMenuFavorites);
        if (llMenuFavorites != null) llMenuFavorites.setOnClickListener(navListener);

        View llMenuProfile = contentRoot.findViewById(R.id.llMenuProfile);
        if (llMenuProfile != null) llMenuProfile.setOnClickListener(navListener);

        View llMenuProfileEdit = contentRoot.findViewById(R.id.llMenuProfileEdit);
        if (llMenuProfileEdit != null) llMenuProfileEdit.setOnClickListener(navListener);

        // Bind "under development" features
        int[] devIds = {
                R.id.llMenuArchive, R.id.llMenuEvents,
                R.id.llMenuPrivacy, R.id.llMenuLanguage, R.id.llMenuDarkMode,
                R.id.llMenuTerms,
                R.id.llMenuShowcaseProducts, R.id.llMenuShowcaseFunds
        };
        for (int id : devIds) {
            View devView = contentRoot.findViewById(id);
            if (devView != null) {
                devView.setOnClickListener(devListener);
            }
        }
    }
}
