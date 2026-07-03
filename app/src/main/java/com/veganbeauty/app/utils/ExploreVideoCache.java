package com.veganbeauty.app.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ExploreVideoCache {

    private static final String CACHE_DIR = "explore_videos";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    private ExploreVideoCache() {
    }

    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }

    @NonNull
    public static File getCacheFile(@NonNull Context context, @NonNull String url) {
        File dir = new File(context.getCacheDir(), CACHE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, hash(url) + ".mp4");
    }

    public static boolean isCached(@NonNull Context context, @NonNull String url) {
        File file = getCacheFile(context, url);
        return file.exists() && file.length() > 0;
    }

    @NonNull
    public static Uri getPlayableUri(@NonNull Context context, @NonNull String url) {
        File cached = getCacheFile(context, url);
        if (cached.exists() && cached.length() > 0) {
            return Uri.fromFile(cached);
        }
        return Uri.parse(url);
    }

    public static void prefetch(@NonNull Context context, @Nullable String url) {
        if (url == null || url.trim().isEmpty()) return;
        if (!isNetworkAvailable(context)) return;
        if (isCached(context, url)) return;

        EXECUTOR.execute(() -> {
            try {
                downloadToCache(context.getApplicationContext(), url);
            } catch (Exception ignored) {
            }
        });
    }

    private static void downloadToCache(@NonNull Context context, @NonNull String url) throws Exception {
        File target = getCacheFile(context, url);
        if (target.exists() && target.length() > 0) return;

        File temp = new File(target.getAbsolutePath() + ".tmp");
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return;

            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(temp)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            if (temp.length() > 0) {
                if (target.exists()) {
                    target.delete();
                }
                if (!temp.renameTo(target)) {
                    temp.delete();
                }
            } else {
                temp.delete();
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (temp.exists() && !target.exists()) {
                temp.delete();
            }
        }
    }

    @NonNull
    private static String hash(@NonNull String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
