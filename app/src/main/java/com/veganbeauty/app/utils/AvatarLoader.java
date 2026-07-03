package com.veganbeauty.app.utils;

import android.widget.ImageView;


import java.io.File;

public class AvatarLoader {

    public static void loadAvatar(ImageView imageView, String avatarUrl) {
        loadAvatar(imageView, avatarUrl, null);
    }

    public static void loadAvatar(ImageView imageView, String avatarUrl, @androidx.annotation.Nullable String fallbackUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
                loadAvatar(imageView, fallbackUrl, null);
                return;
            }
            com.bumptech.glide.Glide.with(imageView.getContext()).load(com.veganbeauty.app.R.drawable.img_avatar).circleCrop().into(imageView);
            return;
        }

        String finalUrl;
        if (avatarUrl.startsWith("file://")) {
            String path = avatarUrl.substring(7);
            File file = new File(path);
            if (file.exists()) {
                finalUrl = avatarUrl;
                
                // Use ObjectKey signature to bust cache for local files instead of appending ?t=
                com.bumptech.glide.Glide.with(imageView.getContext())
                        .load(finalUrl)
                        .signature(new com.bumptech.glide.signature.ObjectKey(file.lastModified()))
                        .placeholder(android.R.color.darker_gray)
                        .error(com.veganbeauty.app.R.drawable.img_avatar)
                        .circleCrop()
                        .into(imageView);
                return;
            } else {
                finalUrl = avatarUrl;
            }
        } else {
            finalUrl = avatarUrl;
        }

        final String nextFallback = fallbackUrl;
        com.bumptech.glide.Glide.with(imageView.getContext())
                .load(finalUrl)
                .placeholder(android.R.color.darker_gray)
                .error(com.veganbeauty.app.R.drawable.img_avatar)
                .circleCrop()
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e,
                                                Object model,
                                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                boolean isFirstResource) {
                        if (nextFallback != null && !nextFallback.isEmpty() && !nextFallback.equals(finalUrl)) {
                            loadAvatar(imageView, nextFallback, null);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                   Object model,
                                                   com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                                   com.bumptech.glide.load.DataSource dataSource,
                                                   boolean isFirstResource) {
                        return false;
                    }
                })
                .into(imageView);
    }
}
