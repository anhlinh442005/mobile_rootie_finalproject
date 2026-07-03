package com.veganbeauty.app.features.community;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.features.community.message.MessageHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Upload personalized {@code community_message.json} templates to Firestore (one-time).
 */
public final class CommunityMessageSeeder {

    private static final String TAG = "CommunityMessageSeeder";
    private static final String PREFS = "RootieQuizPrefs";
    private static final String PREF_KEY = "community_message_firebase_v1";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private CommunityMessageSeeder() {
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
                String templateJson = new LocalJsonReader(appContext).readAsset("community_message.json");
                if (templateJson == null || templateJson.trim().isEmpty()) {
                    Log.w(TAG, "community_message.json not found in assets");
                    return;
                }

                String usersJson = new LocalJsonReader(appContext).readAsset("users.json");
                if (usersJson == null || usersJson.trim().isEmpty()) {
                    Log.w(TAG, "users.json not found in assets");
                    return;
                }

                List<ConversationEntity> allConversations = new ArrayList<>();
                JSONArray users = new JSONArray(usersJson.replace("\uFEFF", ""));
                for (int i = 0; i < users.length(); i++) {
                    JSONObject user = users.getJSONObject(i);
                    String userId = user.optString("user_id", "");
                    if (userId.isEmpty()) {
                        continue;
                    }
                    String userName = user.optString("full_name", user.optString("username", "Bạn"));
                    String userAvatar = user.optString("avatar", "");
                    List<ConversationEntity> personalized = MessageHelper.parsePersonalizedTemplates(
                            templateJson,
                            userId,
                            userName,
                            userAvatar
                    );
                    allConversations.addAll(personalized);
                }

                if (allConversations.isEmpty()) {
                    Log.w(TAG, "No conversations to upload");
                    return;
                }

                String payload = new Gson().toJson(allConversations);
                boolean uploaded = new FirestoreService().syncCommunityMessagesFromJson(payload, true);
                if (uploaded) {
                    prefs.edit().putBoolean(PREF_KEY, true).apply();
                    Log.i(TAG, "Uploaded " + allConversations.size() + " conversations to Firestore");
                } else {
                    Log.w(TAG, "Community message upload failed");
                }
            } catch (Exception e) {
                Log.e(TAG, "seedIfNeeded failed", e);
            }
        });
    }

    public static List<ConversationEntity> buildAllPersonalizedConversations(String templateJson, String usersJson)
            throws Exception {
        List<ConversationEntity> allConversations = new ArrayList<>();
        JSONArray users = new JSONArray(usersJson.replace("\uFEFF", ""));
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            String userId = user.optString("user_id", "");
            if (userId.isEmpty()) {
                continue;
            }
            String userName = user.optString("full_name", user.optString("username", "Bạn"));
            String userAvatar = user.optString("avatar", "");
            allConversations.addAll(MessageHelper.parsePersonalizedTemplates(
                    templateJson,
                    userId,
                    userName,
                    userAvatar
            ));
        }
        return allConversations;
    }
}
