package com.veganbeauty.app.utils;

import android.content.Context;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.ProfileSession;

import java.util.UUID;
import java.io.File;

import java.util.List;
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
        EXECUTOR.execute(() -> {
            try {
                String userId = ProfileSession.INSTANCE.getUserId(context);
                if (userId == null || userId.isEmpty()) {
                    userId = "user_" + UUID.randomUUID().toString();
                    ProfileSession.INSTANCE.setUserId(context, userId);
                }

                UserEntity userEntity = new UserEntity(
                        userId,
                        ProfileSession.INSTANCE.getFullName(context), // Username fallback
                        ProfileSession.INSTANCE.getFullName(context),
                        ProfileSession.INSTANCE.getEmail(context),
                        ProfileSession.INSTANCE.getPhone(context),
                        "", // Password not synced here
                        ProfileSession.INSTANCE.getAvatar(context),
                        null
                );

                // Must update local Room database FIRST so it doesn't get overridden by old data when UI resumes immediately
                com.veganbeauty.app.data.local.RootieDatabase.getDatabase(context).userDao().insertUserSync(userEntity);

                FirestoreService firestoreService = new FirestoreService();
                firestoreService.saveUser(userEntity);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public interface UploadCallback {
        void onResult(String downloadUrl);
    }

    public static void uploadAvatarToFirebase(Context context, android.net.Uri fileUri, UploadCallback callback) {
        if (fileUri == null) {
            if (callback != null) callback.onResult(null);
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                String fileName = "avatars/avatar_" + UUID.randomUUID().toString() + ".jpg";
                StorageReference ref = storage.getReference().child(fileName);
                
                android.net.Uri uriToUpload = fileUri;
                if (fileUri.toString().startsWith("file://")) {
                    File file = new File(fileUri.getPath());
                    uriToUpload = android.net.Uri.fromFile(file);
                }
                
                ref.putFile(uriToUpload)
                        .continueWithTask(task -> {
                            if (!task.isSuccessful() && task.getException() != null) {
                                throw task.getException();
                            }
                            return ref.getDownloadUrl();
                        })
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null) {
                                String downloadUrl = task.getResult().toString();
                                // MUST save to session so the Save button will catch it
                                ProfileSession.INSTANCE.setAvatar(context, downloadUrl);
                                if (callback != null) callback.onResult(downloadUrl);
                            } else {
                                if (callback != null) callback.onResult(null);
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) callback.onResult(null);
            }
        });
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

                String messageTemplateJson = reader.readAsset("community_message.json");
                String usersJson = reader.readAsset("users.json");
                if (messageTemplateJson != null && usersJson != null) {
                    List<com.veganbeauty.app.data.local.entities.ConversationEntity> allMessages =
                            com.veganbeauty.app.features.community.CommunityMessageSeeder
                                    .buildAllPersonalizedConversations(messageTemplateJson, usersJson);
                    if (!allMessages.isEmpty()) {
                        firestoreService.syncCommunityMessagesFromJson(
                                new com.google.gson.Gson().toJson(allMessages),
                                true
                        );
                    }
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
