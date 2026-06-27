package com.veganbeauty.app.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.remote.FirestoreService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncDataHelper {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void syncAllLocalDataToFirebase(Context context) {
        executor.execute(() -> {
            try {
                Log.d("SyncData", "Bắt đầu đồng bộ dữ liệu lên Firebase...");
                FirestoreService firestore = new FirestoreService();

                // 4. Sync Skin Bookings
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("skin_bookings.json")));
                    StringBuilder sb = new StringBuilder(); String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    firestore.forceSyncCollection("skin_bookings", sb.toString(), "id", "bookings");
                    Log.d("SyncData", "Đã đồng bộ Skin Bookings");
                } catch (Exception e) { Log.e("SyncData", "Lỗi đồng bộ Skin Bookings: " + e.getMessage()); }

                // 5. Sync Skin History
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("skin_history.json")));
                    StringBuilder sb = new StringBuilder(); String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    firestore.forceSyncCollection("skin_history", sb.toString(), "id");
                    Log.d("SyncData", "Đã đồng bộ Skin Histories");
                } catch (Exception e) { Log.e("SyncData", "Lỗi đồng bộ Skin Histories: " + e.getMessage()); }

                Log.d("SyncData", "ĐỒNG BỘ THÀNH CÔNG TẤT CẢ DỮ LIỆU!");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SyncData", "Lỗi đồng bộ: " + e.getMessage());
            }
        });
    }

    public static void syncUserProfileToFirebaseAndLocal(Context context) {
        String userId = ProfileSession.getUserId(context);
        String username = ProfileSession.getUsername(context);
        String fullName = ProfileSession.getFullName(context);
        String email = ProfileSession.getEmail(context);
        String phone = ProfileSession.getPhone(context);
        String avatar = ProfileSession.getAvatar(context);
        String address = ProfileSession.getAddress(context);
        String cccd = ProfileSession.getCCCD(context);
        String dob = ProfileSession.getDob(context);
        String gender = ProfileSession.getGender(context);

        executor.execute(() -> {
            try {
                RootieDatabase db = RootieDatabase.getDatabase(context);
                UserEntity existingUser = db.userDao().getUserByIdSync(userId);
                if (existingUser == null) existingUser = db.userDao().getUserByEmail(email);
                if (existingUser == null) existingUser = db.userDao().getUserByPhone(phone);
                String password = existingUser != null ? existingUser.getPassword() : "123456";

                UserEntity userEntity = new UserEntity(userId, username, fullName, email, phone, password, avatar,
                        existingUser != null ? existingUser.getPrimary_image() : null);
                db.userDao().insertUser(userEntity);

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("username", username); userMap.put("full_name", fullName);
                userMap.put("email", email); userMap.put("phone", phone);
                userMap.put("avatar", avatar); userMap.put("address", address);
                userMap.put("cccd", cccd); userMap.put("dob", dob); userMap.put("gender", gender);

                FirebaseFirestore.getInstance().collection("users").document(userId)
                        .set(userMap, SetOptions.merge())
                        .addOnSuccessListener(a -> Log.d("SyncData", "Synced profile to Firestore: " + userId))
                        .addOnFailureListener(e -> Log.e("SyncData", "Failed: " + e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SyncData", "Failed to sync user profile: " + e.getMessage());
            }
        });
    }

    public static void syncRewardPointsToFirestore(Context context) {
        String userId = ProfileSession.getUserId(context);
        executor.execute(() -> {
            try {
                RootieDatabase db = RootieDatabase.getDatabase(context);
                var history = db.rewardPointDao().getAllRewardHistoryList();
                FirebaseFirestore fsdb = FirebaseFirestore.getInstance();
                int total = 0;
                for (var item : history) total += item.getPoints();
                final int totalPoints = total;

                fsdb.collection("users").document(userId).update("coins", totalPoints);
                var batch = fsdb.batch();
                for (var item : history) {
                    var ref = fsdb.collection("users").document(userId)
                            .collection("reward_history").document(String.valueOf(item.getId()));
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", item.getId()); data.put("orderId", item.getOrderId());
                    data.put("points", item.getPoints()); data.put("reason", item.getReason());
                    data.put("timestamp", item.getTimestamp());
                    batch.set(ref, data, SetOptions.merge());
                }
                batch.commit().addOnSuccessListener(a -> Log.d("SyncData", "Synced reward points, total=" + totalPoints));
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SyncData", "Failed to sync reward points: " + e.getMessage());
            }
        });
    }

    public static void syncRewardPointsFromFirestore(Context context) {
        String userId = ProfileSession.getUserId(context);
        if (userId == null || userId.isBlank()) return;
        executor.execute(() -> {
            try {
                RootieDatabase db = RootieDatabase.getDatabase(context);
                FirebaseFirestore.getInstance().collection("users").document(userId)
                        .collection("reward_history").get()
                        .addOnSuccessListener(snapshot -> {
                            if (!snapshot.isEmpty()) {
                                executor.execute(() -> {
                                    for (var doc : snapshot.getDocuments()) {
                                        Long idL = doc.getLong("id"); if (idL == null) continue;
                                        RewardPointEntity entity = new RewardPointEntity(
                                                idL.intValue(),
                                                doc.getString("orderId") != null ? doc.getString("orderId") : "",
                                                doc.getLong("points") != null ? doc.getLong("points").intValue() : 0,
                                                doc.getString("reason") != null ? doc.getString("reason") : "",
                                                doc.getLong("timestamp") != null ? doc.getLong("timestamp") : System.currentTimeMillis()
                                        );
                                        try { db.rewardPointDao().insertRewardPoints(entity); } catch (Exception e) { e.printStackTrace(); }
                                    }
                                    Log.d("SyncData", "Pulled " + snapshot.size() + " reward items from Firestore");
                                });
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("SyncData", "Failed to sync reward points from Firestore: " + e.getMessage());
            }
        });
    }

    public static void uploadAvatarToFirebase(Context context, Uri fileUri, AvatarUploadCallback callback) {
        try {
            String userId = ProfileSession.getUserId(context);
            var avatarRef = FirebaseStorage.getInstance().getReference().child("avatars/" + userId + ".jpg");
            avatarRef.putFile(fileUri)
                    .addOnSuccessListener(t -> avatarRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String url = uri.toString();
                                ProfileSession.setAvatar(context, url);
                                syncUserProfileToFirebaseAndLocal(context);
                                callback.onComplete(url);
                            })
                            .addOnFailureListener(e -> { e.printStackTrace(); callback.onComplete(null); }))
                    .addOnFailureListener(e -> { e.printStackTrace(); callback.onComplete(null); });
        } catch (Exception e) {
            e.printStackTrace();
            callback.onComplete(null);
        }
    }

    public static void syncUserProfileFromFirestore(Context context, Runnable onComplete) {
        String userId = ProfileSession.getUserId(context);
        if (userId == null || userId.isBlank()) { if (onComplete != null) onComplete.run(); return; }
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        executor.execute(() -> {
                            try {
                                String username = doc.getString("username"); if (username == null) username = "";
                                String fullName = doc.getString("full_name"); if (fullName == null) fullName = "";
                                String email = doc.getString("email"); if (email == null) email = "";
                                String phone = doc.getString("phone"); if (phone == null) phone = "";
                                String avatar = doc.getString("avatar"); if (avatar == null) avatar = "";
                                String address = doc.getString("address"); if (address == null) address = "";
                                String cccd = doc.getString("cccd"); if (cccd == null) cccd = "";
                                String dob = doc.getString("dob"); if (dob == null) dob = "";
                                String gender = doc.getString("gender"); if (gender == null) gender = "";

                                ProfileSession.setUserId(context, userId);
                                if (!username.isBlank()) ProfileSession.setUsername(context, username);
                                if (!fullName.isBlank()) ProfileSession.setFullName(context, fullName);
                                if (!email.isBlank()) ProfileSession.setEmail(context, email);
                                if (!phone.isBlank()) ProfileSession.setPhone(context, phone);
                                if (!avatar.isBlank()) ProfileSession.setAvatar(context, avatar);
                                if (!address.isBlank()) ProfileSession.setAddress(context, address);
                                if (!cccd.isBlank()) ProfileSession.setCCCD(context, cccd);
                                if (!dob.isBlank()) ProfileSession.setDob(context, dob);
                                if (!gender.isBlank()) ProfileSession.setGender(context, gender);

                                RootieDatabase db = RootieDatabase.getDatabase(context);
                                UserEntity existing = db.userDao().getUserByIdSync(userId);
                                if (existing == null) existing = db.userDao().getUserByEmail(email);
                                if (existing == null) existing = db.userDao().getUserByPhone(phone);
                                String pass = existing != null ? existing.getPassword() : "123456";
                                String pImg = existing != null ? existing.getPrimary_image() : (!avatar.isBlank() ? avatar : "");
                                final String fu = username, ff = fullName, fe = email, fp = phone, fa = avatar;
                                final UserEntity ex = existing;
                                UserEntity userEntity = new UserEntity(userId,
                                        !fu.isBlank() ? fu : (ex != null ? ex.getUsername() : ""),
                                        !ff.isBlank() ? ff : (ex != null ? ex.getFull_name() : ""),
                                        !fe.isBlank() ? fe : (ex != null ? ex.getEmail() : ""),
                                        !fp.isBlank() ? fp : (ex != null ? ex.getPhone() : ""),
                                        pass,
                                        !fa.isBlank() ? fa : (ex != null ? ex.getAvatar() : ""),
                                        pImg);
                                db.userDao().insertUser(userEntity);
                                Log.d("SyncData", "Synced user profile FROM Firestore: " + userId);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (onComplete != null) onComplete.run();
                            }
                        });
                    } else {
                        if (onComplete != null) onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SyncData", "Failed to sync profile from Firestore: " + e.getMessage());
                    if (onComplete != null) onComplete.run();
                });
    }

    public interface AvatarUploadCallback {
        void onComplete(String downloadUrl);
    }
}
