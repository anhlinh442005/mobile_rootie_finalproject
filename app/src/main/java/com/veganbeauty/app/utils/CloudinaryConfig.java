package com.veganbeauty.app.utils;

/**
 * Cấu hình Cloudinary unsigned upload (demo sinh viên).
 * Điền cloud_name và upload_preset sau khi tạo trên Cloudinary Dashboard.
 */
public final class CloudinaryConfig {

  /** Thay bằng cloud name thật từ Cloudinary Dashboard. */
  public static final String CLOUDINARY_CLOUD_NAME = "pkd8qodm";

  /** Thay bằng unsigned upload preset thật (Settings → Upload → Upload presets). */
  public static final String CLOUDINARY_UPLOAD_PRESET = "rootie_avatar_unsigned";

  private CloudinaryConfig() {}

  public static boolean isConfigured() {
    return CLOUDINARY_CLOUD_NAME != null
        && !CLOUDINARY_CLOUD_NAME.trim().isEmpty()
        && !"YOUR_CLOUD_NAME".equals(CLOUDINARY_CLOUD_NAME.trim())
        && CLOUDINARY_UPLOAD_PRESET != null
        && !CLOUDINARY_UPLOAD_PRESET.trim().isEmpty()
        && !"YOUR_UNSIGNED_UPLOAD_PRESET".equals(CLOUDINARY_UPLOAD_PRESET.trim());
  }
}
