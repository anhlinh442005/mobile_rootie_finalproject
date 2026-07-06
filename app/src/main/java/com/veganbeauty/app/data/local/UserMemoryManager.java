package com.veganbeauty.app.data.local;

import android.content.Context;

import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserMemoryManager {

    public static class HandbookCategory {
        private String id;
        private String name;
        private List<YtVideoEntity> videos;

        public HandbookCategory(String id, String name, List<YtVideoEntity> videos) {
            this.id = id != null ? id : UUID.randomUUID().toString();
            this.name = name;
            this.videos = videos != null ? videos : new ArrayList<>();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public List<YtVideoEntity> getVideos() { return videos; }
        
        public void setName(String name) { this.name = name; }
    }

    private final Context context;
    private final String fileName = "user_handbook_memory.json";
    private static final String LEGACY_FILE_NAME = "user_memory.json";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UserMemoryManager(Context context) {
        this.context = context;
    }

    public List<HandbookCategory> getCategories() {
        File file = resolveHandbookFile();
        if (!file.exists()) return new ArrayList<>();
        List<HandbookCategory> list = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                HandbookCategory cat = new HandbookCategory(
                        obj.optString("id", UUID.randomUUID().toString()),
                        obj.optString("name", ""),
                        new ArrayList<>()
                );
                JSONArray videosArr = obj.optJSONArray("videos");
                if (videosArr != null) {
                    for (int j = 0; j < videosArr.length(); j++) {
                        JSONObject vObj = videosArr.getJSONObject(j);
                        cat.getVideos().add(new YtVideoEntity(
                                vObj.optString("id", ""),
                                vObj.optString("title", ""),
                                vObj.optString("url", ""),
                                vObj.optString("description", ""),
                                vObj.optString("username", ""),
                                vObj.has("avatarUrl") && !vObj.isNull("avatarUrl") ? vObj.optString("avatarUrl") : null,
                                vObj.optString("type", "")
                        ));
                    }
                }
                list.add(cat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private File resolveHandbookFile() {
        File file = new File(context.getFilesDir(), fileName);
        if (file.exists()) {
            return file;
        }
        File legacy = new File(context.getFilesDir(), LEGACY_FILE_NAME);
        if (legacy.exists()) {
            return legacy;
        }
        return file;
    }

    public void saveCategories(List<HandbookCategory> categories) {
        File file = new File(context.getFilesDir(), fileName);
        try {
            JSONArray array = new JSONArray();
            for (HandbookCategory cat : categories) {
                JSONObject obj = new JSONObject();
                obj.put("id", cat.getId());
                obj.put("name", cat.getName());
                JSONArray videosArr = new JSONArray();
                for (YtVideoEntity v : cat.getVideos()) {
                    JSONObject vObj = new JSONObject();
                    vObj.put("id", v.getId());
                    vObj.put("title", v.getTitle());
                    vObj.put("url", v.getUrl());
                    vObj.put("description", v.getDescription());
                    vObj.put("username", v.getUsername());
                    vObj.put("avatarUrl", v.getAvatarUrl());
                    vObj.put("type", v.getType());
                    videosArr.put(vObj);
                }
                obj.put("videos", videosArr);
                array.put(obj);
            }
            String jsonString = array.toString();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addVideoToCategory(String categoryName, YtVideoEntity video) {
        List<HandbookCategory> categories = getCategories();
        HandbookCategory category = null;
        for (HandbookCategory cat : categories) {
            if (cat.getName().equals(categoryName)) {
                category = cat;
                break;
            }
        }
        if (category == null) {
            category = new HandbookCategory(null, categoryName, null);
            categories.add(category);
        }
        
        boolean exists = false;
        for (YtVideoEntity v : category.getVideos()) {
            if (v.getId().equals(video.getId()) || v.getUrl().equals(video.getUrl())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            category.getVideos().add(video);
            saveCategories(categories);
        }
    }

    public void removeVideoFromCategory(String categoryName, YtVideoEntity video) {
        List<HandbookCategory> categories = getCategories();
        for (HandbookCategory category : categories) {
            if (category.getName().equals(categoryName)) {
                for (int i = 0; i < category.getVideos().size(); i++) {
                    YtVideoEntity v = category.getVideos().get(i);
                    if (v.getId().equals(video.getId()) || v.getUrl().equals(video.getUrl())) {
                        category.getVideos().remove(i);
                        break;
                    }
                }
                saveCategories(categories);
                break;
            }
        }
    }
}
