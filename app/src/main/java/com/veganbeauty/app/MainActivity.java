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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Schedule daily weather & skin advice notification at 6:30 AM
        com.veganbeauty.app.features.weather.DailySkinWeatherScheduler.INSTANCE.scheduleDailyNotification(getApplicationContext());

        if (savedInstanceState == null) {
            String navigateTo = getIntent().getStringExtra("NAVIGATE_TO");
            androidx.fragment.app.Fragment destination;
            if ("SKIN_REMINDER".equals(navigateTo)) {
                destination = new com.veganbeauty.app.features.routine.SkinRoutineSettingsFragment();
            } else if ("WEATHER_FORECAST".equals(navigateTo)) {
                destination = new com.veganbeauty.app.features.weather.WeatherForecastFragment();
            } else {
                destination = new com.veganbeauty.app.features.home.HomeFragment();
            }
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, destination)
                .commit();
        }

        if (!com.veganbeauty.app.data.local.ProfileSession.INSTANCE.isLoggedIn(this)) {
            com.veganbeauty.app.features.shop.product.CartHelper.clearCart(this);
        }

        // Trigger ONE-TIME SYNC of all mock data to Firebase
        com.veganbeauty.app.utils.SyncDataHelper.INSTANCE.syncAllLocalDataToFirebase(this);

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

                    com.veganbeauty.app.data.local.entities.UserEntity user =
                        new com.veganbeauty.app.data.local.entities.UserEntity(
                            userId,
                            obj.optString("username", ""),
                            obj.optString("full_name", ""),
                            obj.optString("email", ""),
                            obj.optString("phone", ""),
                            obj.optString("password", ""),
                            obj.optString("avatar", null),
                            obj.optString("primary_image", null)
                        );

                    // Update avatar in SQLite (upsert) using the sync method
                    userDao.insertUserSync(user);
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
            });
    }

    private View bubbleAi;
    private View bubbleHuman;
    private View chatHeadArrow;
    private boolean isExtraBubblesExpanded = false;
    private boolean isDockedToLeft = false;

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
            bubbleAi.setVisibility(View.GONE);
            bubbleAi.setTranslationY(0f);
            bubbleHuman.setVisibility(View.GONE);
            bubbleHuman.setTranslationY(0f);
            isExtraBubblesExpanded = false;
        }
    }

    private void toggleExtraBubbles() {
        final View chatHead = findViewById(R.id.skin_ai_floating_chat_head);
        if (bubbleAi == null || bubbleHuman == null || chatHead == null) return;

        if (isExtraBubblesExpanded) {
            bubbleAi.animate()
                .translationY(0)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> bubbleAi.setVisibility(View.GONE))
                .start();
            bubbleHuman.animate()
                .translationY(0)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> bubbleHuman.setVisibility(View.GONE))
                .start();
            isExtraBubblesExpanded = false;
        } else {
            float mainX = chatHead.getX();
            float mainY = chatHead.getY();
            float density = getResources().getDisplayMetrics().density;
            float offsetNeed = 5 * density;

            float bubbleX = mainX + offsetNeed;
            float bubbleY = mainY + offsetNeed;

            bubbleAi.setX(bubbleX);
            bubbleAi.setY(bubbleY);
            bubbleHuman.setX(bubbleX);
            bubbleHuman.setY(bubbleY);

            bubbleAi.setVisibility(View.VISIBLE);
            bubbleHuman.setVisibility(View.VISIBLE);

            bubbleAi.setAlpha(0f);
            bubbleAi.setTranslationY(0f);
            bubbleHuman.setAlpha(0f);
            bubbleHuman.setTranslationY(0f);

            float targetY_Ai = -65 * density;
            float targetY_Human = -125 * density;

            bubbleAi.animate()
                .translationY(targetY_Ai)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();

            bubbleHuman.animate()
                .translationY(targetY_Human)
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .start();

            isExtraBubblesExpanded = true;
        }
    }

    private void openSkinAiChatDialog() {
        collapseExtraBubblesImmediately();
        com.veganbeauty.app.features.ai.SkinAiChatFragment dialog = new com.veganbeauty.app.features.ai.SkinAiChatFragment();
        dialog.show(getSupportFragmentManager(), "SkinAiChatDialog");
    }

    private void openSkinChatDialog() {
        collapseExtraBubblesImmediately();
        com.veganbeauty.app.features.ai.SkinChatFragment dialog = new com.veganbeauty.app.features.ai.SkinChatFragment();
        dialog.show(getSupportFragmentManager(), "SkinChatDialog");
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
                            arrowIcon.setImageResource(isLeft ? R.drawable.ic_chevron_right : R.drawable.ic_chevron_left);
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
            } else if ("WEATHER_FORECAST".equals(navigateTo)) {
                destination = new com.veganbeauty.app.features.weather.WeatherForecastFragment();
            }
            if (destination != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, destination)
                    .commit();
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, fragment)
                .commit();
    }
}
