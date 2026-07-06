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
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.remote.FirestoreService;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncDataHelper {

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
                    FirebaseFirestore.getInstance().collection("users").document(userId.trim()).get(),
                    8,
                    java.util.concurrent.TimeUnit.SECONDS
            );
            if (!doc.exists()) {
                return;
            }

            // Chưa đồng bộ cloud xong — giữ tên/avatar vừa sửa (vd. meomeomeo).
            if (ProfileSession.hasLocalProfileEdits(context)) {
                return;
            }

            applyFirestoreProfileDocument(context, doc);
            UserEntity user = ProfileSessionHelper.findCurrentUser(context);
            if (user == null) {
                return;
            }
            RootieDatabase.getDatabase(context).userDao().insertUserSync(user);
            pullSkincareHistoryFromFirestoreBlocking(context, userId.trim());
            seedJune2026SkincareHistoryIfNeeded(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Firestore là nguồn đúng khi pull — máy khác cùng tài khoản nhận tên/avatar mới. */
    private static void applyFirestoreProfileDocument(Context context, DocumentSnapshot doc) {
        String remoteAvatar = doc.getString("avatar");
        if (remoteAvatar == null) {
            remoteAvatar = doc.getString("avatar_url");
        }
        if (remoteAvatar == null) {
            remoteAvatar = "";
        }

        String resolvedAvatar;
        String sessionAvatar = ProfileSession.getAvatarStored(context);
        if (sessionAvatar != null
                && sessionAvatar.trim().startsWith("file://")
                && ProfileSessionHelper.isUsableAvatarUrl(sessionAvatar)) {
            // Ảnh vừa chọn/crop, chờ upload Cloudinary — không ghi đè bằng avatar cũ trên Firestore.
            resolvedAvatar = sessionAvatar.trim();
        } else if (isRemoteAvatarUrl(remoteAvatar)) {
            resolvedAvatar = remoteAvatar.trim();
        } else {
            String localAvatarFile = ProfileSessionHelper.getLocalAvatarFileUri(context);
            resolvedAvatar = localAvatarFile != null ? localAvatarFile : "";
        }

        String fullName = doc.getString("full_name") != null ? doc.getString("full_name").trim() : "";
        String username = doc.getString("username") != null ? doc.getString("username").trim() : "";
        String email = doc.getString("email") != null ? doc.getString("email").trim() : "";
        String phone = doc.getString("phone") != null ? doc.getString("phone").trim() : "";
        String bio = doc.getString("bio") != null ? doc.getString("bio").trim() : "";
        String primaryImage = doc.getString("primary_image");
        String dob = doc.getString("dob") != null ? doc.getString("dob").trim() : "";
        String gender = doc.getString("gender") != null ? doc.getString("gender").trim() : "";
        String cccd = doc.getString("cccd") != null ? doc.getString("cccd").trim() : "";
        String address = doc.getString("address") != null ? doc.getString("address").trim() : "";

        if (!fullName.isEmpty()) {
            ProfileSession.setFullName(context, fullName);
        }
        if (!username.isEmpty()) {
            String normalized = username.startsWith("@") ? username : "@" + username.replace(" ", "");
            ProfileSession.setUsername(context, normalized);
        }
        if (!email.isEmpty()) {
            ProfileSession.setEmail(context, email);
        }
        if (!phone.isEmpty()) {
            ProfileSession.setPhone(context, phone);
        }
        if (!bio.isEmpty()) {
            ProfileSession.setBio(context, bio);
        }
        if (!dob.isEmpty()) {
            ProfileSession.setDob(context, dob);
        }
        if (!gender.isEmpty()) {
            ProfileSession.setGender(context, gender);
        }
        if (!cccd.isEmpty()) {
            ProfileSession.setCCCD(context, cccd);
        }
        if (!address.isEmpty()) {
            ProfileSession.setAddress(context, address);
        }
        if (!resolvedAvatar.isEmpty()) {
            ProfileSession.setAvatar(context, resolvedAvatar);
        }

        UserEntity user = ProfileSessionHelper.findCurrentUser(context);
        if (user == null) {
            user = new UserEntity(
                    doc.getId(),
                    username,
                    fullName,
                    email,
                    phone,
                    "",
                    resolvedAvatar,
                    primaryImage
            );
            if (!bio.isEmpty()) {
                user.setBio(bio);
            }
            RootieDatabase.getDatabase(context).userDao().insertUserSync(user);
            return;
        }

        user.setUser_id(doc.getId());
        if (!fullName.isEmpty()) {
            user.setFull_name(fullName);
        }
        if (!username.isEmpty()) {
            user.setUsername(username);
        }
        if (!email.isEmpty()) {
            user.setEmail(email);
        }
        if (!phone.isEmpty()) {
            user.setPhone(phone);
        }
        if (!resolvedAvatar.isEmpty()) {
            user.setAvatar(resolvedAvatar);
        }
        if (primaryImage != null && !primaryImage.trim().isEmpty()) {
            user.setPrimary_image(primaryImage.trim());
        }
        if (!bio.isEmpty()) {
            user.setBio(bio);
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
        ProfileSessionHelper.syncSessionFromUser(context, userEntity, true);
        propagateProfileChangesBlocking(context);
        return true;
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
        try {
            UserEntity userEntity = ProfileSessionHelper.findCurrentUser(context);
            if (userEntity == null) {
                return false;
            }
            String avatarForCloud = resolveRemoteAvatarForSync(context);
            if (!avatarForCloud.isEmpty()) {
                userEntity.setAvatar(avatarForCloud);
            }
            String dob = ProfileSession.getDob(context) != null ? ProfileSession.getDob(context) : "";
            String gender = ProfileSession.getGender(context) != null ? ProfileSession.getGender(context) : "";
            String cccd = ProfileSession.getCCCD(context) != null ? ProfileSession.getCCCD(context) : "";
            String address = ProfileSession.getAddress(context) != null ? ProfileSession.getAddress(context) : "";
            boolean saved = new FirestoreService().saveUser(userEntity, dob, gender, cccd, address);
            if (saved) {
                ProfileSession.clearLocalProfileEdits(context);
            }
            return saved;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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
            boolean firestoreOk = new FirestoreService().updateUserAvatar(userId, remoteUrl);
            if (firestoreOk) {
                propagateAvatarToCommunityBlocking(appCtx, remoteUrl);
                ProfileUpdateNotifier.notifyUpdated();
            }
            return firestoreOk;
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

    /** Upload Cloudinary rồi đồng bộ session/Room/Firestore — luồng gộp, không xếp hàng sau sync nặng. */
    public static void uploadAndSyncAvatar(Context context, Uri fileUri, AvatarSyncCallback callback) {
        Context appCtx = context.getApplicationContext();
        AVATAR_EXECUTOR.execute(() -> {
            String secureUrl = null;
            String uploadError = null;
            try {
                if (fileUri == null) {
                    uploadError = "Uri ảnh null";
                } else if (!CloudinaryConfig.isConfigured()) {
                    uploadError = "Chưa cấu hình Cloudinary";
                } else {
                    String userId = resolveCurrentUserId(appCtx);
                    if (userId == null || userId.trim().isEmpty()) {
                        uploadError = "Thiếu user_id — hãy đăng nhập lại";
                    } else {
                        File imageFile = CloudinaryUploadHelper.resolveImageFile(appCtx, fileUri);
                        secureUrl = CloudinaryUploadHelper.uploadAvatarFile(imageFile, userId.trim());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                uploadError = e.getMessage() != null ? e.getMessage() : "Lỗi upload Cloudinary";
            }

            if (secureUrl == null) {
                final String err = uploadError != null && !uploadError.isEmpty()
                        ? uploadError
                        : "Upload Cloudinary thất bại. Kiểm tra mạng và upload preset.";
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
                    : "Ảnh đã lên Cloudinary; Firebase chưa cập nhật — thử lại khi có mạng.";
            runOnMainThread(() -> {
                if (callback != null) {
                    callback.onComplete(true, url, warning);
                }
            });
        });
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

                String ordersJson = reader.readAsset("orders.json");
                if (ordersJson != null) {
                    firestoreService.syncOrders(reader.getAllOrders());
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
        EXECUTOR.execute(() -> {
            try {
                if (!ProfileSession.isLoggedIn(context)) {
                    return;
                }
                String userId = resolveCurrentUserId(context);
                if (userId == null || userId.trim().isEmpty()) {
                    return;
                }

                FirebaseFirestore db = FirebaseFirestore.getInstance();

                // 1. Lấy chi tiết lịch sử ngày cụ thể
                java.util.Set<String> completedSteps = ProfileSession.getCompletedStepIdsForDate(context, date);
                java.util.List<String> morningCompletedSteps = new java.util.ArrayList<>();
                java.util.List<String> eveningCompletedSteps = new java.util.ArrayList<>();
                for (String stepId : completedSteps) {
                    if (stepId.startsWith("morning_")) {
                        morningCompletedSteps.add(stepId);
                    } else if (stepId.startsWith("evening_")) {
                        eveningCompletedSteps.add(stepId);
                    }
                }

                boolean morningSubmitted = ProfileSession.isRoutineSubmitted(context, "morning", date);
                boolean eveningSubmitted = ProfileSession.isRoutineSubmitted(context, "evening", date);
                boolean morningRewardAwarded = ProfileSession.isMorningRewardAwarded(context, date);
                boolean eveningRewardAwarded = ProfileSession.isEveningRewardAwarded(context, date);

                java.util.Map<String, Object> historyData = new java.util.HashMap<>();
                historyData.put("morning_completed_steps", morningCompletedSteps);
                historyData.put("evening_completed_steps", eveningCompletedSteps);
                historyData.put("morning_submitted", morningSubmitted);
                historyData.put("evening_submitted", eveningSubmitted);
                historyData.put("morning_reward_awarded", morningRewardAwarded);
                historyData.put("evening_reward_awarded", eveningRewardAwarded);

                Tasks.await(
                        db.collection("users")
                                .document(userId.trim())
                                .collection("skincare_history")
                                .document(date)
                                .set(historyData)
                );

                // 2. Cập nhật thông tin Streak toàn cục lên document chính của user
                int skinStreak = ProfileSession.getSkinStreak(context);
                int skinMaxStreak = context.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE)
                        .getInt("skin_max_streak", 0);
                String skinLastCompletedDate = ProfileSession.getSkinLastCompletedDate(context);
                java.util.Set<String> completedMorningDates = ProfileSession.getCompletedMorningDates(context);
                java.util.Set<String> completedEveningDates = ProfileSession.getCompletedEveningDates(context);

                java.util.Map<String, Object> statsData = new java.util.HashMap<>();
                statsData.put("skin_streak", skinStreak);
                statsData.put("skin_max_streak", skinMaxStreak);
                statsData.put("skin_last_completed_date", skinLastCompletedDate);
                statsData.put("completed_morning_dates", new java.util.ArrayList<>(completedMorningDates));
                statsData.put("completed_evening_dates", new java.util.ArrayList<>(completedEveningDates));

                Tasks.await(
                        db.collection("users")
                                .document(userId.trim())
                                .set(statsData, com.google.firebase.firestore.SetOptions.merge())
                );

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void pullSkincareHistoryFromFirestoreBlocking(Context context, String userId) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentSnapshot userDoc = Tasks.await(db.collection("users").document(userId).get());
            if (userDoc.exists()) {
                // 1. Khôi phục thông tin streak
                if (userDoc.contains("skin_streak")) {
                    Long val = userDoc.getLong("skin_streak");
                    if (val != null) {
                        int localStreak = ProfileSession.getSkinStreak(context);
                        if (val.intValue() > localStreak) {
                            ProfileSession.setSkinStreak(context, val.intValue());
                        }
                    }
                }
                if (userDoc.contains("skin_max_streak")) {
                    Long val = userDoc.getLong("skin_max_streak");
                    if (val != null) {
                        android.content.SharedPreferences prefs = context.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE);
                        int localMax = prefs.getInt("skin_max_streak", 0);
                        if (val.intValue() > localMax) {
                            prefs.edit().putInt("skin_max_streak", val.intValue()).apply();
                        }
                    }
                }
                if (userDoc.contains("skin_last_completed_date")) {
                    String lastDate = userDoc.getString("skin_last_completed_date");
                    if (lastDate != null && !lastDate.isEmpty()) {
                        ProfileSession.setSkinLastCompletedDate(context, lastDate);
                    }
                }

                // 2. Khôi phục các ngày hoàn thành bằng cách MERGE với dữ liệu local
                if (userDoc.contains("completed_morning_dates")) {
                    java.util.List<String> list = (java.util.List<String>) userDoc.get("completed_morning_dates");
                    if (list != null) {
                        java.util.Set<String> merged = new java.util.HashSet<>(ProfileSession.getCompletedMorningDates(context));
                        merged.addAll(list);
                        ProfileSession.setCompletedMorningDates(context, merged);
                    }
                }
                if (userDoc.contains("completed_evening_dates")) {
                    java.util.List<String> list = (java.util.List<String>) userDoc.get("completed_evening_dates");
                    if (list != null) {
                        java.util.Set<String> merged = new java.util.HashSet<>(ProfileSession.getCompletedEveningDates(context));
                        merged.addAll(list);
                        ProfileSession.setCompletedEveningDates(context, merged);
                    }
                }
            }

            // 3. Khôi phục chi tiết lịch sử từng ngày từ subcollection bằng cách MERGE
            com.google.firebase.firestore.QuerySnapshot historySnapshot = Tasks.await(
                    db.collection("users").document(userId).collection("skincare_history").get()
            );

            for (DocumentSnapshot doc : historySnapshot.getDocuments()) {
                String date = doc.getId();

                boolean localMorningSubmitted = ProfileSession.isRoutineSubmitted(context, "morning", date);
                boolean localEveningSubmitted = ProfileSession.isRoutineSubmitted(context, "evening", date);
                boolean localMorningRewardAwarded = ProfileSession.isMorningRewardAwarded(context, date);
                boolean localEveningRewardAwarded = ProfileSession.isEveningRewardAwarded(context, date);

                boolean morningSubmitted = (doc.getBoolean("morning_submitted") != null && doc.getBoolean("morning_submitted")) || localMorningSubmitted;
                boolean eveningSubmitted = (doc.getBoolean("evening_submitted") != null && doc.getBoolean("evening_submitted")) || localEveningSubmitted;
                boolean morningRewardAwarded = (doc.getBoolean("morning_reward_awarded") != null && doc.getBoolean("morning_reward_awarded")) || localMorningRewardAwarded;
                boolean eveningRewardAwarded = (doc.getBoolean("evening_reward_awarded") != null && doc.getBoolean("evening_reward_awarded")) || localEveningRewardAwarded;

                ProfileSession.setRoutineSubmitted(context, "morning", date, morningSubmitted);
                ProfileSession.setRoutineSubmitted(context, "evening", date, eveningSubmitted);
                ProfileSession.setMorningRewardAwarded(context, date, morningRewardAwarded);
                ProfileSession.setEveningRewardAwarded(context, date, eveningRewardAwarded);

                // Khôi phục các bước đã tích chọn bằng cách MERGE
                java.util.Set<String> stepIds = new java.util.HashSet<>(ProfileSession.getCompletedStepIdsForDate(context, date));
                java.util.List<String> morningSteps = (java.util.List<String>) doc.get("morning_completed_steps");
                if (morningSteps != null) {
                    stepIds.addAll(morningSteps);
                }
                java.util.List<String> eveningSteps = (java.util.List<String>) doc.get("evening_completed_steps");
                if (eveningSteps != null) {
                    stepIds.addAll(eveningSteps);
                }
                ProfileSession.setCompletedStepIdsForDate(context, date, stepIds);
            }

            // 4. Đồng bộ ngược lại các thay đổi đã merge từ local lên Firestore để đảm bảo Firestore có đầy đủ dữ liệu nhất
            pushAllLocalSkincareHistoryToFirestoreBlocking(context, userId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void pushAllLocalSkincareHistoryToFirestoreBlocking(Context context, String userId) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            java.util.Set<String> morningDates = ProfileSession.getCompletedMorningDates(context);
            java.util.Set<String> eveningDates = ProfileSession.getCompletedEveningDates(context);

            java.util.Set<String> allDates = new java.util.HashSet<>(morningDates);
            allDates.addAll(eveningDates);

            for (String date : allDates) {
                java.util.Set<String> completedSteps = ProfileSession.getCompletedStepIdsForDate(context, date);
                java.util.List<String> morningCompletedSteps = new java.util.ArrayList<>();
                java.util.List<String> eveningCompletedSteps = new java.util.ArrayList<>();
                for (String stepId : completedSteps) {
                    if (stepId.startsWith("morning_")) {
                        morningCompletedSteps.add(stepId);
                    } else if (stepId.startsWith("evening_")) {
                        eveningCompletedSteps.add(stepId);
                    }
                }

                boolean morningSubmitted = ProfileSession.isRoutineSubmitted(context, "morning", date);
                boolean eveningSubmitted = ProfileSession.isRoutineSubmitted(context, "evening", date);
                boolean morningRewardAwarded = ProfileSession.isMorningRewardAwarded(context, date);
                boolean eveningRewardAwarded = ProfileSession.isEveningRewardAwarded(context, date);

                java.util.Map<String, Object> historyData = new java.util.HashMap<>();
                historyData.put("morning_completed_steps", morningCompletedSteps);
                historyData.put("evening_completed_steps", eveningCompletedSteps);
                historyData.put("morning_submitted", morningSubmitted);
                historyData.put("evening_submitted", eveningSubmitted);
                historyData.put("morning_reward_awarded", morningRewardAwarded);
                historyData.put("evening_reward_awarded", eveningRewardAwarded);

                Tasks.await(
                        db.collection("users")
                                .document(userId.trim())
                                .collection("skincare_history")
                                .document(date)
                                .set(historyData)
                );
            }

            int skinStreak = ProfileSession.getSkinStreak(context);
            int skinMaxStreak = context.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE)
                    .getInt("skin_max_streak", 0);
            String skinLastCompletedDate = ProfileSession.getSkinLastCompletedDate(context);

            java.util.Map<String, Object> statsData = new java.util.HashMap<>();
            statsData.put("skin_streak", skinStreak);
            statsData.put("skin_max_streak", skinMaxStreak);
            statsData.put("skin_last_completed_date", skinLastCompletedDate);
            statsData.put("completed_morning_dates", new java.util.ArrayList<>(morningDates));
            statsData.put("completed_evening_dates", new java.util.ArrayList<>(eveningDates));

            Tasks.await(
                    db.collection("users")
                            .document(userId.trim())
                            .set(statsData, com.google.firebase.firestore.SetOptions.merge())
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
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

            // Đẩy toàn bộ dữ liệu mới gieo lên Firestore
            pushAllLocalSkincareHistoryToFirestoreBlocking(context, userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

