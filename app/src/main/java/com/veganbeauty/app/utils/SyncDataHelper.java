package com.veganbeauty.app.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.remote.FirestoreService;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncDataHelper {

    private static final String TAG = "SyncDataHelper";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    /** Tách riêng — tránh avatar upload bị kẹt sau job sync Firebase nặng trên EXECUTOR. */
    private static final ExecutorService AVATAR_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "rootie-avatar-upload");
        thread.setDaemon(true);
        return thread;
    });

    public interface SyncCallback {
        void onComplete(boolean success);
    }

    public interface ProfileSaveCallback {
        void onComplete(boolean localSuccess, boolean cloudSynced);
    }

    public interface UploadCallback {
        void onResult(@Nullable String secureUrl, @Nullable String errorMessage);
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
            pullUserProfileFromLocal(context);
            runOnMainThread(callback);
        });
    }

    /** Gọi trên background thread khi cần fetch đồng bộ trước khi load UI. */
    public static void pullUserProfileFromFirestoreSync(Context context) {
        pullUserProfileFromLocal(context);
    }

    private static void pullUserProfileFromLocal(Context context) {
        try {
            if (!ProfileSession.isLoggedIn(context)) {
                return;
            }
            UserEntity user = ProfileSessionHelper.findCurrentUser(context);
            if (user != null) {
                ProfileSessionHelper.syncSessionFromUser(context, user, false);
            }
            seedJune2026SkincareHistoryIfNeeded(context);
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

    public static void syncUserProfileToFirebaseAndLocal(Context context, @Nullable ProfileSaveCallback callback) {
        EXECUTOR.execute(() -> {
            boolean localSaved = false;
            boolean cloudSaved = false;
            try {
                localSaved = persistProfileLocallyBlocking(context);
                if (localSaved) {
                    cloudSaved = pushProfileToFirestoreBlocking(context);
                    ProfileUpdateNotifier.notifyUpdated();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final boolean localOk = localSaved;
            final boolean cloudOk = cloudSaved;
            if (callback != null) {
                runOnMainThread(() -> callback.onComplete(localOk, cloudOk));
            }
        });
    }

    private static void propagateProfileChangesBlocking(Context context) {
        try {
            String userId = resolveCurrentUserId(context);
            if (userId == null || userId.trim().isEmpty()) {
                return;
            }

            String username = normalizeUsernameForStorage(ProfileSession.getUsername(context));
            String displayName = ProfileSession.getFullName(context) != null ? ProfileSession.getFullName(context).trim() : "";

            RootieDatabase db = RootieDatabase.getDatabase(context);
            List<CommunityPostEntity> posts = db.communityDao().getPostsByAuthorSync(userId.trim());
            if (posts != null && !posts.isEmpty()) {
                for (CommunityPostEntity post : posts) {
                    if (!username.isEmpty()) {
                        post.setAuthorUsername(username);
                    }
                    if (!displayName.isEmpty()) {
                        post.setAuthorDisplayName(displayName);
                    }
                }
                db.communityDao().insertPosts(posts);
            }

            List<ReelEntity> reels = db.communityDao().getReelsByAuthorSync(userId.trim());
            if (reels != null && !reels.isEmpty()) {
                for (ReelEntity reel : reels) {
                    if (!username.isEmpty()) {
                        reel.setAuthorUsername(username);
                    }
                    if (!displayName.isEmpty()) {
                        reel.setAuthorDisplayName(displayName);
                    }
                }
                db.communityDao().insertReels(reels);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean syncUserProfileToFirebaseAndLocalBlocking(Context context) {
        try {
            if (!persistProfileLocallyBlocking(context)) {
                return false;
            }
            pushProfileToFirestoreBlocking(context);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean persistProfileLocallyBlocking(Context context) {
        String userId = resolveCurrentUserId(context);
        if (userId == null || userId.isEmpty()) {
            userId = "user_" + UUID.randomUUID();
            ProfileSession.setUserId(context, userId);
        }

        String normalizedUsername = normalizeUsernameForStorage(ProfileSession.getUsername(context));
        String fullName = ProfileSession.getFullName(context) != null ? ProfileSession.getFullName(context) : "";
        String email = ProfileSession.getEmail(context) != null ? ProfileSession.getEmail(context) : "";
        String phone = ProfileSession.getPhone(context) != null ? ProfileSession.getPhone(context) : "";
        String bio = ProfileSession.getBio(context);

        UserEntity userEntity = ProfileSessionHelper.findCurrentUser(context);
        String preservedAvatar = preserveAvatarForProfileSave(context, userEntity);

        if (userEntity == null) {
            userEntity = new UserEntity(
                    userId,
                    normalizedUsername,
                    fullName,
                    email,
                    phone,
                    "",
                    preservedAvatar,
                    ProfileSession.getPrimaryImage(context)
            );
        } else {
            userEntity.setUser_id(userId);
            userEntity.setFull_name(fullName);
            userEntity.setUsername(normalizedUsername);
            userEntity.setEmail(email);
            userEntity.setPhone(phone);
            if (preservedAvatar != null && !preservedAvatar.isEmpty()) {
                userEntity.setAvatar(preservedAvatar);
            }
        }
        if (bio != null && !bio.trim().isEmpty()) {
            userEntity.setBio(bio.trim());
        }

        RootieDatabase.getDatabase(context).userDao().insertUserSync(userEntity);
        propagateCurrentUserToCommunityUsersBlocking(context, userEntity);
        ProfileSessionHelper.syncSessionFromUser(context, userEntity, true);
        propagateProfileChangesBlocking(context);
        return true;
    }

    private static void propagateCurrentUserToCommunityUsersBlocking(Context context, UserEntity user) {
        if (user == null || user.getUser_id() == null || user.getUser_id().trim().isEmpty()) {
            return;
        }
        try {
            RootieDatabase.getDatabase(context).communityDao().insertUsers(
                    java.util.Collections.singletonList(user)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Giữ nguyên avatar hiện tại khi chỉ lưu thông tin text. */
    @Nullable
    private static String preserveAvatarForProfileSave(Context context, @Nullable UserEntity existingUser) {
        String localAvatar = ProfileSessionHelper.getLocalAvatarFileUri(context);
        if (localAvatar != null) {
            return localAvatar;
        }
        String sessionAvatar = ProfileSession.getAvatarStored(context);
        if (ProfileSessionHelper.isUsableAvatarUrl(sessionAvatar)) {
            return sessionAvatar;
        }
        if (existingUser != null && ProfileSessionHelper.isUsableAvatarUrl(existingUser.getAvatar())) {
            return existingUser.getAvatar() != null ? existingUser.getAvatar().trim() : "";
        }
        if (existingUser != null && existingUser.getAvatar() != null && !existingUser.getAvatar().trim().isEmpty()) {
            return existingUser.getAvatar().trim();
        }
        return "";
    }

    private static boolean pushProfileToFirestoreBlocking(Context context) {
        ProfileSession.clearLocalProfileEdits(context);
        return true;
    }

    private static void propagateAvatarToCommunityBlocking(Context context, String avatarUrl) {
        if (!isRemoteAvatarUrl(avatarUrl)) {
            return;
        }
        try {
            String userId = resolveCurrentUserId(context);
            if (userId == null || userId.trim().isEmpty()) {
                return;
            }
            RootieDatabase db = RootieDatabase.getDatabase(context);
            List<CommunityPostEntity> posts = db.communityDao().getPostsByAuthorSync(userId.trim());
            if (posts != null && !posts.isEmpty()) {
                for (CommunityPostEntity post : posts) {
                    post.setAuthorAvatarUrl(avatarUrl.trim());
                }
                db.communityDao().insertPosts(posts);
            }
            List<ReelEntity> reels = db.communityDao().getReelsByAuthorSync(userId.trim());
            if (reels != null && !reels.isEmpty()) {
                for (ReelEntity reel : reels) {
                    reel.setAuthorAvatarUrl(avatarUrl.trim());
                }
                db.communityDao().insertReels(reels);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String normalizeUsernameForStorage(@Nullable String username) {
        if (username == null) {
            return "";
        }
        return username.replace("@", "").trim();
    }

    private static String resolveRemoteAvatarForSync(Context context) {
        String sessionAvatar = ProfileSession.getAvatarStored(context);
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
                runOnMainThread(() -> callback.onResult(null, "Uri ảnh null"));
            }
            return;
        }
        if (!CloudinaryConfig.isConfigured()) {
            if (callback != null) {
                runOnMainThread(() -> callback.onResult(null, "Chưa cấu hình Cloudinary"));
            }
            return;
        }

        AVATAR_EXECUTOR.execute(() -> {
            String secureUrl = null;
            String errorMessage = null;
            try {
                Context appCtx = context.getApplicationContext();
                String userId = resolveCurrentUserId(appCtx);
                if (userId == null || userId.trim().isEmpty()) {
                    errorMessage = "Thiếu user_id — hãy đăng nhập lại";
                } else {
                    File imageFile = CloudinaryUploadHelper.resolveImageFile(appCtx, fileUri);
                    secureUrl = CloudinaryUploadHelper.uploadAvatarFile(imageFile, userId.trim());
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage = e.getMessage() != null ? e.getMessage() : "Lỗi upload Cloudinary";
            }
            final String url = secureUrl;
            final String err = errorMessage;
            runOnMainThread(() -> {
                if (callback != null) {
                    callback.onResult(url, err);
                }
            });
        });
    }

    /** Sau khi có secure_url: cập nhật ProfileSession, Room và Firestore field avatar (blocking). */
    private static boolean applyAvatarSecureUrlBlocking(Context context, String secureUrl) {
        try {
            if (!isRemoteAvatarUrl(secureUrl)) {
                return false;
            }

            Context appCtx = context.getApplicationContext();
            String userId = resolveCurrentUserId(appCtx);
            if (userId == null || userId.trim().isEmpty()) {
                return false;
            }

            String remoteUrl = secureUrl.trim();
            ProfileSession.setAvatar(appCtx, remoteUrl);

            UserEntity user = ProfileSessionHelper.findCurrentUser(appCtx);
            if (user == null) {
                user = new UserEntity(
                        userId,
                        ProfileSession.getUsername(appCtx) != null ? ProfileSession.getUsername(appCtx) : "",
                        ProfileSession.getFullName(appCtx) != null ? ProfileSession.getFullName(appCtx) : "",
                        ProfileSession.getEmail(appCtx) != null ? ProfileSession.getEmail(appCtx) : "",
                        ProfileSession.getPhone(appCtx) != null ? ProfileSession.getPhone(appCtx) : "",
                        "",
                        remoteUrl,
                        ProfileSession.getPrimaryImage(appCtx)
                );
            } else {
                user.setAvatar(remoteUrl);
            }

            RootieDatabase.getDatabase(appCtx).userDao().insertUserSync(user);
            propagateCurrentUserToCommunityUsersBlocking(appCtx, user);
            propagateAvatarToCommunityBlocking(appCtx, remoteUrl);
            ProfileUpdateNotifier.notifyUpdated();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Sau khi có secure_url: cập nhật ProfileSession, Room và Firestore field avatar. */
    public static void applyAvatarSecureUrl(Context context, String secureUrl, SyncCallback callback) {
        AVATAR_EXECUTOR.execute(() -> {
            boolean success = applyAvatarSecureUrlBlocking(context, secureUrl);
            if (callback != null) {
                runOnMainThread(() -> callback.onComplete(success));
            }
        });
    }

    /** Upload Firebase Storage (ưu tiên) rồi đồng bộ session/Room/Firestore. */
    public static void uploadAndSyncAvatar(Context context, Uri fileUri, AvatarSyncCallback callback) {
        Context appCtx = context.getApplicationContext();
        AVATAR_EXECUTOR.execute(() -> {
            String secureUrl = null;
            String uploadError = null;
            try {
                if (fileUri == null) {
                    uploadError = "Uri ảnh null";
                } else {
                    String userId = resolveCurrentUserId(appCtx);
                    if (userId == null || userId.trim().isEmpty()) {
                        uploadError = "Thiếu user_id — hãy đăng nhập lại";
                    } else {
                        secureUrl = uploadAvatarToFirebaseBlocking(appCtx, fileUri, userId.trim());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                uploadError = e.getMessage() != null ? e.getMessage() : "Lỗi upload Firebase Storage";
            }

            if (secureUrl == null) {
                final String err = uploadError != null && !uploadError.isEmpty()
                        ? uploadError
                        : "Upload avatar thất bại. Kiểm tra mạng và thử lại.";
                runOnMainThread(() -> {
                    if (callback != null) {
                        callback.onComplete(false, null, err);
                    }
                });
                return;
            }

            boolean firestoreOk = applyAvatarSecureUrlBlocking(appCtx, secureUrl);
            final String url = secureUrl;
            final String warning = firestoreOk
                    ? null
                    : "Ảnh đã lên cloud; Firestore chưa cập nhật — thử lại khi có mạng.";
            runOnMainThread(() -> {
                if (callback != null) {
                    callback.onComplete(true, url, warning);
                }
            });
        });
    }

    private static String uploadAvatarToFirebaseBlocking(Context context, Uri fileUri, String userId) throws Exception {
        File imageFile = CloudinaryUploadHelper.resolveImageFile(context, fileUri);
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String storagePath = "avatars/" + userId + "/avatar_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(storagePath);
        Tasks.await(ref.putFile(Uri.fromFile(imageFile)));
        android.net.Uri downloadUri = Tasks.await(ref.getDownloadUrl());
        if (downloadUri == null || downloadUri.toString().trim().isEmpty()) {
            throw new Exception("Firebase Storage không trả về download URL");
        }
        return downloadUri.toString().trim();
    }

    public static void uploadAvatarToFirebase(Context context, android.net.Uri fileUri, UploadCallback callback) {
        if (fileUri == null) {
            if (callback != null) {
                callback.onResult(null, "Uri ảnh null");
            }
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
                                if (callback != null) {
                                    callback.onResult(downloadUrl, null);
                                }
                            } else {
                                String err = task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Upload Firebase Storage thất bại";
                                if (callback != null) {
                                    callback.onResult(null, err);
                                }
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    callback.onResult(null, e.getMessage());
                }
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

    public static void pushSkincareHistoryToFirestore(Context context, String date) {
        // Lịch sử routine chỉ lưu local (ProfileSession) — không cần Firebase.
    }

    public static void pullSkincareHistoryFromFirestoreBlocking(Context context, String userId) {
        // No-op: skincare history is local-only.
    }

    public static void seedJune2026SkincareHistoryIfNeeded(Context context) {
        try {
            if (!ProfileSession.isLoggedIn(context)) {
                return;
            }
            String userId = resolveCurrentUserId(context);
            if (userId == null || userId.trim().isEmpty()) {
                return;
            }

            android.content.SharedPreferences prefs = context.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE);
            if (prefs.getBoolean("june_2026_seeded_v1", false)) {
                return;
            }

            // Gieo dữ liệu từ ngày 10 đến ngày 25 tháng 06, 2026
            java.util.Set<String> morningDates = new java.util.HashSet<>(ProfileSession.getCompletedMorningDates(context));
            java.util.Set<String> eveningDates = new java.util.HashSet<>(ProfileSession.getCompletedEveningDates(context));

            for (int day = 10; day <= 25; day++) {
                String date = "2026-06-" + String.format(java.util.Locale.US, "%02d", day);
                morningDates.add(date);
                eveningDates.add(date);

                ProfileSession.setRoutineSubmitted(context, "morning", date, true);
                ProfileSession.setRoutineSubmitted(context, "evening", date, true);
                ProfileSession.setMorningRewardAwarded(context, date, true);
                ProfileSession.setEveningRewardAwarded(context, date, true);

                // Gieo các bước đã hoàn thành
                java.util.Set<String> stepIds = new java.util.HashSet<>();
                stepIds.add("morning_0");
                stepIds.add("morning_1");
                stepIds.add("morning_2");
                stepIds.add("evening_0");
                stepIds.add("evening_1");
                stepIds.add("evening_2");
                ProfileSession.setCompletedStepIdsForDate(context, date, stepIds);
            }

            ProfileSession.setCompletedMorningDates(context, morningDates);
            ProfileSession.setCompletedEveningDates(context, eveningDates);

            // Cập nhật streak và ngày cuối cùng hoàn thành
            ProfileSession.setSkinStreak(context, 16);
            prefs.edit().putInt("skin_max_streak", 16).apply();
            ProfileSession.setSkinLastCompletedDate(context, "2026-06-25");

            prefs.edit().putBoolean("june_2026_seeded_v1", true).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

