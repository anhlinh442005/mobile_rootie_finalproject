package com.veganbeauty.app.utils;

import android.content.Context;
import android.net.Uri;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Upload ảnh avatar lên Cloudinary bằng unsigned preset (không cần API Secret trên app).
 */
public final class CloudinaryUploadHelper {

  private static final String UPLOAD_URL_TEMPLATE =
      "https://api.cloudinary.com/v1_1/%s/image/upload";

  private CloudinaryUploadHelper() {}

  public static String uploadAvatarFile(File imageFile, String userId) throws Exception {
    if (!CloudinaryConfig.isConfigured()) {
      throw new IllegalStateException("Chưa cấu hình Cloudinary cloud_name / upload_preset");
    }
    if (imageFile == null || !imageFile.exists() || imageFile.length() == 0) {
      throw new IllegalArgumentException("File ảnh không hợp lệ");
    }
    if (userId == null || userId.trim().isEmpty()) {
      throw new IllegalArgumentException("Thiếu user_id");
    }

    String boundary = "----RootieBoundary" + System.currentTimeMillis();
    String uploadUrl =
        String.format(UPLOAD_URL_TEMPLATE, CloudinaryConfig.CLOUDINARY_CLOUD_NAME.trim());

    HttpURLConnection connection = null;
    try {
      URL url = new URL(uploadUrl);
      connection = (HttpURLConnection) url.openConnection();
      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setUseCaches(false);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
      connection.setConnectTimeout(30_000);
      connection.setReadTimeout(60_000);

      String folder = "rootie/avatars/" + userId.trim();
      String publicId = "avatar_" + System.currentTimeMillis();

      try (DataOutputStream output = new DataOutputStream(connection.getOutputStream())) {
        writeFormField(output, boundary, "upload_preset", CloudinaryConfig.CLOUDINARY_UPLOAD_PRESET.trim());
        writeFormField(output, boundary, "folder", folder);
        writeFormField(output, boundary, "public_id", publicId);
        writeFileField(output, boundary, "file", imageFile.getName(), "image/jpeg", imageFile);
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        output.flush();
      }

      int responseCode = connection.getResponseCode();
      InputStream responseStream =
          responseCode >= 200 && responseCode < 300
              ? connection.getInputStream()
              : connection.getErrorStream();

      String responseBody = readStream(responseStream);
      if (responseCode < 200 || responseCode >= 300) {
        throw new Exception("Cloudinary upload failed (" + responseCode + "): " + responseBody);
      }

      JSONObject json = new JSONObject(responseBody);
      String secureUrl = json.optString("secure_url", "");
      if (secureUrl.isEmpty()) {
        throw new Exception("Cloudinary không trả về secure_url");
      }
      return secureUrl;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  public static File resolveImageFile(Context context, Uri fileUri) throws Exception {
    if (fileUri == null) {
      throw new IllegalArgumentException("Uri ảnh null");
    }
    String uriString = fileUri.toString();
    if (uriString.startsWith("file://")) {
      File file = new File(Uri.parse(uriString).getPath());
      if (file.exists()) {
        return file;
      }
    }
    File dest = new File(context.getCacheDir(), "cloudinary_upload_" + System.currentTimeMillis() + ".jpg");
    try (InputStream in = context.getContentResolver().openInputStream(fileUri);
        java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
      if (in == null) {
        throw new IllegalArgumentException("Không đọc được ảnh từ Uri");
      }
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
    }
    return dest;
  }

  private static void writeFormField(
      DataOutputStream output, String boundary, String name, String value) throws Exception {
    output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
    output.write(
        ("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n")
            .getBytes(StandardCharsets.UTF_8));
    output.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
  }

  private static void writeFileField(
      DataOutputStream output,
      String boundary,
      String fieldName,
      String fileName,
      String mimeType,
      File file)
      throws Exception {
    output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
    output.write(
        ("Content-Disposition: form-data; name=\""
                + fieldName
                + "\"; filename=\""
                + fileName
                + "\"\r\n")
            .getBytes(StandardCharsets.UTF_8));
    output.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));

    try (FileInputStream input = new FileInputStream(file)) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        output.write(buffer, 0, read);
      }
    }
    output.write("\r\n".getBytes(StandardCharsets.UTF_8));
  }

  private static String readStream(InputStream stream) throws Exception {
    if (stream == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
    }
    return sb.toString();
  }
}
