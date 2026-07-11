package com.veganbeauty.app.features.community;

import android.content.Context;

import com.veganbeauty.app.data.local.entities.CommunityPostEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        copy.put("repostedPosts", source.optJSONArray("repostedPosts") != null
                ? new JSONArray(source.optJSONArray("repostedPosts").toString())
                : new JSONArray());
        copy.put("savedPostIds", source.optJSONArray("savedPostIds") != null
                ? new JSONArray(source.optJSONArray("savedPostIds").toString())
                : new JSONArray());
        copy.put("savedPosts", source.optJSONArray("savedPosts") != null
                ? new JSONArray(source.optJSONArray("savedPosts").toString())
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
            newObj.put("repostedPosts", seed != null && seed.optJSONArray("repostedPosts") != null
                    ? new JSONArray(seed.optJSONArray("repostedPosts").toString())
                    : new JSONArray());
            newObj.put("savedPostIds", seed != null && seed.optJSONArray("savedPostIds") != null
                    ? new JSONArray(seed.optJSONArray("savedPostIds").toString())
                    : new JSONArray());
            newObj.put("savedPosts", seed != null && seed.optJSONArray("savedPosts") != null
                    ? new JSONArray(seed.optJSONArray("savedPosts").toString())
                    : new JSONArray());
        } catch (Exception e) {
            return newObj;
        }
        return newObj;
    }

    private static JSONObject postToJson(CommunityPostEntity post) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("postId", post.getPostId());
        obj.put("authorId", post.getAuthorId());
        obj.put("authorUsername", post.getAuthorUsername());
        obj.put("authorDisplayName", post.getAuthorDisplayName());
        obj.put("authorAvatarUrl", post.getAuthorAvatarUrl() != null ? post.getAuthorAvatarUrl() : "");
        obj.put("content", post.getContent());
        obj.put("createdAt", post.getCreatedAt());
        obj.put("likesCount", post.getLikesCount());
        obj.put("commentsCount", post.getCommentsCount());
        obj.put("reupsCount", post.getReupsCount());
        obj.put("skinType", post.getSkinType() != null ? post.getSkinType() : "");
        obj.put("concern", post.getConcern() != null ? post.getConcern() : "");
        obj.put("mediaUrlsString", post.getMediaUrlsString());
        obj.put("type", post.getType() != null ? post.getType() : "");
        obj.put("linkedProductIds", post.getLinkedProductIds() != null ? post.getLinkedProductIds() : "");
        return obj;
    }

    private static CommunityPostEntity postFromJson(JSONObject obj) {
        if (obj == null) return null;
        String postId = obj.optString("postId", "");
        if (postId.isEmpty()) return null;
        String authorId = obj.optString("authorId", "");
        String authorUsername = obj.optString("authorUsername", "");
        String authorDisplayName = obj.optString("authorDisplayName", "");
        String authorAvatarUrl = obj.optString("authorAvatarUrl", null);
        if (authorAvatarUrl != null && authorAvatarUrl.isEmpty()) authorAvatarUrl = null;
        String content = obj.optString("content", "");
        String createdAt = obj.optString("createdAt", "");
        int likesCount = obj.optInt("likesCount", 0);
        int commentsCount = obj.optInt("commentsCount", 0);
        int reupsCount = obj.optInt("reupsCount", 0);
        String skinType = obj.optString("skinType", null);
        if (skinType != null && skinType.isEmpty()) skinType = null;
        String concern = obj.optString("concern", null);
        if (concern != null && concern.isEmpty()) concern = null;
        String mediaUrlsString = obj.optString("mediaUrlsString", "");
        String type = obj.optString("type", null);
        if (type != null && type.isEmpty()) type = null;
        String linkedProductIds = obj.optString("linkedProductIds", null);
        if (linkedProductIds != null && linkedProductIds.isEmpty()) linkedProductIds = null;
        return new CommunityPostEntity(
                postId, authorId, authorUsername, authorDisplayName, authorAvatarUrl,
                content, createdAt, likesCount, commentsCount, reupsCount,
                skinType, concern, mediaUrlsString, type, linkedProductIds
        );
    }

    /** Prefer the variant with more displayable content (news snapshot vs Room stub). */
    public static int postRichnessScore(CommunityPostEntity post) {
        if (post == null) {
            return 0;
        }
        int score = 0;
        String content = post.getContent();
        if (content != null && !content.trim().isEmpty()) {
            score += 10 + Math.min(content.trim().length(), 500);
        }
        String media = post.getMediaUrlsString();
        if (media != null && !media.trim().isEmpty()) {
            score += 25;
        }
        String linked = post.getLinkedProductIds();
        if (linked != null && !linked.trim().isEmpty()) {
            score += 5;
        }
        String author = post.getAuthorDisplayName();
        if (author != null && !author.trim().isEmpty()) {
            score += 2;
        }
        return score;
    }

    public static CommunityPostEntity pickRicherPost(CommunityPostEntity candidate, CommunityPostEntity existing) {
        if (candidate == null) {
            return existing;
        }
        if (existing == null) {
            return candidate;
        }
        return postRichnessScore(candidate) >= postRichnessScore(existing) ? candidate : existing;
    }

    @SafeVarargs
    public static List<CommunityPostEntity> mergePostSources(List<CommunityPostEntity>... sources) {
        LinkedHashMap<String, CommunityPostEntity> byId = new LinkedHashMap<>();
        if (sources == null) {
            return new ArrayList<>();
        }
        for (List<CommunityPostEntity> list : sources) {
            if (list == null) {
                continue;
            }
            for (CommunityPostEntity post : list) {
                if (post == null || post.getPostId() == null || post.getPostId().trim().isEmpty()) {
                    continue;
                }
                String id = post.getPostId().trim();
                byId.put(id, pickRicherPost(post, byId.get(id)));
            }
        }
        return new ArrayList<>(byId.values());
    }

    private static void upsertPostSnapshot(JSONObject memory, String arrayKey, CommunityPostEntity post) throws Exception {
        JSONArray snapshots = memory.optJSONArray(arrayKey);
        if (snapshots == null) {
            snapshots = new JSONArray();
        }
        String postId = post.getPostId();
        int foundIdx = -1;
        CommunityPostEntity existingPost = null;
        for (int i = 0; i < snapshots.length(); i++) {
            JSONObject obj = snapshots.optJSONObject(i);
            if (obj != null && postId.equals(obj.optString("postId"))) {
                foundIdx = i;
                existingPost = postFromJson(obj);
                break;
            }
        }
        CommunityPostEntity merged = pickRicherPost(post, existingPost);
        JSONObject snapshot = postToJson(merged);
        if (foundIdx >= 0) {
            snapshots.put(foundIdx, snapshot);
        } else {
            snapshots.put(snapshot);
        }
        memory.put(arrayKey, snapshots);
    }

    private static void removePostSnapshot(JSONObject memory, String arrayKey, String postId) throws Exception {
        JSONArray snapshots = memory.optJSONArray(arrayKey);
        if (snapshots == null) return;
        for (int i = snapshots.length() - 1; i >= 0; i--) {
            JSONObject obj = snapshots.optJSONObject(i);
            if (obj != null && postId.equals(obj.optString("postId"))) {
                snapshots.remove(i);
            }
        }
        memory.put(arrayKey, snapshots);
    }

    private static void upsertSavedPostSnapshot(JSONObject memory, CommunityPostEntity post) throws Exception {
        upsertPostSnapshot(memory, "savedPosts", post);
    }

    private static void removeSavedPostSnapshot(JSONObject memory, String postId) throws Exception {
        removePostSnapshot(memory, "savedPosts", postId);
    }

    private static void upsertRepostedPostSnapshot(JSONObject memory, CommunityPostEntity post) throws Exception {
        upsertPostSnapshot(memory, "repostedPosts", post);
    }

    private static void removeRepostedPostSnapshot(JSONObject memory, String postId) throws Exception {
        removePostSnapshot(memory, "repostedPosts", postId);
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
        return toggleRepost(context, userId, postId, null);
    }

    public static boolean toggleRepost(Context context, String userId, CommunityPostEntity post) {
        if (post == null || post.getPostId() == null || post.getPostId().isEmpty()) {
            return false;
        }
        return toggleRepost(context, userId, post.getPostId(), post);
    }

    private static boolean toggleRepost(Context context, String userId, String postId, CommunityPostEntity post) {
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
                removeRepostedPostSnapshot(memory, postId);
                isNowReposted = false;
            } else {
                arr.put(postId);
                if (post != null) {
                    upsertRepostedPostSnapshot(memory, post);
                }
                isNowReposted = true;
            }

            memory.put("repostedPostIds", arr);
            saveUserMemory(context, memory);
            return isNowReposted;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<CommunityPostEntity> getRepostedPosts(Context context, String userId) {
        JSONObject memory = findUserEntry(context, userId);
        if (memory == null) {
            return Collections.emptyList();
        }
        List<CommunityPostEntity> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        JSONArray snapshots = memory.optJSONArray("repostedPosts");
        if (snapshots != null) {
            for (int i = 0; i < snapshots.length(); i++) {
                CommunityPostEntity post = postFromJson(snapshots.optJSONObject(i));
                if (post != null && seen.add(post.getPostId())) {
                    result.add(post);
                }
            }
        }
        return result;
    }

    public static List<CommunityPostEntity> resolveRepostedPosts(
            Context context,
            String userId,
            List<CommunityPostEntity> availablePosts
    ) {
        return resolveMemoryPosts(context, userId, availablePosts, "repostedPostIds", getRepostedPosts(context, userId));
    }

    /** Convert explore/handbook video into a post snapshot so it appears on that user's profile tabs. */
    public static CommunityPostEntity postFromVideo(com.veganbeauty.app.data.local.entities.YtVideoEntity video) {
        if (video == null || video.getId() == null || video.getId().trim().isEmpty()) {
            return null;
        }
        String username = video.getUsername() != null ? video.getUsername() : "explore";
        String display = username.startsWith("@") ? username.substring(1) : username;
        String content = video.getTitle() != null && !video.getTitle().isEmpty()
                ? video.getTitle()
                : (video.getDescription() != null ? video.getDescription() : "");
        String media = video.getUrl() != null ? video.getUrl() : "";
        return new CommunityPostEntity(
                video.getId(),
                "video_" + display,
                username,
                display,
                video.getAvatarUrl(),
                content,
                String.valueOf(System.currentTimeMillis() / 1000),
                video.getLikesCount(),
                video.getCommentsCount(),
                video.getShareCount(),
                null,
                null,
                media,
                video.getType() != null ? video.getType() : "Video",
                null
        );
    }

    public static boolean toggleVideoSave(Context context, String userId, com.veganbeauty.app.data.local.entities.YtVideoEntity video) {
        CommunityPostEntity post = postFromVideo(video);
        if (post == null) return false;
        return toggleSave(context, userId, post);
    }

    public static boolean toggleVideoRepost(Context context, String userId, com.veganbeauty.app.data.local.entities.YtVideoEntity video) {
        CommunityPostEntity post = postFromVideo(video);
        if (post == null) return false;
        return toggleRepost(context, userId, post);
    }

    public static boolean isPostSaved(Context context, String userId, String postId) {
        return getSavedPostIds(context, userId).contains(postId);
    }

    /** Persist/update full post payload for an already-saved id (e.g. Rootie news). */
    public static void ensureSavedPostSnapshot(Context context, String userId, CommunityPostEntity post) {
        if (post == null || post.getPostId() == null || post.getPostId().isEmpty()) return;
        if (!isPostSaved(context, userId, post.getPostId())) return;
        try {
            JSONObject memory = getOrCreateLocalUserEntry(context, userId);
            JSONArray snapshots = memory.optJSONArray("savedPosts");
            CommunityPostEntity existing = null;
            if (snapshots != null) {
                for (int i = 0; i < snapshots.length(); i++) {
                    JSONObject obj = snapshots.optJSONObject(i);
                    if (obj != null && post.getPostId().equals(obj.optString("postId"))) {
                        existing = postFromJson(obj);
                        break;
                    }
                }
            }
            if (existing != null && postRichnessScore(existing) >= postRichnessScore(post)) {
                return;
            }
            upsertSavedPostSnapshot(memory, post);
            saveUserMemory(context, memory);
        } catch (Exception ignored) {
        }
    }

    /**
     * Returns saved posts with full snapshot data (persists across sessions).
     * Prefer this for displaying "Mục đã lưu" so Rootie news still has content.
     */
    public static List<CommunityPostEntity> getSavedPosts(Context context, String userId) {
        JSONObject memory = findUserEntry(context, userId);
        if (memory == null) {
            return Collections.emptyList();
        }

        List<CommunityPostEntity> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        JSONArray snapshots = memory.optJSONArray("savedPosts");
        if (snapshots != null) {
            for (int i = 0; i < snapshots.length(); i++) {
                CommunityPostEntity post = postFromJson(snapshots.optJSONObject(i));
                if (post != null && seen.add(post.getPostId())) {
                    result.add(post);
                }
            }
        }

        // Keep order aligned with savedPostIds when possible; append any id-only leftovers.
        JSONArray ids = memory.optJSONArray("savedPostIds");
        if (ids != null && ids.length() > 0) {
            List<CommunityPostEntity> ordered = new ArrayList<>();
            Set<String> orderedSeen = new HashSet<>();
            for (int i = 0; i < ids.length(); i++) {
                String id = ids.optString(i);
                if (id == null || id.isEmpty() || !orderedSeen.add(id)) continue;
                CommunityPostEntity match = null;
                for (CommunityPostEntity p : result) {
                    if (id.equals(p.getPostId())) {
                        match = p;
                        break;
                    }
                }
                if (match != null) {
                    ordered.add(match);
                }
            }
            for (CommunityPostEntity p : result) {
                if (!orderedSeen.contains(p.getPostId())) {
                    ordered.add(p);
                    orderedSeen.add(p.getPostId());
                }
            }
            return ordered;
        }

        return result;
    }

    /**
     * Merge live feed posts with saved snapshots so Rootie news (not in Room) still appears.
     */
    public static List<CommunityPostEntity> resolveSavedPosts(
            Context context,
            String userId,
            List<CommunityPostEntity> availablePosts
    ) {
        return resolveMemoryPosts(context, userId, availablePosts, "savedPostIds", getSavedPosts(context, userId));
    }

    private static List<CommunityPostEntity> resolveMemoryPosts(
            Context context,
            String userId,
            List<CommunityPostEntity> availablePosts,
            String idsKey,
            List<CommunityPostEntity> snapshots
    ) {
        JSONObject memory = findUserEntry(context, userId);
        if (memory == null) {
            return Collections.emptyList();
        }
        Set<String> targetIds = toPostIdSet(memory, idsKey);
        if (targetIds.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashMap<String, CommunityPostEntity> merged = new LinkedHashMap<>();
        if (availablePosts != null) {
            for (CommunityPostEntity post : availablePosts) {
                if (post == null || post.getPostId() == null || !targetIds.contains(post.getPostId())) {
                    continue;
                }
                String id = post.getPostId();
                merged.put(id, pickRicherPost(post, merged.get(id)));
            }
        }
        if (snapshots != null) {
            for (CommunityPostEntity snapshot : snapshots) {
                if (snapshot == null || snapshot.getPostId() == null || !targetIds.contains(snapshot.getPostId())) {
                    continue;
                }
                String id = snapshot.getPostId();
                merged.put(id, pickRicherPost(snapshot, merged.get(id)));
            }
        }

        List<CommunityPostEntity> ordered = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        JSONArray ids = memory.optJSONArray(idsKey);
        if (ids != null) {
            for (int i = 0; i < ids.length(); i++) {
                String id = ids.optString(i);
                if (id == null || id.isEmpty() || !seen.add(id)) {
                    continue;
                }
                CommunityPostEntity post = merged.get(id);
                if (post != null) {
                    ordered.add(post);
                }
            }
        }
        for (Map.Entry<String, CommunityPostEntity> entry : merged.entrySet()) {
            if (seen.add(entry.getKey())) {
                ordered.add(entry.getValue());
            }
        }
        return ordered;
    }

    /** Backfill saved snapshots from live news list (fixes older saves that only stored ids). */
    public static void repairSavedNewsSnapshots(Context context, String userId, List<CommunityPostEntity> newsPosts) {
        if (context == null || userId == null || userId.trim().isEmpty() || newsPosts == null || newsPosts.isEmpty()) {
            return;
        }
        Set<String> savedIds = getSavedPostIds(context, userId);
        if (savedIds.isEmpty()) {
            return;
        }
        try {
            JSONObject memory = findUserEntry(context, userId);
            if (memory == null) {
                memory = getOrCreateLocalUserEntry(context, userId);
            }
            boolean changed = false;
            for (CommunityPostEntity news : newsPosts) {
                if (news == null || news.getPostId() == null || !savedIds.contains(news.getPostId())) {
                    continue;
                }
                JSONArray snapshots = memory.optJSONArray("savedPosts");
                CommunityPostEntity existing = null;
                if (snapshots != null) {
                    for (int i = 0; i < snapshots.length(); i++) {
                        JSONObject obj = snapshots.optJSONObject(i);
                        if (obj != null && news.getPostId().equals(obj.optString("postId"))) {
                            existing = postFromJson(obj);
                            break;
                        }
                    }
                }
                if (existing == null || postRichnessScore(news) > postRichnessScore(existing)) {
                    upsertSavedPostSnapshot(memory, news);
                    changed = true;
                }
            }
            if (changed) {
                saveUserMemory(context, memory);
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean toggleSave(Context context, String userId, String postId) {
        return toggleSave(context, userId, postId, null);
    }

    public static boolean toggleSave(Context context, String userId, CommunityPostEntity post) {
        if (post == null || post.getPostId() == null || post.getPostId().isEmpty()) {
            return false;
        }
        return toggleSave(context, userId, post.getPostId(), post);
    }

    private static boolean toggleSave(Context context, String userId, String postId, CommunityPostEntity post) {
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
                removeSavedPostSnapshot(memory, postId);
                isNowSaved = false;
            } else {
                arr.put(postId);
                if (post != null) {
                    upsertSavedPostSnapshot(memory, post);
                }
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
