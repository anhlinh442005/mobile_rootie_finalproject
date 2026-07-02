package com.veganbeauty.app.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ImageSaveHelper {

    public interface Callback {
        void onSuccess();
        void onError(String message);
    }

    private ImageSaveHelper() {
    }

    public static void saveImageFromUrl(@NonNull Context context, @NonNull String imageUrl, @NonNull String label, @NonNull Callback callback) {
        String trimmedUrl = imageUrl.trim();
        if (trimmedUrl.isEmpty()) {
            callback.onError("Không có ảnh để lưu");
            return;
        }

        Glide.with(context)
                .asBitmap()
                .load(trimmedUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            saveBitmap(context, resource, label);
                            callback.onSuccess();
                        } catch (IOException e) {
                            callback.onError("Không thể lưu ảnh");
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        callback.onError("Không thể tải ảnh");
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private static void saveBitmap(@NonNull Context context, @NonNull Bitmap bitmap, @NonNull String label) throws IOException {
        String safeLabel = label.trim().isEmpty() ? "anh" : label.trim().toLowerCase(Locale.US).replaceAll("\\s+", "_");
        String fileName = "rootie_" + safeLabel + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Rootie");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri imageUri = context.getContentResolver().insert(collection, values);
            if (imageUri == null) {
                throw new IOException("Cannot create media entry");
            }

            try (OutputStream outputStream = context.getContentResolver().openOutputStream(imageUri)) {
                if (outputStream == null) {
                    throw new IOException("Cannot open output stream");
                }
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                    throw new IOException("Bitmap compress failed");
                }
            }

            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            context.getContentResolver().update(imageUri, values, null, null);
            return;
        }

        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File appDir = new File(picturesDir, "Rootie");
        if (!appDir.exists() && !appDir.mkdirs()) {
            throw new IOException("Cannot create directory");
        }

        File imageFile = new File(appDir, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                throw new IOException("Bitmap compress failed");
            }
        }

        MediaScannerConnection.scanFile(
                context,
                new String[]{imageFile.getAbsolutePath()},
                new String[]{"image/jpeg"},
                null
        );
    }
}
