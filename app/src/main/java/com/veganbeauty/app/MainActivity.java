package com.veganbeauty.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.features.home.HomeFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            android.content.SharedPreferences prefs = getSharedPreferences("RootieQuizPrefs", MODE_PRIVATE);
            String savedSkin = prefs.getString("SAVED_USER_SKIN_TYPE", null);
            androidx.fragment.app.Fragment destination = new com.veganbeauty.app.features.home.HomeFragment();
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, destination)
                .commit();
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
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, fragment)
                .commit();
    }
}
