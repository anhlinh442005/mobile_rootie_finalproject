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

public class UserMemoryHelper {
    private static final String FILE_NAME = "user_memory.json";

    private static File getMemoryFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    private static JSONArray readMemory(Context context) {
        File file = getMemoryFile(context);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return new JSONArray(sb.toString());
            } catch (Exception e) {
                return new JSONArray();
            }
        } else {
            return new JSONArray();
        }
    }

    private static void writeMemory(Context context, JSONArray jsonArray) {
        File file = getMemoryFile(context);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONObject getUserMemory(Context context, String userId) {
        JSONArray memoryArray = readMemory(context);
        for (int i = 0; i < memoryArray.length(); i++) {
            JSONObject obj = memoryArray.optJSONObject(i);
            if (obj != null && obj.optString("userId").equals(userId)) {
                return obj;
            }
        }
        JSONObject newObj = new JSONObject();
        try {
            newObj.put("userId", userId);
            newObj.put("repostedPostIds", new JSONArray());
            newObj.put("savedPostIds", new JSONArray());
        } catch (Exception e) {}
        return newObj;
    }

    private static void saveUserMemory(Context context, JSONObject userMemory) {
        JSONArray memoryArray = readMemory(context);
        boolean found = false;
        try {
            for (int i = 0; i < memoryArray.length(); i++) {
                JSONObject obj = memoryArray.optJSONObject(i);
                if (obj != null && obj.optString("userId").equals(userMemory.optString("userId"))) {
                    memoryArray.put(i, userMemory);
                    found = true;
                    break;
                }
            }
            if (!found) {
                memoryArray.put(userMemory);
            }
            writeMemory(context, memoryArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isPostReposted(Context context, String userId, String postId) {
        JSONObject memory = getUserMemory(context, userId);
        JSONArray arr = memory.optJSONArray("repostedPostIds");
        if (arr == null) return false;
        for (int i = 0; i < arr.length(); i++) {
            if (arr.optString(i).equals(postId)) return true;
        }
        return false;
    }

    public static boolean toggleRepost(Context context, String userId, String postId) {
        try {
            JSONObject memory = getUserMemory(context, userId);
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
        JSONObject memory = getUserMemory(context, userId);
        JSONArray arr = memory.optJSONArray("savedPostIds");
        if (arr == null) return false;
        for (int i = 0; i < arr.length(); i++) {
            if (arr.optString(i).equals(postId)) return true;
        }
        return false;
    }

    public static boolean toggleSave(Context context, String userId, String postId) {
        try {
            JSONObject memory = getUserMemory(context, userId);
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
