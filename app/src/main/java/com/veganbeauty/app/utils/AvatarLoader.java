package com.veganbeauty.app.utils;

import android.widget.ImageView;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

import java.io.File;

public class AvatarLoader {

    public static void loadAvatar(ImageView imageView, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(imageView.getContext())
                    .data(com.veganbeauty.app.R.drawable.img_avatar)
                    .transformations(new CircleCropTransformation())
                    .target(imageView)
                    .build();
            Coil.imageLoader(imageView.getContext()).enqueue(request);
            return;
        }

        String finalUrl;
        if (avatarUrl.startsWith("file://")) {
            String path = avatarUrl.substring(7);
            File file = new File(path);
            if (file.exists()) {
                finalUrl = "file://" + file.getAbsolutePath() + "?t=" + file.lastModified();
            } else {
                finalUrl = avatarUrl;
            }
        } else {
            finalUrl = avatarUrl;
        }

        ImageRequest request = new ImageRequest.Builder(imageView.getContext())
                .data(finalUrl)
                .crossfade(true)
                .transformations(new CircleCropTransformation())
                .placeholder(android.R.color.darker_gray)
                .error(com.veganbeauty.app.R.drawable.img_avatar)
                .target(imageView)
                .build();
        Coil.imageLoader(imageView.getContext()).enqueue(request);
    }
}
