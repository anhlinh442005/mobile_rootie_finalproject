package com.veganbeauty.app.features.community;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.remote.FirestoreService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Upload {@code User_com_friend.json} lên Firestore collection {@code user_social} (một lần).
 */
public final class UserSocialSeeder {

    private static final String TAG = "UserSocialSeeder";
    private static final String PREFS = "RootieQuizPrefs";
    private static final String PREF_KEY = "user_social_firebase_v1";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private UserSocialSeeder() {
    }

    public static void seedIfNeeded(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_KEY, false)) {
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                String json = new LocalJsonReader(appContext).readAsset("User_com_friend.json");
                if (json == null || json.trim().isEmpty()) {
                    Log.w(TAG, "User_com_friend.json not found in assets");
                    return;
                }
                boolean uploaded = new FirestoreService().syncUserSocialFromJson(json);
                if (uploaded) {
                    prefs.edit().putBoolean(PREF_KEY, true).apply();
                    Log.i(TAG, "Uploaded user social graph to Firestore");
                } else {
                    Log.w(TAG, "User social upload failed");
                }
            } catch (Exception e) {
                Log.e(TAG, "seedIfNeeded failed", e);
            }
        });
    }
}
