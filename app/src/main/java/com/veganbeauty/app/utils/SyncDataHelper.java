package com.veganbeauty.app.utils;

import android.content.Context;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.remote.FirestoreService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncDataHelper {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface SyncCallback {
        void onComplete(boolean success);
    }

    public static void syncRewardPointsToFirestore(Context context) {
    }

    public static void syncRewardPointsFromFirestore(Context context) {
    }

    public static void syncUserProfileFromFirestore(Context context, Runnable callback) {
        if (callback != null) {
            callback.run();
        }
    }

    public static void syncUserProfileToFirebaseAndLocal(Context context) {
    }

    public interface UploadCallback {
        void onResult(String downloadUrl);
    }

    public static void uploadAvatarToFirebase(Context context, android.net.Uri fileUri, UploadCallback callback) {
        if (callback != null) callback.onResult(null);
    }

    /**
     * Wipes legacy Firebase collections and re-uploads canonical data from assets.
     */
    public static void syncAllLocalDataToFirebase(Context context) {
        syncAllLocalDataToFirebase(context, null);
    }

    public static void syncAllLocalDataToFirebase(Context context, SyncCallback callback) {
        EXECUTOR.execute(() -> {
            boolean success = false;
            try {
                FirestoreService firestoreService = new FirestoreService();
                LocalJsonReader reader = new LocalJsonReader(context.getApplicationContext());

                firestoreService.clearOldAffiliateData();
                firestoreService.wipeCollection("skin_bookings");

                String productsJson = reader.readAsset("products.json");
                if (productsJson != null) {
                    firestoreService.forceSyncProductsFromJson(productsJson);
                }

                String postsJson = reader.readAsset("community_posts.json");
                String newsJson = reader.readAsset("community_news.json");
                if (postsJson != null && newsJson != null) {
                    firestoreService.forceSyncLocalPostsToFirebase(postsJson, newsJson);
                }

                String reelsJson = reader.readAsset("community_reels_fb.json");
                if (reelsJson != null) {
                    firestoreService.forceSyncCollection("community_reels_fb", reelsJson, "video_id", null);
                }

                String videosJson = reader.readAsset("community_video_yt.json");
                if (videosJson != null) {
                    firestoreService.forceSyncCollection("community_video_yt", videosJson, "_id", null);
                }

                String ingredientsJson = reader.readAsset("ingredient.json");
                if (ingredientsJson != null) {
                    firestoreService.forceSyncCollection("ingredients", ingredientsJson, "slug", null);
                }

                String storesJson = reader.readAsset("rootie_stores.json");
                if (storesJson != null) {
                    firestoreService.forceSyncCollection("stores", storesJson, "ma_cua_hang", null);
                }

                String socialJson = reader.readAsset("User_com_friend.json");
                if (socialJson != null) {
                    firestoreService.syncUserSocialFromJson(socialJson);
                }

                success = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (callback != null) {
                boolean finalSuccess = success;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                        callback.onComplete(finalSuccess));
            }
        });
    }
}
