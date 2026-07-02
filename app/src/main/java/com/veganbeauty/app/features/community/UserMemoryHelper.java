package com.veganbeauty.app.features.community;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserMemoryHelper {
    private static final String FILE_NAME = "user_post_actions.json";
    private static final String LEGACY_FILE_NAME = "user_memory.json";
    private static final String SEED_ASSET_NAME = "User_com_post_memory.json";
    private static boolean legacyMigrated = false;

    private UserMemoryHelper() {
    }

    private static File getMemoryFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    private static void migrateLegacyIfNeeded(Context context) {
        if (legacyMigrated) return;
        legacyMigrated = true;

        File legacyFile = new File(context.getFilesDir(), LEGACY_FILE_NAME);
        if (!legacyFile.exists()) return;

        try {
            JSONArray legacyArray = readJsonArrayFromFile(legacyFile);
            JSONArray current = readLocalActions(context);
            boolean changed = false;

            for (int i = 0; i < legacyArray.length(); i++) {
                JSONObject obj = legacyArray.optJSONObject(i);
                if (obj == null || !obj.has("userId")) continue;

                String userId = obj.optString("userId");
                if (userId.isEmpty()) continue;

                JSONObject existing = findEntryInArray(current, userId);
                if (existing == null) {
                    current.put(cloneUserEntry(obj));
                    changed = true;
                }
            }

            if (changed) {
                writeLocalActions(context, current);
            }
        } catch (Exception ignored) {
        }
    }

    private static JSONObject cloneUserEntry(JSONObject source) throws Exception {
        JSONObject copy = new JSONObject();
        copy.put("userId", source.optString("userId"));
        copy.put("repostedPostIds", source.optJSONArray("repostedPostIds") != null
                ? new JSONArray(source.optJSONArray("repostedPostIds").toString())
                : new JSONArray());
        copy.put("savedPostIds", source.optJSONArray("savedPostIds") != null
                ? new JSONArray(source.optJSONArray("savedPostIds").toString())
                : new JSONArray());
        return copy;
    }

    private static JSONArray readJsonArrayFromFile(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return new JSONArray(sb.toString());
        }
    }

    private static JSONArray readLocalActions(Context context) {
        migrateLegacyIfNeeded(context);
        File file = getMemoryFile(context);
        if (!file.exists()) {
            return new JSONArray();
        }
        try {
            return readJsonArrayFromFile(file);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static void writeLocalActions(Context context, JSONArray jsonArray) {
        File file = getMemoryFile(context);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONArray readSeedActions(Context context) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(SEED_ASSET_NAME), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return new JSONArray(sb.toString());
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static JSONObject findEntryInArray(JSONArray array, String userId) {
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null && userId.equals(obj.optString("userId"))) {
                return obj;
            }
        }
        return null;
    }

    private static JSONObject findUserEntry(Context context, String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        JSONObject local = findEntryInArray(readLocalActions(context), userId);
        if (local != null) {
            return local;
        }
        return findEntryInArray(readSeedActions(context), userId);
    }

    private static JSONObject getOrCreateLocalUserEntry(Context context, String userId) {
        JSONArray memoryArray = readLocalActions(context);
        JSONObject existing = findEntryInArray(memoryArray, userId);
        if (existing != null) {
            return existing;
        }

        JSONObject seed = findEntryInArray(readSeedActions(context), userId);
        JSONObject newObj = new JSONObject();
        try {
            newObj.put("userId", userId);
            newObj.put("repostedPostIds", seed != null && seed.optJSONArray("repostedPostIds") != null
                    ? new JSONArray(seed.optJSONArray("repostedPostIds").toString())
                    : new JSONArray());
            newObj.put("savedPostIds", seed != null && seed.optJSONArray("savedPostIds") != null
                    ? new JSONArray(seed.optJSONArray("savedPostIds").toString())
                    : new JSONArray());
        } catch (Exception e) {
            return newObj;
        }
        return newObj;
    }

    private static void saveUserMemory(Context context, JSONObject userMemory) {
        JSONArray memoryArray = readLocalActions(context);
        String userId = userMemory.optString("userId");
        boolean found = false;
        try {
            for (int i = 0; i < memoryArray.length(); i++) {
                JSONObject obj = memoryArray.optJSONObject(i);
                if (obj != null && userId.equals(obj.optString("userId"))) {
                    memoryArray.put(i, userMemory);
                    found = true;
                    break;
                }
            }
            if (!found) {
                memoryArray.put(userMemory);
            }
            writeLocalActions(context, memoryArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Set<String> toPostIdSet(JSONObject memory, String key) {
        if (memory == null) {
            return Collections.emptySet();
        }
        JSONArray arr = memory.optJSONArray(key);
        if (arr == null || arr.length() == 0) {
            return Collections.emptySet();
        }
        HashSet<String> ids = new HashSet<>();
        for (int i = 0; i < arr.length(); i++) {
            String id = arr.optString(i);
            if (id != null && !id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }

    public static Set<String> getRepostedPostIds(Context context, String userId) {
        return toPostIdSet(findUserEntry(context, userId), "repostedPostIds");
    }

    public static Set<String> getSavedPostIds(Context context, String userId) {
        return toPostIdSet(findUserEntry(context, userId), "savedPostIds");
    }

    public static boolean isPostReposted(Context context, String userId, String postId) {
        return getRepostedPostIds(context, userId).contains(postId);
    }

    public static boolean toggleRepost(Context context, String userId, String postId) {
        try {
            JSONObject memory = getOrCreateLocalUserEntry(context, userId);
            JSONArray arr = memory.optJSONArray("repostedPostIds");
            if (arr == null) arr = new JSONArray();

            int foundIdx = -1;
            for (int i = 0; i < arr.length(); i++) {
                if (arr.optString(i).equals(postId)) {
                    foundIdx = i;
                    break;
                }
            }

            boolean isNowReposted;
            if (foundIdx >= 0) {
                arr.remove(foundIdx);
                isNowReposted = false;
            } else {
                arr.put(postId);
                isNowReposted = true;
            }

            memory.put("repostedPostIds", arr);
            saveUserMemory(context, memory);
            return isNowReposted;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isPostSaved(Context context, String userId, String postId) {
        return getSavedPostIds(context, userId).contains(postId);
    }

    public static boolean toggleSave(Context context, String userId, String postId) {
        try {
            JSONObject memory = getOrCreateLocalUserEntry(context, userId);
            JSONArray arr = memory.optJSONArray("savedPostIds");
            if (arr == null) arr = new JSONArray();

            int foundIdx = -1;
            for (int i = 0; i < arr.length(); i++) {
                if (arr.optString(i).equals(postId)) {
                    foundIdx = i;
                    break;
                }
            }

            boolean isNowSaved;
            if (foundIdx >= 0) {
                arr.remove(foundIdx);
                isNowSaved = false;
            } else {
                arr.put(postId);
                isNowSaved = true;
            }

            memory.put("savedPostIds", arr);
            saveUserMemory(context, memory);
            return isNowSaved;
        } catch (Exception e) {
            return false;
        }
    }
}
