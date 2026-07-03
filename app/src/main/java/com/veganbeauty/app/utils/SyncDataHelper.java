package com.veganbeauty.app.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.remote.FirestoreService;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncDataHelper {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface SyncCallback {
        void onComplete(boolean success);
    }

    public interface UploadCallback {
        void onResult(@Nullable String secureUrl);
    }

    public interface AvatarSyncCallback {
        void onComplete(boolean success, @Nullable String secureUrl, @Nullable String errorMessage);
    }

    public static void syncRewardPointsToFirestore(Context context) {
    }

    public static void syncRewardPointsFromFirestore(Context context) {
    }

    public static void syncUserProfileFromFirestore(Context context, Runnable callback) {
        EXECUTOR.execute(() -> {
            pullUserProfileFromFirestore(context);
            runOnMainThread(callback);
        });
    }

    /** Gọi trên background thread khi cần fetch đồng bộ trước khi load UI. */
    public static void pullUserProfileFromFirestoreSync(Context context) {
        pullUserProfileFromFirestore(context);
    }

    private static void pullUserProfileFromFirestore(Context context) {
        try {
            if (!ProfileSession.isLoggedIn(context)) {
                return;
            }
            String userId = resolveCurrentUserId(context);
            if (userId == null || userId.trim().isEmpty()) {
                return;
            }

            DocumentSnapshot doc = Tasks.await(
                    FirebaseFirestore.getInstance().collection("users").document(userId.trim()).get()
            );
            if (!doc.exists()) {
                return;
            }

            String avatar = doc.getString("avatar");
            if (avatar == null) {
                avatar = doc.getString("avatar_url");
            }

            UserEntity user = ProfileSessionHelper.findCurrentUser(context);
            if (user == null) {
                user = new UserEntity(
                        doc.getId(),
                        doc.getString("username") != null ? doc.getString("username") : "",
                        doc.getString("full_name") != null ? doc.getString("full_name") : "",
                        doc.getString("email") != null ? doc.getString("email") : "",
                        doc.getString("phone") != null ? doc.getString("phone") : "",
                        "",
                        isRemoteAvatarUrl(avatar) ? avatar.trim() : "",
                        doc.getString("primary_image")
                );
                if (doc.getString("bio") != null && !doc.getString("bio").trim().isEmpty()) {
                    user.setBio(doc.getString("bio").trim());
                }
            } else {
                if (doc.getString("full_name") != null && !doc.getString("full_name").trim().isEmpty()) {
                    user.setFull_name(doc.getString("full_name").trim());
                }
                if (doc.getString("username") != null && !doc.getString("username").trim().isEmpty()) {
                    user.setUsername(doc.getString("username").trim());
                }
                if (doc.getString("phone") != null && !doc.getString("phone").trim().isEmpty()) {
                    user.setPhone(doc.getString("phone").trim());
                }
                if (doc.getString("bio") != null && !doc.getString("bio").trim().isEmpty()) {
                    user.setBio(doc.getString("bio").trim());
                }
                if (doc.getString("primary_image") != null && !doc.getString("primary_image").trim().isEmpty()) {
                    user.setPrimary_image(doc.getString("primary_image").trim());
                }
                if (isRemoteAvatarUrl(avatar)) {
                    user.setAvatar(avatar.trim());
                }
            }

            ProfileSessionHelper.syncSessionFromUser(context, user);
            RootieDatabase.getDatabase(context).userDao().insertUserSync(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isRemoteAvatarUrl(@Nullable String url) {
        if (url == null) {
            return false;
        }
        String value = url.trim();
        return value.startsWith("https://") || value.startsWith("http://");
    }

    private static String resolveCurrentUserId(Context context) {
        String userId = ProfileSessionHelper.getEffectiveUserId(context);
        if (userId == null || userId.trim().isEmpty()) {
            userId = ProfileSession.getUserId(context);
        }
        return userId;
    }

    private static void runOnMainThread(Runnable callback) {
        if (callback == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(callback);
    }

    public static void syncUserProfileToFirebaseAndLocal(Context context) {
        EXECUTOR.execute(() -> syncUserProfileToFirebaseAndLocalBlocking(context));
    }

    private static boolean syncUserProfileToFirebaseAndLocalBlocking(Context context) {
        try {
            String userId = resolveCurrentUserId(context);
            if (userId == null || userId.isEmpty()) {
                userId = "user_" + UUID.randomUUID();
                ProfileSession.setUserId(context, userId);
            }

            String avatar = resolveRemoteAvatarForSync(context);

            UserEntity userEntity = ProfileSessionHelper.findCurrentUser(context);
            if (userEntity == null) {
                userEntity = new UserEntity(
                        userId,
                        ProfileSession.getUsername(context) != null ? ProfileSession.getUsername(context) : "",
                        ProfileSession.getFullName(context) != null ? ProfileSession.getFullName(context) : "",
                        ProfileSession.getEmail(context) != null ? ProfileSession.getEmail(context) : "",
                        ProfileSession.getPhone(context) != null ? ProfileSession.getPhone(context) : "",
                        "",
                        avatar,
                        ProfileSession.getPrimaryImage(context)
                );
            } else {
                userEntity.setFull_name(ProfileSession.getFullName(context) != null ? ProfileSession.getFullName(context) : userEntity.getFull_name());
                userEntity.setUsername(ProfileSession.getUsername(context) != null ? ProfileSession.getUsername(context) : userEntity.getUsername());
                userEntity.setEmail(ProfileSession.getEmail(context) != null ? ProfileSession.getEmail(context) : userEntity.getEmail());
                userEntity.setPhone(ProfileSession.getPhone(context) != null ? ProfileSession.getPhone(context) : userEntity.getPhone());
                if (isRemoteAvatarUrl(avatar)) {
                    userEntity.setAvatar(avatar);
                }
            }

            RootieDatabase.getDatabase(context).userDao().insertUserSync(userEntity);
            return new FirestoreService().saveUser(userEntity);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String resolveRemoteAvatarForSync(Context context) {
        String sessionAvatar = ProfileSession.getAvatar(context);
        if (isRemoteAvatarUrl(sessionAvatar)) {
            return sessionAvatar.trim();
        }
        UserEntity existing = ProfileSessionHelper.findCurrentUser(context);
        if (existing != null && isRemoteAvatarUrl(existing.getAvatar())) {
            return existing.getAvatar().trim();
        }
        return "";
    }

    /** Chỉ upload lên Cloudinary, trả secure_url. Không ghi session/Firestore. */
    public static void uploadAvatarToCloudinary(Context context, Uri fileUri, UploadCallback callback) {
        if (fileUri == null) {
            if (callback != null) {
                callback.onResult(null);
            }
            return;
        }
        if (!CloudinaryConfig.isConfigured()) {
            if (callback != null) {
                callback.onResult(null);
            }
            return;
        }

        EXECUTOR.execute(() -> {
            String secureUrl = null;
            try {
                String userId = resolveCurrentUserId(context);
                if (userId == null || userId.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onResult(null);
                    }
                    return;
                }
                File imageFile = CloudinaryUploadHelper.resolveImageFile(context, fileUri);
                secureUrl = CloudinaryUploadHelper.uploadAvatarFile(imageFile, userId.trim());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (callback != null) {
                callback.onResult(secureUrl);
            }
        });
    }

    /** Sau khi có secure_url: cập nhật ProfileSession, Room và Firestore field avatar. */
    public static void applyAvatarSecureUrl(Context context, String secureUrl, SyncCallback callback) {
        EXECUTOR.execute(() -> {
            boolean success = false;
            try {
                if (!isRemoteAvatarUrl(secureUrl)) {
                    if (callback != null) {
                        boolean finalSuccess = false;
                        runOnMainThread(() -> callback.onComplete(finalSuccess));
                    }
                    return;
                }

                String userId = resolveCurrentUserId(context);
                if (userId == null || userId.trim().isEmpty()) {
                    if (callback != null) {
                        runOnMainThread(() -> callback.onComplete(false));
                    }
                    return;
                }

                String remoteUrl = secureUrl.trim();
                ProfileSession.setAvatar(context, remoteUrl);

                UserEntity user = ProfileSessionHelper.findCurrentUser(context);
                if (user == null) {
                    user = new UserEntity(
                            userId,
                            ProfileSession.getUsername(context) != null ? ProfileSession.getUsername(context) : "",
                            ProfileSession.getFullName(context) != null ? ProfileSession.getFullName(context) : "",
                            ProfileSession.getEmail(context) != null ? ProfileSession.getEmail(context) : "",
                            ProfileSession.getPhone(context) != null ? ProfileSession.getPhone(context) : "",
                            "",
                            remoteUrl,
                            ProfileSession.getPrimaryImage(context)
                    );
                } else {
                    user.setAvatar(remoteUrl);
                }

                RootieDatabase.getDatabase(context).userDao().insertUserSync(user);
                success = new FirestoreService().updateUserAvatar(userId, remoteUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }

            boolean finalSuccess = success;
            if (callback != null) {
                runOnMainThread(() -> callback.onComplete(finalSuccess));
            }
        });
    }

    /** Upload Cloudinary rồi đồng bộ session/Room/Firestore. */
    public static void uploadAndSyncAvatar(Context context, Uri fileUri, AvatarSyncCallback callback) {
        uploadAvatarToCloudinary(context, fileUri, secureUrl -> {
            if (secureUrl == null) {
                if (callback != null) {
                    runOnMainThread(() -> callback.onComplete(false, null, "Upload Cloudinary thất bại"));
                }
                return;
            }
            applyAvatarSecureUrl(context, secureUrl, success -> {
                if (callback != null) {
                    String message = success ? null : "Cập nhật Firestore thất bại";
                    callback.onComplete(success, secureUrl, message);
                }
            });
        });
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
                new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(finalSuccess));
            }
        });
    }
}
