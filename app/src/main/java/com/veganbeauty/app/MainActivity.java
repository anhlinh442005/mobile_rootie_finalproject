package com.veganbeauty.app;

import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.os.Handler;
import android.content.DialogInterface;
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
                android.content.SharedPreferences prefs = getSharedPreferences("RootieQuizPrefs", MODE_PRIVATE);
                String savedSkin = prefs.getString("SAVED_USER_SKIN_TYPE", null);
                if (savedSkin != null) {
                    destination = new com.veganbeauty.app.features.weather.WeatherForecastFragment();
                } else {
                    destination = new com.veganbeauty.app.features.quiz.QuizTestIntroFragment();
                }
            }
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, destination)
                .commit();
        }

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
                            obj.optString("avatar", null)
                        );

                    // Update avatar in SQLite (upsert) using the sync method
                    userDao.insertUserSync(user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        setupFloatingChatHead();
    }

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

        android.content.SharedPreferences prefs = getSharedPreferences("RootieQuizPrefs", MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("SKIN_AI_FLOATING_CHAT_ENABLED", true);
        chatHead.setVisibility(enabled ? View.VISIBLE : View.GONE);

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
                            openChatFragment();
                        } else {
                            int screenWidth = getResources().getDisplayMetrics().widthPixels;
                            float currentX = chatHead.getX();
                            float centerX = currentX + chatHead.getWidth() / 2f;
                            
                            float targetX;
                            if (centerX < screenWidth / 2f) {
                                targetX = 16 * getResources().getDisplayMetrics().density;
                            } else {
                                targetX = screenWidth - chatHead.getWidth() - (16 * getResources().getDisplayMetrics().density);
                            }
                            
                            chatHead.animate()
                                    .x(targetX)
                                    .setDuration(250)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                    .start();
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

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
