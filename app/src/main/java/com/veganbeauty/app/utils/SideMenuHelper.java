package com.veganbeauty.app.utils;

import android.content.Context;
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

    private SideMenuHelper() {
    }

    public static void bindCurrentUser(@NonNull View menuRoot) {
        Context context = menuRoot.getContext().getApplicationContext();
        applyUserInfo(menuRoot, context, null);

        EXECUTOR.execute(() -> {
            UserEntity user = ProfileSessionHelper.findCurrentUser(context);
            menuRoot.post(() -> applyUserInfo(menuRoot, context, user));
        });
    }

    private static void applyUserInfo(@NonNull View menuRoot, Context context, @Nullable UserEntity user) {
        ImageView ivAvatar = menuRoot.findViewById(R.id.ivSideMenuAvatar);
        TextView tvDisplayName = menuRoot.findViewById(R.id.tvSideMenuDisplayName);
        TextView tvUsername = menuRoot.findViewById(R.id.tvSideMenuUsername);

        String displayName = ProfileSession.getFullName(context);
        String username = ProfileSession.getUsername(context);
        String avatarUrl = ProfileSession.getAvatar(context);
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            avatarUrl = ProfileSession.getPrimaryImage(context);
        }

        if (user != null) {
            if (user.getFull_name() != null && !user.getFull_name().trim().isEmpty()) {
                displayName = user.getFull_name().trim();
            }
            if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
                username = user.getUsername().trim();
            }
            String resolvedAvatar = ProfileSessionHelper.resolveAvatarUrl(user);
            if (!resolvedAvatar.isEmpty()) {
                avatarUrl = resolvedAvatar;
            }
        }

        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = "Người dùng";
        }

        if (tvDisplayName != null) {
            tvDisplayName.setText(displayName);
        }
        if (tvUsername != null) {
            tvUsername.setText(formatHandle(username));
        }
        if (ivAvatar != null) {
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                Glide.with(ivAvatar.getContext())
                        .load(avatarUrl)
                        .placeholder(R.drawable.img_avatar)
                        .error(R.drawable.img_avatar)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.img_avatar);
            }
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
