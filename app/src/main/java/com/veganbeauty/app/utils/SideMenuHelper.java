package com.veganbeauty.app.utils;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            Glide.with(ivAvatar.getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.img_avatar)
                    .error(R.drawable.img_avatar)
                    .circleCrop()
                    .into(ivAvatar);
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
}
