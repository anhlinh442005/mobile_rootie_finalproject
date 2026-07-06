package com.veganbeauty.app;

import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.os.Handler;
import android.content.DialogInterface;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.features.home.HomeFragment;
import com.veganbeauty.app.utils.RootieBrandHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Schedule daily weather & skin advice notification at 6:30 AM
        try {
            com.veganbeauty.app.features.weather.DailySkinWeatherScheduler.scheduleDailyNotification(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Schedule skincare routine notifications
        try {
            com.veganbeauty.app.features.routine.RoutineAlarmScheduler.rescheduleAlarms(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (savedInstanceState == null) {
            String navigateTo = getIntent().getStringExtra("NAVIGATE_TO");
            androidx.fragment.app.Fragment destination;
            if ("SKIN_REMINDER".equals(navigateTo)) {
                destination = new com.veganbeauty.app.features.routine.SkinRoutineSettingsFragment();
            } else if ("WEATHER_FORECAST".equals(navigateTo) || "SKIN_WEATHER_FORECAST".equals(navigateTo)) {
                destination = new com.veganbeauty.app.features.weather.SkinWeatherForecastFragment();
            } else {
                destination = new com.veganbeauty.app.features.home.HomeFragment();
            }
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, destination)
                .commit();
        }

        if (!com.veganbeauty.app.data.local.ProfileSession.isLoggedIn(this)) {
            com.veganbeauty.app.features.shop.product.CartHelper.clearCart(this);
        } else {
            com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsFromFirestore(this);
            com.veganbeauty.app.utils.SyncDataHelper.syncUserProfileFromFirestore(this, null);
        }

        // One-time wipe + re-upload canonical asset data to Firebase
        android.content.SharedPreferences syncPrefs = getSharedPreferences("rootie_prefs", MODE_PRIVATE);
        if (!syncPrefs.getBoolean("firebase_assets_sync_v3", false)) {
            com.veganbeauty.app.utils.SyncDataHelper.syncAllLocalDataToFirebase(this, success -> {
                if (success) {
                    syncPrefs.edit().putBoolean("firebase_assets_sync_v3", true).apply();
                }
            });
        }

        com.veganbeauty.app.features.myskin.BookingSampleSeeder.seedIfNeeded(this);
        com.veganbeauty.app.features.community.UserSocialSeeder.seedIfNeeded(this);
        com.veganbeauty.app.features.community.CommunityMessageSeeder.seedIfNeeded(this);

        // Preload product catalog + images from assets in background
        new Thread(() -> {
            try {
                com.veganbeauty.app.utils.ProductImageCache.preload(getApplicationContext());
                com.veganbeauty.app.data.local.RootieDatabase db =
                        com.veganbeauty.app.data.local.RootieDatabase.getDatabase(getApplicationContext());
                com.veganbeauty.app.data.repository.ProductRepository productRepository =
                        new com.veganbeauty.app.data.repository.ProductRepository(
                                db.productDao(),
                                new com.veganbeauty.app.data.local.LocalJsonReader(getApplicationContext()),
                                new com.veganbeauty.app.data.remote.FirestoreService(),
                                db.userProductExpiryDao()
                        );
                productRepository.seedProductsFromAssets();
                productRepository.refreshProducts(true);

                com.veganbeauty.app.data.repository.OrderRepository orderRepository =
                        new com.veganbeauty.app.data.repository.OrderRepository(
                                db.orderDao(),
                                db.rewardPointDao(),
                                db.userGiftDao(),
                                new com.veganbeauty.app.data.local.LocalJsonReader(getApplicationContext())
                        );
                orderRepository.seedOrdersFromAssetsIfNeeded();

                com.veganbeauty.app.data.repository.CommunityRepository communityRepository =
                        new com.veganbeauty.app.data.repository.CommunityRepository(
                                db.communityDao(),
                                new com.veganbeauty.app.data.local.LocalJsonReader(getApplicationContext()),
                                new com.veganbeauty.app.data.remote.FirestoreService()
                        );
                communityRepository.seedFromAssetsIfNeeded();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Sync team users from users.json into SQLite + Firebase on background thread
        new Thread(() -> {
            try {
                com.veganbeauty.app.data.local.RootieDatabase db =
                    com.veganbeauty.app.data.local.RootieDatabase.getDatabase(getApplicationContext());
                com.veganbeauty.app.data.local.dao.UserDao userDao = db.userDao();
                com.veganbeauty.app.data.remote.FirestoreService firestoreService =
                    new com.veganbeauty.app.data.remote.FirestoreService();

                String jsonString = new java.io.BufferedReader(
                    new java.io.InputStreamReader(getAssets().open("users.json")))
                    .lines().collect(java.util.stream.Collectors.joining("\n"));

                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonString);

                // Clean up old "Test Account" from SQLite
                userDao.deleteUserByUsernameSync("Test Account");

                // Clean up "Test Account" from Firebase
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("username", "Test Account")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            doc.getReference().delete();
                        }
                    });

                // Only sync team member user IDs
                java.util.Set<String> teamIds = new java.util.HashSet<>(java.util.Arrays.asList(
                    "test_001", "39751498", "87962440", "68751659", "85097162", "48228004"
                ));

                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject obj = jsonArray.getJSONObject(i);
                    String userId = obj.optString("user_id", "");
                    if (!teamIds.contains(userId)) continue;

                    // Skip the user associated with this device to avoid overriding their profile changes in Firestore and SQLite
                    String savedUserId = com.veganbeauty.app.data.local.ProfileSession.getUserId(getApplicationContext());
                    com.veganbeauty.app.data.local.entities.UserEntity existingUser = userDao.getUserByIdSync(userId);

                    String username = obj.optString("username", "");
                    String fullName = obj.optString("full_name", "");
                    String email = obj.optString("email", "");
                    String phone = obj.optString("phone", "");
                    String password = obj.optString("password", "");
                    String avatar = obj.optString("avatar", null);
                    String primaryImage = obj.optString("primary_image", null);

                    if (RootieBrandHelper.isRootieUser(userId)) {
                        avatar = RootieBrandHelper.AVATAR_URL;
                    } else if (existingUser != null) {
                        if (existingUser.getUsername() != null && !existingUser.getUsername().isEmpty()) {
                            username = existingUser.getUsername();
                        }
                        if (existingUser.getFull_name() != null && !existingUser.getFull_name().isEmpty()) {
                            fullName = existingUser.getFull_name();
                        }
                        if (existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()) {
                            email = existingUser.getEmail();
                        }
                        if (existingUser.getPhone() != null && !existingUser.getPhone().isEmpty()) {
                            phone = existingUser.getPhone();
                        }
                        if (existingUser.getPassword() != null && !existingUser.getPassword().isEmpty()) {
                            password = existingUser.getPassword();
                        }
                        if (savedUserId != null && savedUserId.equals(userId)) {
                            String jsonAvatar = obj.optString("avatar", null);
                            if (jsonAvatar != null && !jsonAvatar.trim().isEmpty()) {
                                avatar = jsonAvatar.trim();
                            }
                            String jsonPrimary = obj.optString("primary_image", null);
                            if (jsonPrimary != null && !jsonPrimary.trim().isEmpty()) {
                                primaryImage = jsonPrimary.trim();
                            }
                        } else {
                            if (existingUser.getAvatar() != null && !existingUser.getAvatar().isEmpty()) {
                                avatar = existingUser.getAvatar();
                            }
                            if (existingUser.getPrimary_image() != null && !existingUser.getPrimary_image().isEmpty()) {
                                primaryImage = existingUser.getPrimary_image();
                            }
                        }
                    }

                    if (savedUserId != null && savedUserId.equals(userId)) {
                        com.veganbeauty.app.utils.ProfileSessionHelper.syncSessionFromUser(
                                getApplicationContext(),
                                new com.veganbeauty.app.data.local.entities.UserEntity(
                                        userId, username, fullName, email, phone, password, avatar, primaryImage
                                )
                        );
                    }

                    com.veganbeauty.app.data.local.entities.UserEntity user =
                        new com.veganbeauty.app.data.local.entities.UserEntity(
                            userId,
                            username,
                            fullName,
                            email,
                            phone,
                            password,
                            avatar,
                            primaryImage
                        );

                    // Update avatar in SQLite (upsert) using the sync method
                    userDao.insertUserSync(user);

                    // Sync user profile to Firestore ONLY if the document doesn't exist to prevent overwriting custom profiles
                    java.util.Map<String, Object> userMap = new java.util.HashMap<>();
                    userMap.put("username", user.getUsername());
                    userMap.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
                    userMap.put("email", user.getEmail());
                    userMap.put("phone", user.getPhone());
                    userMap.put("full_name", user.getFull_name());

                    final String targetUserId = userId;
                    final java.util.Map<String, Object> finalUserMap = userMap;
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(targetUserId)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && task.getResult() != null && !task.getResult().exists()) {
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(targetUserId)
                                    .set(finalUserMap);
                            }
                        });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        setupFloatingChatHead();

        // Handle push notification intents
        com.veganbeauty.app.features.account.notification.NotificationIntentHandler.handleIntent(this, getIntent());

        // Request POST_NOTIFICATIONS permission for Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Fetch and log FCM token
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    android.util.Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.getException());
                    return;
                }
                String token = task.getResult();
                android.util.Log.d("FCM_TOKEN", "=== ROOTIE_FCM_TOKEN: " + token + " ===");
                getSharedPreferences("RootiePrefs", MODE_PRIVATE)
                    .edit()
                    .putString("FCM_REGISTRATION_TOKEN", token)
                    .apply();

                // Upload FCM Token to Firestore under users/{userId}
                try {
                    String currentUserId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(MainActivity.this);
                    com.veganbeauty.app.data.remote.FirestoreService fs = new com.veganbeauty.app.data.remote.FirestoreService();
                    
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUserId)
                        .update("fcm_token", token)
                        .addOnCompleteListener(t -> {
                            fs.startListeningToNotifications(MainActivity.this, currentUserId);
                            fs.startListeningToUserEvents(MainActivity.this, currentUserId);
                        })
                        .addOnFailureListener(e -> {
                            java.util.Map<String, Object> data = new java.util.HashMap<>();
                            data.put("fcm_token", token);
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUserId)
                                .set(data, com.google.firebase.firestore.SetOptions.merge())
                                .addOnCompleteListener(t -> {
                                    fs.startListeningToNotifications(MainActivity.this, currentUserId);
                                    fs.startListeningToUserEvents(MainActivity.this, currentUserId);
                                });
                        });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        // Initialize unread status logic for chat bubbles
        setupUnreadBadgesLogic();
    }

    private View bubbleAi;
    private View bubbleHuman;
    private View chatHeadArrow;
    private boolean isExtraBubblesExpanded = false;
    private boolean isDockedToLeft = false;

    private android.widget.TextView tvMascotBadge;
    private View viewHumanBadge;
    private View viewAiBadge;

    private float initialTouchX;
    private float initialTouchY;
    private float initialViewX;
    private float initialViewY;
    private boolean isDragging = false;
    private boolean isLongClickTriggered = false;
    private android.os.Handler longClickHandler = new android.os.Handler();
    private Runnable longClickRunnable;

    private void setupFloatingChatHead() {
        final View chatHead = findViewById(R.id.skin_ai_floating_chat_head);
        if (chatHead == null) return;

        bubbleAi = findViewById(R.id.skin_floating_bubble_ai);
        bubbleHuman = findViewById(R.id.skin_floating_bubble_human);
        chatHeadArrow = findViewById(R.id.skin_ai_floating_chat_head_arrow);

        if (chatHeadArrow != null) {
            chatHeadArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    undockChatHead();
                }
            });
        }

        if (bubbleAi != null) {
            bubbleAi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSkinAiChatDialog();
                }
            });
        }

        if (bubbleHuman != null) {
            bubbleHuman.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSkinChatDialog();
                }
            });
        }

        android.content.SharedPreferences prefs = getSharedPreferences("RootieQuizPrefs", MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("SKIN_AI_FLOATING_CHAT_ENABLED", true);
        chatHead.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (chatHeadArrow != null) {
            chatHeadArrow.setVisibility(View.GONE);
        }
        if (!enabled) {
            collapseExtraBubblesImmediately();
        }

        longClickRunnable = new Runnable() {
            @Override
            public void run() {
                isLongClickTriggered = true;
                chatHead.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                chatHead.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).start();
                showHideConfirmDialog(chatHead);
            }
        };

        chatHead.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        initialViewX = chatHead.getX();
                        initialViewY = chatHead.getY();
                        isDragging = false;
                        isLongClickTriggered = false;
                        longClickHandler.postDelayed(longClickRunnable, 500);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialTouchX;
                        float deltaY = event.getRawY() - initialTouchY;

                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            longClickHandler.removeCallbacks(longClickRunnable);
                            if (isLongClickTriggered) {
                                chatHead.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                                isLongClickTriggered = false;
                            }
                            isDragging = true;
                            collapseExtraBubblesImmediately();
                        }

                        if (isDragging) {
                            float newX = initialViewX + deltaX;
                            float newY = initialViewY + deltaY;

                            int screenWidth = getResources().getDisplayMetrics().widthPixels;
                            int screenHeight = getResources().getDisplayMetrics().heightPixels;
                            
                            if (newX < 0) newX = 0;
                            if (newX > screenWidth - chatHead.getWidth()) {
                                newX = screenWidth - chatHead.getWidth();
                            }
                            
                            int statusBarHeight = (int) (24 * getResources().getDisplayMetrics().density);
                            int navigationBarHeight = (int) (48 * getResources().getDisplayMetrics().density);
                            if (newY < statusBarHeight) newY = statusBarHeight;
                            if (newY > screenHeight - chatHead.getHeight() - navigationBarHeight) {
                                newY = screenHeight - chatHead.getHeight() - navigationBarHeight;
                            }

                            chatHead.setX(newX);
                            chatHead.setY(newY);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        longClickHandler.removeCallbacks(longClickRunnable);
                        chatHead.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();

                        if (isLongClickTriggered) {
                            return true;
                        }

                        if (!isDragging) {
                            toggleExtraBubbles();
                        } else {
                            int screenWidth = getResources().getDisplayMetrics().widthPixels;
                            float currentX = chatHead.getX();
                            float centerX = currentX + chatHead.getWidth() / 2f;
                            float density = getResources().getDisplayMetrics().density;
                            float threshold = 12 * density;

                            if (currentX < threshold) {
                                dockChatHead(true);
                            } else if (currentX > screenWidth - chatHead.getWidth() - threshold) {
                                dockChatHead(false);
                            } else {
                                float targetX;
                                if (centerX < screenWidth / 2f) {
                                    targetX = 16 * density;
                                } else {
                                    targetX = screenWidth - chatHead.getWidth() - (16 * density);
                                }
                                
                                chatHead.animate()
                                        .x(targetX)
                                        .setDuration(250)
                                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                        .start();
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void openChatFragment() {
        androidx.fragment.app.Fragment current = getSupportFragmentManager().findFragmentById(R.id.main_container);
        if (current instanceof com.veganbeauty.app.features.ai.SkinAiChatFragment) {
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, new com.veganbeauty.app.features.ai.SkinAiChatFragment())
                .addToBackStack(null)
                .commit();
    }

    private void showHideConfirmDialog(final View chatHead) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_hide_chat, null);
        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialogView.findViewById(R.id.btn_dialog_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                chatHead.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }
        });

        dialogView.findViewById(R.id.btn_dialog_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                android.content.SharedPreferences prefs = getSharedPreferences("RootieQuizPrefs", MODE_PRIVATE);
                prefs.edit().putBoolean("SKIN_AI_FLOATING_CHAT_ENABLED", false).apply();
                chatHead.setVisibility(View.GONE);
                collapseExtraBubblesImmediately();
                if (chatHeadArrow != null) {
                    chatHeadArrow.setVisibility(View.GONE);
                }
                Toast.makeText(MainActivity.this, "Đã ẩn Trợ lý Rootie AI", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                chatHead.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
            }
        });

        dialog.show();
    }

    private void collapseExtraBubblesImmediately() {
        if (bubbleAi == null || bubbleHuman == null) return;
        if (isExtraBubblesExpanded) {
            final View chatHead = findViewById(R.id.skin_ai_floating_chat_head);
            if (chatHead != null) {
                float density = getResources().getDisplayMetrics().density;
                float offsetNeed = 5 * density;
                float collapsedX = chatHead.getX() + offsetNeed;
                float collapsedY = chatHead.getY() + offsetNeed;
                bubbleAi.setX(collapsedX);
                bubbleAi.setY(collapsedY);
                bubbleHuman.setX(collapsedX);
                bubbleHuman.setY(collapsedY);
            }
            bubbleAi.setVisibility(View.GONE);
            bubbleHuman.setVisibility(View.GONE);
            isExtraBubblesExpanded = false;
            updateUnreadBadges();
        }
    }

    private void toggleExtraBubbles() {
        final View chatHead = findViewById(R.id.skin_ai_floating_chat_head);
        if (bubbleAi == null || bubbleHuman == null || chatHead == null) return;

        float density = getResources().getDisplayMetrics().density;
        float offsetNeed = 5 * density;
        float mainX = chatHead.getX();
        float mainY = chatHead.getY();

        float collapsedX = mainX + offsetNeed;
        float collapsedY = mainY + offsetNeed;

        if (isExtraBubblesExpanded) {
            bubbleAi.animate()
                .x(collapsedX)
                .y(collapsedY)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    bubbleAi.setVisibility(View.GONE);
                    updateUnreadBadges();
                })
                .start();
            bubbleHuman.animate()
                .x(collapsedX)
                .y(collapsedY)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    bubbleHuman.setVisibility(View.GONE);
                    updateUnreadBadges();
                })
                .start();
            isExtraBubblesExpanded = false;
            updateUnreadBadges();
        } else {
            bubbleAi.setX(collapsedX);
            bubbleAi.setY(collapsedY);
            bubbleHuman.setX(collapsedX);
            bubbleHuman.setY(collapsedY);

            bubbleAi.setVisibility(View.VISIBLE);
            bubbleHuman.setVisibility(View.VISIBLE);

            bubbleAi.setAlpha(0f);
            bubbleHuman.setAlpha(0f);

            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            boolean expandDown = mainY < screenHeight / 2f;

            float targetY_Ai;
            float targetY_Human;

            if (expandDown) {
                targetY_Ai = mainY + 70 * density;
                targetY_Human = mainY + 130 * density;
            } else {
                targetY_Ai = mainY - 60 * density;
                targetY_Human = mainY - 120 * density;
            }

            bubbleAi.animate()
                .x(collapsedX)
                .y(targetY_Ai)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();

            bubbleHuman.animate()
                .x(collapsedX)
                .y(targetY_Human)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();

            isExtraBubblesExpanded = true;
            updateUnreadBadges();
        }
    }

    private void openSkinAiChatDialog() {
        collapseExtraBubblesImmediately();
        getSharedPreferences("RootieQuizPrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("SKIN_AI_CHAT_UNREAD", false)
            .apply();
        updateUnreadBadges();

        com.veganbeauty.app.features.ai.SkinChatDialogContainer dialog =
            com.veganbeauty.app.features.ai.SkinChatDialogContainer.newInstance(true);
        dialog.show(getSupportFragmentManager(), "SkinChatDialogContainer");
    }

    private void openSkinChatDialog() {
        collapseExtraBubblesImmediately();
        String currentUserId = com.veganbeauty.app.data.local.ProfileSession.INSTANCE.getCurrentUserId(this);
        String skinChatConvId = "chat_rootie_vn_" + currentUserId;
        com.veganbeauty.app.features.community.message.MessageHelper.markAsRead(this, skinChatConvId, currentUserId);
        updateUnreadBadges();

        com.veganbeauty.app.features.ai.SkinChatDialogContainer dialog =
            com.veganbeauty.app.features.ai.SkinChatDialogContainer.newInstance(false);
        dialog.show(getSupportFragmentManager(), "SkinChatDialogContainer");
    }

    private void setupUnreadBadgesLogic() {
        tvMascotBadge = findViewById(R.id.tv_mascot_badge);
        viewHumanBadge = findViewById(R.id.view_human_badge);
        viewAiBadge = findViewById(R.id.view_ai_badge);

        // Initialize AI chat unread state to true on first launch if not set
        android.content.SharedPreferences quizPrefs = getSharedPreferences("RootieQuizPrefs", MODE_PRIVATE);
        if (!quizPrefs.contains("SKIN_AI_CHAT_UNREAD")) {
            quizPrefs.edit().putBoolean("SKIN_AI_CHAT_UNREAD", true).apply();
        }

        // Seed unread message from rootie_vn if conversation is empty
        final String currentUserId = com.veganbeauty.app.data.local.ProfileSession.INSTANCE.getCurrentUserId(this);
        final String skinChatConvId = "chat_rootie_vn_" + currentUserId;
        com.veganbeauty.app.data.local.entities.ConversationEntity conv =
            com.veganbeauty.app.features.community.message.MessageHelper.getConversationById(this, skinChatConvId);
        if (conv == null || conv.getMessages() == null || conv.getMessages().isEmpty()) {
            com.veganbeauty.app.features.community.message.MessageHelper.getOrCreateConversation(
                this,
                currentUserId,
                RootieBrandHelper.USER_ID_VN,
                "Rootie VietNam",
                RootieBrandHelper.AVATAR_URL
            );
            com.veganbeauty.app.features.community.message.MessageHelper.sendMessage(
                this,
                skinChatConvId,
                "rootie_vn",
                currentUserId,
                "Chào bạn! Tôi có thể giúp gì cho làn da của bạn hôm nay?"
            );
        }

        // Start listening to the expert conversation
        com.veganbeauty.app.features.community.message.MessageHelper.listenToConversation(this, skinChatConvId, () -> {
            updateUnreadBadges();
        });

        // Initial update of badges
        updateUnreadBadges();
        
        setupRewardPointsTierListener();
    }

    private void setupRewardPointsTierListener() {
        try {
            com.veganbeauty.app.data.local.RootieDatabase db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(this);
            androidx.lifecycle.FlowLiveDataConversions.asLiveData(db.rewardPointDao().getTotalPointsFlow())
                .observe(this, ptsList -> {
                    int pts = (ptsList != null && !ptsList.isEmpty()) ? ptsList.get(0).total : 0;
                    String newTier;
                    if (pts >= 20000) newTier = "VIP";
                    else if (pts >= 10000) newTier = "Vàng";
                    else if (pts >= 5000) newTier = "Bạc";
                    else newTier = "Thường";

                    android.content.SharedPreferences prefs = getSharedPreferences("RootiePrefs", MODE_PRIVATE);
                    String oldTier = prefs.getString("LAST_KNOWN_TIER", null);
                    
                    if (oldTier == null) {
                        prefs.edit().putString("LAST_KNOWN_TIER", newTier).apply();
                    } else if (!oldTier.equals(newTier)) {
                        prefs.edit().putString("LAST_KNOWN_TIER", newTier).apply();
                        
                        int oldLvl = 0;
                        if ("Bạc".equals(oldTier)) oldLvl = 1;
                        else if ("Vàng".equals(oldTier)) oldLvl = 2;
                        else if ("VIP".equals(oldTier)) oldLvl = 3;
                        
                        int newLvl = 0;
                        if ("Bạc".equals(newTier)) newLvl = 1;
                        else if ("Vàng".equals(newTier)) newLvl = 2;
                        else if ("VIP".equals(newTier)) newLvl = 3;
                        
                        if (newLvl > oldLvl) {
                            String title = "Thăng hạng thành viên mới! \uD83D\uDC51";
                            String content = "Chúc mừng bạn đã thăng hạng thành công từ hạng " + oldTier + " lên hạng " + newTier + "!";
                            
                            String id = java.util.UUID.randomUUID().toString();
                            String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
                            com.veganbeauty.app.data.local.entities.NotificationItem item = new com.veganbeauty.app.data.local.entities.NotificationItem(
                                    id,
                                    title,
                                    content,
                                    currentDate,
                                    "Tài khoản",
                                    null,
                                    null,
                                    "XEM HẠNG",
                                    false,
                                    "Hôm nay",
                                    "ic_bell",
                                    "member_tier",
                                    null,
                                    null
                            );
                            com.veganbeauty.app.data.repository.NotificationRepository.getInstance(MainActivity.this).addNotification(item);
                            
                            com.veganbeauty.app.features.account.notification.NotificationPushHelper.sendPushNotification(
                                    MainActivity.this,
                                    id,
                                    title,
                                    content,
                                    "member_tier",
                                    null,
                                    null,
                                    null
                            );
                            
                            String currentUserId = com.veganbeauty.app.data.local.ProfileSession.INSTANCE.getCurrentUserId(MainActivity.this);
                            new com.veganbeauty.app.data.remote.FirestoreService().sendCommunityNotificationEvent(
                                    currentUserId,
                                    "rootie_system",
                                    "Rootie System",
                                    "",
                                    "MEMBER_TIER",
                                    "UPDATE",
                                    content,
                                    null,
                                    null
                            );
                        }
                    }
                });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUnreadBadges() {
        if (tvMascotBadge == null || viewHumanBadge == null || viewAiBadge == null) return;

        android.content.SharedPreferences quizPrefs = getSharedPreferences("RootieQuizPrefs", MODE_PRIVATE);
        boolean isAiUnread = quizPrefs.getBoolean("SKIN_AI_CHAT_UNREAD", true);

        String currentUserId = com.veganbeauty.app.data.local.ProfileSession.INSTANCE.getCurrentUserId(this);
        String skinChatConvId = "chat_rootie_vn_" + currentUserId;
        com.veganbeauty.app.data.local.entities.ConversationEntity conv =
            com.veganbeauty.app.features.community.message.MessageHelper.getConversationById(this, skinChatConvId);
        
        boolean isHumanUnread = false;
        if (conv != null && conv.getUnreadBy() != null) {
            isHumanUnread = conv.getUnreadBy().contains(currentUserId);
        }

        // Set sub-bubble badges visibility
        viewHumanBadge.setVisibility(isHumanUnread ? View.VISIBLE : View.GONE);
        viewAiBadge.setVisibility(isAiUnread ? View.VISIBLE : View.GONE);

        // Set main mascot badge visibility
        if (isExtraBubblesExpanded) {
            // When expanded, hide the main mascot badge
            tvMascotBadge.setVisibility(View.GONE);
        } else {
            // When collapsed, show main mascot badge with unread count
            int unreadCount = 0;
            if (isAiUnread) unreadCount++;
            if (isHumanUnread) unreadCount++;

            if (unreadCount > 0) {
                tvMascotBadge.setVisibility(View.VISIBLE);
                tvMascotBadge.setText(String.valueOf(unreadCount));
            } else {
                tvMascotBadge.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            String currentUserId = com.veganbeauty.app.data.local.ProfileSession.INSTANCE.getCurrentUserId(this);
            String skinChatConvId = "chat_rootie_vn_" + currentUserId;
            com.veganbeauty.app.features.community.message.MessageHelper.removeConversationListener(skinChatConvId);
            com.veganbeauty.app.data.remote.FirestoreService fsService = new com.veganbeauty.app.data.remote.FirestoreService();
            fsService.stopListeningToNotifications();
            fsService.stopListeningToUserEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dockChatHead(final boolean isLeft) {
        final View chatHead = findViewById(R.id.skin_ai_floating_chat_head);
        if (chatHead == null || chatHeadArrow == null) return;

        collapseExtraBubblesImmediately();
        isDockedToLeft = isLeft;

        final int screenWidth = getResources().getDisplayMetrics().widthPixels;
        final float density = getResources().getDisplayMetrics().density;
        float targetX = isLeft ? -chatHead.getWidth() : screenWidth;

        chatHead.animate()
                .x(targetX)
                .setDuration(250)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        chatHead.setVisibility(View.GONE);
                        
                        float arrowY = chatHead.getY() + (chatHead.getHeight() - chatHeadArrow.getHeight()) / 2f;
                        int statusBarHeight = (int) (24 * density);
                        int navigationBarHeight = (int) (48 * density);
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        if (arrowY < statusBarHeight) arrowY = statusBarHeight;
                        if (arrowY > screenHeight - chatHeadArrow.getHeight() - navigationBarHeight) {
                            arrowY = screenHeight - chatHeadArrow.getHeight() - navigationBarHeight;
                        }

                        float arrowX = isLeft ? -12 * density : screenWidth - 20 * density;
                        chatHeadArrow.setX(arrowX);
                        chatHeadArrow.setY(arrowY);

                        ImageView arrowIcon = findViewById(R.id.skin_ai_floating_chat_head_arrow_icon);
                        if (arrowIcon != null) {
                            arrowIcon.setImageResource(isLeft ? R.drawable.ic_arrow_next : R.drawable.ic_arrow_back);
                        }

                        chatHeadArrow.setVisibility(View.VISIBLE);
                        chatHeadArrow.setScaleX(0f);
                        chatHeadArrow.setScaleY(0f);
                        chatHeadArrow.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(200)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                                    .start();
                    }
                })
                .start();
    }

    private void undockChatHead() {
        final View chatHead = findViewById(R.id.skin_ai_floating_chat_head);
        if (chatHead == null || chatHeadArrow == null) return;

        final int screenWidth = getResources().getDisplayMetrics().widthPixels;
        final float density = getResources().getDisplayMetrics().density;

        chatHeadArrow.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(150)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            chatHeadArrow.setVisibility(View.GONE);
                            chatHead.setVisibility(View.VISIBLE);
                            
                            float targetX = isDockedToLeft ? 16 * density : screenWidth - chatHead.getWidth() - (16 * density);
                            chatHead.animate()
                                    .x(targetX)
                                    .setDuration(300)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                    .start();
                        }
                    })
                    .start();
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Handle push notification intents
        com.veganbeauty.app.features.account.notification.NotificationIntentHandler.handleIntent(this, intent);

        String navigateTo = intent.getStringExtra("NAVIGATE_TO");
        if (navigateTo != null) {
            androidx.fragment.app.Fragment destination = null;
            if ("SKIN_REMINDER".equals(navigateTo)) {
                destination = new com.veganbeauty.app.features.routine.SkinRoutineSettingsFragment();
            } else if ("WEATHER_FORECAST".equals(navigateTo) || "SKIN_WEATHER_FORECAST".equals(navigateTo)) {
                destination = new com.veganbeauty.app.features.weather.SkinWeatherForecastFragment();
            }
            if (destination != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, destination)
                    .commit();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (isExtraBubblesExpanded && ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            View chatHead = findViewById(R.id.skin_ai_floating_chat_head);
            if (chatHead != null && bubbleAi != null && bubbleHuman != null) {
                int[] chatHeadLocation = new int[2];
                int[] aiLocation = new int[2];
                int[] humanLocation = new int[2];

                chatHead.getLocationOnScreen(chatHeadLocation);
                bubbleAi.getLocationOnScreen(aiLocation);
                bubbleHuman.getLocationOnScreen(humanLocation);

                float x = ev.getRawX();
                float y = ev.getRawY();

                boolean insideChatHead = x >= chatHeadLocation[0] && x <= chatHeadLocation[0] + chatHead.getWidth()
                        && y >= chatHeadLocation[1] && y <= chatHeadLocation[1] + chatHead.getHeight();

                boolean insideAi = x >= aiLocation[0] && x <= aiLocation[0] + bubbleAi.getWidth()
                        && y >= aiLocation[1] && y <= aiLocation[1] + bubbleAi.getHeight();

                boolean insideHuman = x >= humanLocation[0] && x <= humanLocation[0] + bubbleHuman.getWidth()
                        && y >= humanLocation[1] && y <= humanLocation[1] + bubbleHuman.getHeight();

                if (!insideChatHead && !insideAi && !insideHuman) {
                    toggleExtraBubbles();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, fragment)
                .commit();
    }
}
