package com.veganbeauty.app.utils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Upload ảnh feedback/review lên Cloudinary (free tier, app đã dùng cho avatar).
 * Không dùng Firebase Storage — chỉ lưu URL https vào Firestore.
 */
public final class FeedbackImageUploadHelper {

    private FeedbackImageUploadHelper() {
    }

    public static List<String> ensureRemoteUrls(Context context, List<String> paths, String storageFolder) throws Exception {
        List<String> result = new ArrayList<>();
        if (paths == null || paths.isEmpty()) {
            return result;
        }
        if (!CloudinaryConfig.isConfigured()) {
            throw new IllegalStateException("Chưa cấu hình Cloudinary — không thể đồng bộ ảnh feedback");
        }

        String folder = storageFolder != null ? storageFolder.trim() : "rootie/feedback";
        if (folder.isEmpty()) {
            folder = "rootie/feedback";
        }
        if (!folder.startsWith("rootie/")) {
            folder = "rootie/" + folder;
        }

        for (String path : paths) {
            if (path == null || path.trim().isEmpty()) {
                continue;
            }
            String trimmed = path.trim();
            if (isRemoteUrl(trimmed)) {
                result.add(trimmed);
                continue;
            }

            File file = resolveFile(context, trimmed);
            if (file == null || !file.exists() || file.length() == 0) {
                continue;
            }

            String publicId = "img_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            String secureUrl = CloudinaryUploadHelper.uploadImageFile(file, folder, publicId);
            if (secureUrl != null && !secureUrl.trim().isEmpty()) {
                result.add(secureUrl.trim());
            }
        }
        return result;
    }

    private static boolean isRemoteUrl(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static File resolveFile(Context context, String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("file://")) {
            return new File(Uri.parse(trimmed).getPath());
        }
        if (trimmed.startsWith("/")) {
            return new File(trimmed);
        }
        if (context != null) {
            try {
                return CloudinaryUploadHelper.resolveImageFile(context, Uri.parse(trimmed));
            } catch (Exception ignored) {
                // fall through
            }
        }
        return new File(trimmed);
    }
}
