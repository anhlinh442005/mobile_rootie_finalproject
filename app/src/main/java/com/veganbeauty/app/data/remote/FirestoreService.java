package com.veganbeauty.app.data.remote;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.data.local.entities.KeyIngredient;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.entities.UserMemoryEntity;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FirestoreService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Gson gson = new Gson();

    public org.json.JSONArray getSkinHistory(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return new org.json.JSONArray();
        }
        try {
            // Because the json data uses userId like 'xuannk_001' or userEmail, we query by userId/email.
            // Here we check both or just 'userEmail' for simplicity.
            Task<QuerySnapshot> task = db.collection("skin_history")
                    .whereEqualTo("userId", userEmail)
                    .get();
            QuerySnapshot snapshot = Tasks.await(task);
            
            if (snapshot.isEmpty()) {
                // Also try matching by email if userId wasn't used
                task = db.collection("skin_history")
                        .whereEqualTo("email", userEmail)
                        .get();
                snapshot = Tasks.await(task);
            }

            List<DocumentSnapshot> docs = snapshot.getDocuments();
            // Sort by date/time descending manually
            docs.sort((d1, d2) -> {
                String dt1 = d1.getString("date") + " " + d1.getString("time");
                String dt2 = d2.getString("date") + " " + d2.getString("time");
                return dt2.compareTo(dt1);
            });

            org.json.JSONArray array = new org.json.JSONArray();
            for (DocumentSnapshot doc : docs) {
                array.put(new org.json.JSONObject(doc.getData()));
            }
            return array;
        } catch (Exception e) {
            e.printStackTrace();
            return new org.json.JSONArray();
        }
    }

    public void addSkinHistory(String email, org.json.JSONObject data) {
        try {
            String id = data.optString("id", UUID.randomUUID().toString());
            data.put("userId", email); // use email as identifier
            Map<String, Object> map = new HashMap<>();
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, data.get(key));
            }
            Tasks.await(db.collection("skin_history").document(id).set(map));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ProductEntity> fetchAllProducts() {
        try {
            Task<QuerySnapshot> task = db.collection("products").get();
            QuerySnapshot snapshot = Tasks.await(task);
            List<ProductEntity> products = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                ProductEntity product = mapProductDocument(doc);
                if (product != null) products.add(product);
            }
            return products;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public ProductEntity fetchProductByBarcode(String barcode) {
        try {
            Task<QuerySnapshot> task = db.collection("products")
                    .whereEqualTo("barcode", barcode.trim())
                    .limit(1)
                    .get();
            QuerySnapshot snapshot = Tasks.await(task);
            if (!snapshot.isEmpty()) {
                return mapProductDocument(snapshot.getDocuments().get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ProductEntity mapProductDocument(DocumentSnapshot doc) {
        List<String> albumList = (List<String>) doc.get("album");
        if (albumList == null) albumList = new ArrayList<>();

        List<Map<String, Object>> keyIngredientsRaw = (List<Map<String, Object>>) doc.get("keyIngredients");
        List<KeyIngredient> keyIngredientsList = new ArrayList<>();
        if (keyIngredientsRaw != null) {
            for (Map<String, Object> map : keyIngredientsRaw) {
                String name = map.containsKey("name") && map.get("name") instanceof String ? (String) map.get("name") : "";
                String desc = map.containsKey("description") && map.get("description") instanceof String ? (String) map.get("description") : "";
                keyIngredientsList.add(new KeyIngredient(name, desc));
            }
        }

        List<String> detailedList = (List<String>) doc.get("detailedIngredients");
        if (detailedList == null) detailedList = new ArrayList<>();

        List<String> idealList = (List<String>) doc.get("idealFor");
        if (idealList == null) idealList = new ArrayList<>();

        List<String> benefitsList = (List<String>) doc.get("benefits");
        if (benefitsList == null) benefitsList = new ArrayList<>();

        Object categoryIdRaw = doc.get("categoryId");
        String categoryIdsStr = "";
        if (categoryIdRaw instanceof List) {
            List<?> list = (List<?>) categoryIdRaw;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof String) {
                    sb.append(list.get(i));
                    if (i < list.size() - 1) sb.append(",");
                }
            }
            categoryIdsStr = sb.toString();
        } else if (categoryIdRaw instanceof String) {
            categoryIdsStr = (String) categoryIdRaw;
        }

        String productId = doc.getString("id");
        if (productId == null || productId.isEmpty()) {
            productId = doc.getId();
        }

        String mainImage = resolveProductImage(doc, albumList);

        return new ProductEntity(
                productId,
                doc.getString("name") != null ? doc.getString("name") : "",
                doc.getString("sku") != null ? doc.getString("sku") : "",
                doc.getString("barcode") != null ? doc.getString("barcode") : "",
                doc.getLong("price") != null ? doc.getLong("price") : 0L,
                doc.getLong("originalPrice"),
                doc.getString("category") != null ? doc.getString("category") : "",
                doc.getString("brand") != null ? doc.getString("brand") : "",
                doc.getLong("stock") != null ? doc.getLong("stock").intValue() : 0,
                doc.getString("description") != null ? doc.getString("description") : "",
                mainImage,
                doc.getString("suitableFor") != null ? doc.getString("suitableFor") : "",
                doc.getString("origin") != null ? doc.getString("origin") : "",
                doc.getString("expiryDate") != null ? doc.getString("expiryDate") : "",
                doc.getBoolean("isNew") != null ? doc.getBoolean("isNew") : false,
                categoryIdsStr,
                albumList,
                doc.getString("mainIngredientsSummary") != null ? doc.getString("mainIngredientsSummary") : "",
                doc.getString("allergyInformation") != null ? doc.getString("allergyInformation") : "",
                keyIngredientsList,
                detailedList,
                doc.getString("storyDescription") != null ? doc.getString("storyDescription") : "",
                doc.getString("storyImage") != null ? doc.getString("storyImage") : "",
                idealList,
                benefitsList,
                doc.getString("usage") != null ? doc.getString("usage") : "",
                doc.getString("usageAmount") != null ? doc.getString("usageAmount") : "",
                doc.getString("scent") != null ? doc.getString("scent") : "",
                doc.getString("notes") != null ? doc.getString("notes") : "",
                doc.getDouble("rating") != null ? doc.getDouble("rating").floatValue() : 0f,
                doc.getLong("sold") != null ? doc.getLong("sold").intValue() : 0
        );
    }

    private String resolveProductImage(DocumentSnapshot doc, List<String> albumList) {
        String mainImage = doc.getString("mainImage");
        if (mainImage == null || mainImage.isEmpty()) mainImage = doc.getString("image");
        if (mainImage == null || mainImage.isEmpty()) mainImage = doc.getString("imageUrl");
        if (mainImage == null || mainImage.isEmpty()) mainImage = doc.getString("image_url");
        if ((mainImage == null || mainImage.isEmpty()) && albumList != null && !albumList.isEmpty()) {
            mainImage = albumList.get(0);
        }
        return mainImage != null ? mainImage : "";
    }

    public List<UserEntity> fetchAllUsers() {
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("users").get());
            List<UserEntity> users = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String avatar = doc.getString("avatar");
                if (avatar == null) avatar = doc.getString("avatar_url");
                if (avatar == null) avatar = "";
                users.add(new UserEntity(
                        doc.getId(),
                        doc.getString("username") != null ? doc.getString("username") : "",
                        doc.getString("full_name") != null ? doc.getString("full_name") : "",
                        doc.getString("email") != null ? doc.getString("email") : "",
                        doc.getString("phone") != null ? doc.getString("phone") : "",
                        "",
                        avatar,
                        null
                ));
            }
            return users;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public UserEntity authenticateUser(String emailOrPhone, String passwordHash, String passwordPlain, boolean isEmail) {
        try {
            String field = isEmail ? "email" : "phone";
            QuerySnapshot snapshot = Tasks.await(db.collection("users")
                    .whereEqualTo(field, emailOrPhone)
                    .get());

            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String dbPassword = doc.getString("password") != null ? doc.getString("password") : "";
                if (dbPassword.equals(passwordHash) || dbPassword.equals(passwordPlain)) {
                    String avatar = doc.getString("avatar");
                    if (avatar == null) avatar = doc.getString("avatar_url");
                    if (avatar == null) avatar = "";
                    return new UserEntity(
                            doc.getId(),
                            doc.getString("username") != null ? doc.getString("username") : "",
                            doc.getString("full_name") != null ? doc.getString("full_name") : "",
                            doc.getString("email") != null ? doc.getString("email") : "",
                            doc.getString("phone") != null ? doc.getString("phone") : "",
                            dbPassword,
                            avatar,
                            null
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveUser(UserEntity user) {
        try {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("username", user.getUsername());
            userMap.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            userMap.put("email", user.getEmail());
            userMap.put("phone", user.getPhone());
            userMap.put("full_name", user.getFull_name());
            userMap.put("password", user.getPassword());
            if (user.getPrimary_image() != null && !user.getPrimary_image().trim().isEmpty()) {
                userMap.put("primary_image", user.getPrimary_image());
            }
            if (user.getBio() != null && !user.getBio().trim().isEmpty()) {
                userMap.put("bio", user.getBio());
            }
            Tasks.await(db.collection("users").document(user.getUser_id()).set(userMap, com.google.firebase.firestore.SetOptions.merge()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Chỉ cập nhật field avatar (merge), dùng sau khi upload Cloudinary thành công. */
    public boolean updateUserAvatar(String userId, String avatarUrl) {
        if (userId == null || userId.trim().isEmpty() || avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return false;
        }
        try {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("avatar", avatarUrl.trim());
            Tasks.await(db.collection("users").document(userId.trim()).set(userMap, com.google.firebase.firestore.SetOptions.merge()));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<CommunityPostEntity> fetchAllCommunityPosts() {
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("community_posts").get());
            List<CommunityPostEntity> posts = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Map<String, Object> authorMap = (Map<String, Object>) doc.get("author");
                List<Object> mediaRaw = (List<Object>) doc.get("media");
                StringBuilder mediaUrls = new StringBuilder();
                if (mediaRaw != null) {
                    for (int i = 0; i < mediaRaw.size(); i++) {
                        Object it = mediaRaw.get(i);
                        if (it instanceof String) {
                            mediaUrls.append((String) it);
                        } else if (it instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) it;
                            if (map.containsKey("url") && map.get("url") instanceof String) {
                                mediaUrls.append((String) map.get("url"));
                            }
                        }
                        if (i < mediaRaw.size() - 1) mediaUrls.append(",");
                    }
                }

                List<Object> productsRaw = (List<Object>) doc.get("linked_products");
                StringBuilder linkedProductIds = new StringBuilder();
                if (productsRaw != null) {
                    for (int i = 0; i < productsRaw.size(); i++) {
                        Object it = productsRaw.get(i);
                        if (it instanceof String) {
                            linkedProductIds.append((String) it);
                        } else if (it instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) it;
                            if (map.containsKey("product_id") && map.get("product_id") instanceof String) {
                                linkedProductIds.append((String) map.get("product_id"));
                            }
                        }
                        if (i < productsRaw.size() - 1) linkedProductIds.append(",");
                    }
                }

                Map<String, Object> reactionsMap = (Map<String, Object>) doc.get("reactions");
                int likesCount = 0;
                if (reactionsMap != null && reactionsMap.get("like") instanceof Number) {
                    likesCount = ((Number) reactionsMap.get("like")).intValue();
                }

                String avatarUrl = "";
                if (authorMap != null) {
                    if (authorMap.get("avatar_url") != null) {
                        avatarUrl = authorMap.get("avatar_url").toString();
                    } else if (authorMap.get("avatar") != null) {
                        avatarUrl = authorMap.get("avatar").toString();
                    }
                }

                posts.add(new CommunityPostEntity(
                        doc.getId(),
                        authorMap != null && authorMap.get("user_id") != null ? authorMap.get("user_id").toString() : "",
                        authorMap != null && authorMap.get("username") != null ? authorMap.get("username").toString() : "",
                        authorMap != null && authorMap.get("display_name") != null ? authorMap.get("display_name").toString() : "",
                        avatarUrl,
                        doc.getString("content") != null ? doc.getString("content") : "",
                        doc.getString("created_at") != null ? doc.getString("created_at") : "",
                        likesCount,
                        doc.getLong("comments_count") != null ? doc.getLong("comments_count").intValue() : 0,
                        doc.getLong("reups_count") != null ? doc.getLong("reups_count").intValue() : 0,
                        doc.getString("skin_type") != null ? doc.getString("skin_type") : "",
                        doc.getString("concern") != null ? doc.getString("concern") : "",
                        mediaUrls.toString(),
                        doc.getString("type") != null ? doc.getString("type") : "",
                        linkedProductIds.toString()
                ));
            }
            return posts;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<ReelEntity> fetchAllReels() {
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("community_reels_fb").get());
            List<ReelEntity> reels = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Map<String, Object> authorMap = (Map<String, Object>) doc.get("author");
                Map<String, Object> statsMap = (Map<String, Object>) doc.get("stats");
                Map<String, Object> videoMap = (Map<String, Object>) doc.get("video");

                int shareCount = 0;
                if (statsMap != null && statsMap.get("shares") instanceof Number) {
                    shareCount = ((Number) statsMap.get("shares")).intValue();
                }

                String avatarUrl = "";
                if (authorMap != null) {
                    if (authorMap.get("avatar_url") != null) {
                        avatarUrl = authorMap.get("avatar_url").toString();
                    } else if (authorMap.get("avatar") != null) {
                        avatarUrl = authorMap.get("avatar").toString();
                    }
                }

                String thumbnailUrl = "";
                if (videoMap != null && videoMap.get("thumbnail") != null) {
                    thumbnailUrl = videoMap.get("thumbnail").toString();
                } else if (doc.getString("thumbnail_url") != null) {
                    thumbnailUrl = doc.getString("thumbnail_url");
                }

                reels.add(new ReelEntity(
                        doc.getId(),
                        doc.getString("caption") != null ? doc.getString("caption") : "",
                        authorMap != null && authorMap.get("user_id") != null ? authorMap.get("user_id").toString() : "",
                        authorMap != null && authorMap.get("username") != null ? authorMap.get("username").toString() : "",
                        authorMap != null && authorMap.get("display_name") != null ? authorMap.get("display_name").toString() : "",
                        avatarUrl,
                        doc.getLong("likes_count") != null ? doc.getLong("likes_count").intValue() : 0,
                        doc.getLong("comments_count") != null ? doc.getLong("comments_count").intValue() : 0,
                        shareCount,
                        thumbnailUrl
                ));
            }
            return reels;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<YtVideoEntity> fetchAllExploreVideos() {
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("community_video_yt").get());
            List<YtVideoEntity> videos = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                List<String> typeRaw = (List<String>) doc.get("Type");
                String typeStr = "";
                if (typeRaw != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < typeRaw.size(); i++) {
                        sb.append(typeRaw.get(i));
                        if (i < typeRaw.size() - 1) sb.append(",");
                    }
                    typeStr = sb.toString();
                }

                String desc = doc.getString("short_description");
                if (desc == null) desc = doc.getString("title");
                if (desc == null) desc = "";

                videos.add(new YtVideoEntity(
                        doc.getId(),
                        doc.getString("title") != null ? doc.getString("title") : "",
                        doc.getString("url") != null ? doc.getString("url") : "",
                        desc,
                        doc.getString("username") != null ? doc.getString("username") : "",
                        doc.getString("avatar") != null ? doc.getString("avatar") : "",
                        typeStr
                ));
            }
            return videos;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean uploadAllExploreVideos(List<YtVideoEntity> videos) {
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("community_video_yt").get());
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Tasks.await(db.collection("community_video_yt").document(doc.getId()).delete());
            }

            for (YtVideoEntity video : videos) {
                Map<String, Object> data = new HashMap<>();
                data.put("title", video.getTitle());
                data.put("url", video.getUrl());
                data.put("short_description", video.getDescription());
                data.put("username", video.getUsername());
                data.put("avatar", video.getAvatarUrl());
                data.put("Type", java.util.Arrays.asList(video.getType().split(",")));
                Tasks.await(db.collection("community_video_yt").document(video.getId()).set(data));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean uploadAllCommunityMessages(String messagesJson) {
        return syncCommunityMessagesFromJson(messagesJson, true);
    }

    public boolean syncCommunityMessagesFromJson(String messagesJson, boolean wipeFirst) {
        try {
            if (wipeFirst) {
                QuerySnapshot snapshot = Tasks.await(db.collection("community_message").get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Tasks.await(db.collection("community_message").document(doc.getId()).delete());
                }
            }

            Type type = new TypeToken<List<ConversationEntity>>() {}.getType();
            List<ConversationEntity> conversations = gson.fromJson(messagesJson, type);
            if (conversations == null || conversations.isEmpty()) {
                return false;
            }

            for (ConversationEntity conversation : conversations) {
                if (conversation == null || conversation.getId() == null || conversation.getId().isEmpty()) {
                    continue;
                }
                JsonElement jsonTree = gson.toJsonTree(conversation);
                Map<String, Object> map = gson.fromJson(jsonTree, Map.class);
                Tasks.await(db.collection("community_message").document(conversation.getId()).set(map));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<IngredientEntity> fetchAllIngredients() {
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("ingredients").get());
            List<IngredientEntity> ingredients = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                String slug = doc.getString("slug");
                if (slug == null) slug = doc.getId();

                String scientificName = doc.getString("scientificName");
                if (scientificName == null) scientificName = doc.getString("scientific_name");
                if (scientificName == null) scientificName = "";

                String image = doc.getString("image");
                if (image == null) image = doc.getString("imageUrl");
                if (image == null) image = doc.getString("image_url");
                if (image == null) image = "";

                List<String> uses = (List<String>) doc.get("uses");
                StringBuilder usesStr = new StringBuilder();
                if (uses != null) {
                    for (int i = 0; i < uses.size(); i++) {
                        usesStr.append(uses.get(i));
                        if (i < uses.size() - 1) usesStr.append(",");
                    }
                }

                ingredients.add(new IngredientEntity(
                        slug,
                        doc.getString("name") != null ? doc.getString("name") : "",
                        scientificName,
                        image,
                        doc.getString("origin") != null ? doc.getString("origin") : "",
                        doc.getString("description") != null ? doc.getString("description") : "",
                        usesStr.toString(),
                        new ArrayList<>()
                ));
            }
            return ingredients;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean uploadCommunityPost(CommunityPostEntity post) {
        try {
            Map<String, Object> authorMap = new HashMap<>();
            authorMap.put("user_id", post.getAuthorId());
            authorMap.put("username", post.getAuthorUsername());
            authorMap.put("display_name", post.getAuthorDisplayName());
            authorMap.put("avatar", post.getAuthorAvatarUrl());

            List<String> mediaList = new ArrayList<>();
            if (post.getMediaUrlsString() != null && !post.getMediaUrlsString().isEmpty()) {
                for (String s : post.getMediaUrlsString().split(",")) {
                    if (!s.trim().isEmpty()) mediaList.add(s.trim());
                }
            }

            Map<String, Object> reactionsMap = new HashMap<>();
            reactionsMap.put("like", post.getLikesCount());

            List<String> linkedProductsList = new ArrayList<>();
            if (post.getLinkedProductIds() != null && !post.getLinkedProductIds().isEmpty()) {
                for (String s : post.getLinkedProductIds().split(",")) {
                    if (!s.trim().isEmpty()) linkedProductsList.add(s.trim());
                }
            }

            Map<String, Object> postMap = new HashMap<>();
            postMap.put("author", authorMap);
            postMap.put("content", post.getContent());
            postMap.put("media", mediaList);
            postMap.put("created_at", post.getCreatedAt());
            postMap.put("reactions", reactionsMap);
            postMap.put("comments_count", post.getCommentsCount());
            postMap.put("skin_type", post.getSkinType());
            postMap.put("concern", post.getConcern());
            postMap.put("type", post.getType());
            postMap.put("linked_products", linkedProductsList);

            Tasks.await(db.collection("community_posts").document(post.getPostId()).set(postMap));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean uploadUserMemory(UserMemoryEntity memory) {
        try {
            Map<String, Object> memoryMap = new HashMap<>();
            memoryMap.put("actionType", memory.getActionType());
            memoryMap.put("targetUserId", memory.getTargetUserId());
            memoryMap.put("targetUsername", memory.getTargetUsername());
            memoryMap.put("targetAvatar", memory.getTargetAvatar());
            memoryMap.put("content", memory.getContent());
            memoryMap.put("timestamp", memory.getTimestamp());
            Tasks.await(db.collection("user_memory").document(memory.getId()).set(memoryMap));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addCommentToPost(String postId, Map<String, Object> commentMap) {
        try {
            Tasks.await(db.collection("community_posts").document(postId)
                    .update(
                            "comments", FieldValue.arrayUnion(commentMap),
                            "comments_count", FieldValue.increment(1)
                    ));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean forceSyncLocalPostsToFirebase(String postsJson, String newsJson) {
        try {
            wipeCollection("community_posts");

            JSONArray postsArray = new JSONObject(postsJson).getJSONArray("posts");
            for (int i = 0; i < postsArray.length(); i++) {
                JSONObject obj = postsArray.getJSONObject(i);
                Map<String, Object> map = jsonObjectToMap(obj);
                String id = obj.optString("post_id", UUID.randomUUID().toString());
                Tasks.await(db.collection("community_posts").document(id).set(map));
            }

            JSONArray newsArray = new JSONArray(newsJson);
            for (int i = 0; i < newsArray.length(); i++) {
                JSONObject obj = newsArray.getJSONObject(i);
                Map<String, Object> map = jsonObjectToMap(obj);
                String id = obj.optString("post_id", UUID.randomUUID().toString());
                Tasks.await(db.collection("community_posts").document(id).set(map));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean forceSyncCollection(String collectionName, String jsonStr, String idKey, String arrayKey) {
        try {
            wipeCollection(collectionName);

            JSONArray jsonArray;
            if (arrayKey != null) {
                jsonArray = new JSONObject(jsonStr).getJSONArray(arrayKey);
            } else {
                jsonArray = new JSONArray(jsonStr);
            }

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Map<String, Object> map = jsonObjectToMap(obj);
                String id = obj.optString(idKey);
                if (id == null || id.trim().isEmpty()) {
                    id = UUID.randomUUID().toString();
                }
                Tasks.await(db.collection(collectionName).document(id).set(map));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void wipeCollection(String collectionName) {
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection(collectionName).get());
            WriteBatch batch = db.batch();
            int count = 0;
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                batch.delete(doc.getReference());
                count++;
                if (count >= 400) {
                    Tasks.await(batch.commit());
                    batch = db.batch();
                    count = 0;
                }
            }
            if (count > 0) {
                Tasks.await(batch.commit());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean forceSyncProductsFromJson(String jsonStr) {
        try {
            wipeCollection("products");
            JSONObject root = new JSONObject(jsonStr);
            JSONArray arr = root.getJSONArray("products");
            WriteBatch batch = db.batch();
            int count = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String id = obj.optString("id", "");
                if (id.isEmpty()) continue;
                batch.set(db.collection("products").document(id), jsonObjectToMap(obj));
                count++;
                if (count >= 400) {
                    Tasks.await(batch.commit());
                    batch = db.batch();
                    count = 0;
                }
            }
            if (count > 0) {
                Tasks.await(batch.commit());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, Object> jsonObjectToMap(JSONObject obj) {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object value = obj.get(key);
                if (value instanceof JSONArray) {
                    map.put(key, jsonArrayToList((JSONArray) value));
                } else if (value instanceof JSONObject) {
                    map.put(key, jsonObjectToMap((JSONObject) value));
                } else if (JSONObject.NULL.equals(value)) {
                    map.put(key, "");
                } else {
                    map.put(key, value);
                }
            } catch (Exception e) {
                map.put(key, "");
            }
        }
        return map;
    }

    private List<Object> jsonArrayToList(JSONArray arr) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            try {
                Object value = arr.get(i);
                if (value instanceof JSONArray) {
                    list.add(jsonArrayToList((JSONArray) value));
                } else if (value instanceof JSONObject) {
                    list.add(jsonObjectToMap((JSONObject) value));
                } else if (JSONObject.NULL.equals(value)) {
                    list.add("");
                } else {
                    list.add(value);
                }
            } catch (Exception e) {
                list.add("");
            }
        }
        return list;
    }

    public void clearOldAffiliateData() {
        String[] collections = {
                "affiliate_orders", "affiliate", "affiliates", "affiliate_wallets",
                "affiliate_products", "showcase_products", "user_showcases",
                "attached_products", "cart_affiliate", "affiliate_dashboard"
        };
        for (String colName : collections) {
            try {
                QuerySnapshot snapshot = Tasks.await(db.collection(colName).get());
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Tasks.await(db.collection(colName).document(doc.getId()).delete());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void syncOrders(List<OrderEntity> orders) {
        try {
            wipeCollection("orders");
            for (OrderEntity order : orders) {
                Tasks.await(db.collection("orders").document(order.getId()).set(order));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<StoreEntity> fetchAllStores() {
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("stores").get());
            List<StoreEntity> stores = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Map<String, Object> diaChiMap = (Map<String, Object>) doc.get("dia_chi");
                Map<String, Object> toaDoMap = (Map<String, Object>) doc.get("toa_do");
                Map<String, Object> lienHeMap = (Map<String, Object>) doc.get("thong_tin_lien_he");
                Map<String, Object> hoatDongMap = (Map<String, Object>) doc.get("thoi_gian_hoat_dong");
                Map<String, Object> thu26Map = hoatDongMap != null ? (Map<String, Object>) hoatDongMap.get("thu_2_6") : null;

                String moCua = thu26Map != null && thu26Map.get("mo_cua") instanceof String ? (String) thu26Map.get("mo_cua") : "07:00";
                String dongCua = thu26Map != null && thu26Map.get("dong_cua") instanceof String ? (String) thu26Map.get("dong_cua") : "21:00";

                List<?> phoneList = lienHeMap != null ? (List<?>) lienHeMap.get("so_dien_thoai") : null;
                StringBuilder phoneStr = new StringBuilder();
                if (phoneList != null) {
                    for (int i = 0; i < phoneList.size(); i++) {
                        if (phoneList.get(i) instanceof String) {
                            phoneStr.append(phoneList.get(i));
                            if (i < phoneList.size() - 1) phoneStr.append(", ");
                        }
                    }
                }

                List<?> tienNghiList = (List<?>) doc.get("tien_nghi");
                StringBuilder tienNghiStr = new StringBuilder();
                if (tienNghiList != null) {
                    for (int i = 0; i < tienNghiList.size(); i++) {
                        if (tienNghiList.get(i) instanceof String) {
                            tienNghiStr.append(tienNghiList.get(i));
                            if (i < tienNghiList.size() - 1) tienNghiStr.append(",");
                        }
                    }
                }

                String diaChiDayDu = diaChiMap != null && diaChiMap.get("dia_chi_day_du") instanceof String
                        ? (String) diaChiMap.get("dia_chi_day_du")
                        : doc.getString("dia_chi_day_du");
                if (diaChiDayDu == null) diaChiDayDu = "";

                boolean isActive = true;
                if (doc.getBoolean("isActive") != null) isActive = doc.getBoolean("isActive");
                else if (doc.getBoolean("is_active") != null) isActive = doc.getBoolean("is_active");

                stores.add(new StoreEntity(
                        doc.getId(),
                        doc.getString("ma_cua_hang") != null ? doc.getString("ma_cua_hang") : "",
                        doc.getString("ten_cua_hang") != null ? doc.getString("ten_cua_hang") : "",
                        doc.getString("loai_hinh") != null ? doc.getString("loai_hinh") : "",
                        diaChiMap != null && diaChiMap.get("so_nha") instanceof String ? (String) diaChiMap.get("so_nha") : "",
                        diaChiMap != null && diaChiMap.get("duong") instanceof String ? (String) diaChiMap.get("duong") : "",
                        diaChiMap != null && diaChiMap.get("phuong_xa") instanceof String ? (String) diaChiMap.get("phuong_xa") : "",
                        diaChiMap != null && diaChiMap.get("quan_huyen") instanceof String ? (String) diaChiMap.get("quan_huyen") : "",
                        diaChiMap != null && diaChiMap.get("tinh_thanh") instanceof String ? (String) diaChiMap.get("tinh_thanh") : "",
                        diaChiDayDu,
                        toaDoMap != null && toaDoMap.get("lat") instanceof Number ? ((Number) toaDoMap.get("lat")).doubleValue() : 0.0,
                        toaDoMap != null && toaDoMap.get("lng") instanceof Number ? ((Number) toaDoMap.get("lng")).doubleValue() : 0.0,
                        phoneStr.toString(),
                        lienHeMap != null && lienHeMap.get("email") instanceof String ? (String) lienHeMap.get("email") : "",
                        moCua,
                        dongCua,
                        doc.getString("trang_thai") != null ? doc.getString("trang_thai") : "Đang hoạt động",
                        isActive,
                        tienNghiStr.toString(),
                        "",
                        0.0
                ));
            }
            return stores;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // --- Bookings ---
    public void uploadBookings(List<BookingHistoryEntity> bookings) {
        try {
            WriteBatch batch = db.batch();
            for (BookingHistoryEntity booking : bookings) {
                String id = booking.getId();
                if (id == null || id.isEmpty()) id = UUID.randomUUID().toString();
                Map<String, Object> map = bookingToMap(booking);
                batch.set(db.collection("bookings").document(id), map);
            }
            Tasks.await(batch.commit());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> bookingToMap(BookingHistoryEntity booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("userId", booking.getUserId());
        map.put("userName", booking.getUserName());
        map.put("userPhone", booking.getUserPhone());
        map.put("userEmail", booking.getUserEmail());
        map.put("email", booking.getUserEmail());
        map.put("serviceName", booking.getServiceName());
        map.put("dateDisplay", booking.getDateDisplay());
        map.put("monthDisplay", booking.getMonthDisplay());
        map.put("dayOfWeek", booking.getDayOfWeek());
        map.put("time", booking.getTime());
        map.put("duration", booking.getDuration());
        map.put("storeName", booking.getStoreName());
        map.put("storeAddress", booking.getStoreAddress());
        map.put("storePhone", booking.getStorePhone());
        map.put("storeImage", booking.getStoreImage());
        map.put("note", booking.getNote());
        map.put("status", booking.getStatus());
        map.put("policy", booking.getPolicy());
        map.put("createdAt", booking.getCreatedAt());
        map.put("completedAt", booking.getCompletedAt());
        map.put("skinResults", booking.getSkinResults());
        map.put("consultantName", booking.getConsultantName());
        map.put("consultantAvatar", booking.getConsultantAvatar());
        map.put("consultantRating", booking.getConsultantRating());
        map.put("userRating", booking.getUserRating());
        map.put("userReview", booking.getUserReview());
        map.put("reviewDate", booking.getReviewDate());
        map.put("beforeImage", booking.getBeforeImage());
        map.put("afterImage", booking.getAfterImage());
        map.put("earnedPoints", booking.getEarnedPoints());
        map.put("totalPoints", booking.getTotalPoints());
        map.put("nextAppointmentDate", booking.getNextAppointmentDate());
        map.put("nextAppointmentText", booking.getNextAppointmentText());
        map.put("cancelledAt", booking.getCancelledAt());
        map.put("cancelReason", booking.getCancelReason());
        return map;
    }

    public boolean uploadBooking(BookingHistoryEntity booking) {
        try {
            String docId = booking.getId();
            if (docId == null || docId.isEmpty()) {
                docId = UUID.randomUUID().toString();
            }

            Map<String, Object> bookingMap = bookingToMap(booking);
            bookingMap.put("email", booking.getUserEmail());

            String storeID = resolveStoreId(booking);
            bookingMap.put("storeID", storeID);
            bookingMap.put("storeId", storeID);

            Tasks.await(db.collection("bookings").document(docId).set(bookingMap));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean uploadBookingMap(String docId, Map<String, Object> fields) {
        try {
            if (docId == null || docId.trim().isEmpty()) {
                return false;
            }
            Tasks.await(db.collection("bookings").document(docId).set(fields));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String resolveStoreId(BookingHistoryEntity booking) {
        String nameLower = booking.getStoreName() != null ? booking.getStoreName().toLowerCase(Locale.ROOT) : "";
        String addrLower = booking.getStoreAddress() != null ? booking.getStoreAddress().toLowerCase(Locale.ROOT) : "";
        if (nameLower.contains("cơ sở 1") || addrLower.contains("minh khai")) {
            return "CH001";
        }
        if (nameLower.contains("cơ sở 5") || addrLower.contains("hoàng văn thụ")) {
            return "CH005";
        }
        return "";
    }

    public boolean updateBookingStatus(String bookingId, String newStatus, String cancelReason) {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", newStatus);
            if (cancelReason != null && !cancelReason.isEmpty()) updates.put("cancelReason", cancelReason);
            if ("Đã huỷ".equals(newStatus)) {
                updates.put("cancelledAt", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
            }

            Tasks.await(db.collection("bookings").document(bookingId).update(updates));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateBookingReview(String bookingId, float rating, String reviewText, String reviewDate) {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("userRating", rating);
            updates.put("userReview", reviewText);
            updates.put("reviewDate", reviewDate);
            Tasks.await(db.collection("bookings").document(bookingId).update(updates));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<BookingHistoryEntity> getUserBookingHistory(String userIdOrEmail) {
        if (userIdOrEmail == null || userIdOrEmail.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String key = userIdOrEmail.trim();
        if (key.contains("@")) {
            return fetchBookingsForUser(key);
        }
        List<BookingHistoryEntity> byUserId = fetchBookingsForUserByUserId(key);
        if (!byUserId.isEmpty()) {
            return byUserId;
        }
        return fetchBookingsForUser(key);
    }

    public List<BookingHistoryEntity> fetchBookingsForUser(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            String normalizedEmail = email.trim();
            QuerySnapshot snapshot = Tasks.await(
                    db.collection("bookings").whereEqualTo("userEmail", normalizedEmail).get());
            if (snapshot.isEmpty()) {
                snapshot = Tasks.await(
                        db.collection("bookings").whereEqualTo("email", normalizedEmail).get());
            }

            List<BookingHistoryEntity> bookings = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                BookingHistoryEntity entity = mapBookingDocument(doc);
                if (entity != null) {
                    bookings.add(entity);
                }
            }
            return bookings;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<BookingHistoryEntity> fetchBookingsForUserByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            QuerySnapshot snapshot = Tasks.await(
                    db.collection("bookings").whereEqualTo("userId", userId.trim()).get());
            List<BookingHistoryEntity> bookings = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                BookingHistoryEntity entity = mapBookingDocument(doc);
                if (entity != null) {
                    bookings.add(entity);
                }
            }
            return bookings;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private BookingHistoryEntity mapBookingDocument(DocumentSnapshot doc) {
        try {
            String userEmail = doc.getString("userEmail");
            if (userEmail == null || userEmail.isEmpty()) {
                userEmail = doc.getString("email") != null ? doc.getString("email") : "";
            }

            List<String> skinResults = new ArrayList<>();
            List<?> rawSkinResults = (List<?>) doc.get("skinResults");
            if (rawSkinResults != null) {
                for (Object item : rawSkinResults) {
                    if (item != null) skinResults.add(item.toString());
                }
            }

            String docId = doc.getString("id");
            if (docId == null || docId.isEmpty()) {
                docId = doc.getId();
            }

            return new BookingHistoryEntity(
                    docId,
                    doc.getString("userId") != null ? doc.getString("userId") : "",
                    doc.getString("userName") != null ? doc.getString("userName") : "",
                    doc.getString("userPhone") != null ? doc.getString("userPhone") : "",
                    userEmail,
                    doc.getString("serviceName") != null ? doc.getString("serviceName") : "",
                    doc.getString("dateDisplay") != null ? doc.getString("dateDisplay") : "",
                    doc.getString("monthDisplay") != null ? doc.getString("monthDisplay") : "",
                    doc.getString("dayOfWeek") != null ? doc.getString("dayOfWeek") : "",
                    doc.getString("time") != null ? doc.getString("time") : "",
                    doc.getString("duration") != null ? doc.getString("duration") : "",
                    doc.getString("storeName") != null ? doc.getString("storeName") : "",
                    doc.getString("storeAddress") != null ? doc.getString("storeAddress") : "",
                    doc.getString("storePhone") != null ? doc.getString("storePhone") : "",
                    doc.getString("storeImage") != null ? doc.getString("storeImage") : "",
                    doc.getString("note") != null ? doc.getString("note") : "",
                    doc.getString("status") != null ? doc.getString("status") : "",
                    doc.getString("policy") != null ? doc.getString("policy") : "",
                    doc.getString("createdAt") != null ? doc.getString("createdAt") : "",
                    doc.getString("completedAt") != null ? doc.getString("completedAt") : "",
                    skinResults,
                    doc.getString("consultantName") != null ? doc.getString("consultantName") : "",
                    doc.getString("consultantAvatar") != null ? doc.getString("consultantAvatar") : "",
                    doc.getDouble("consultantRating") != null ? doc.getDouble("consultantRating").floatValue() : 0f,
                    doc.getDouble("userRating") != null ? doc.getDouble("userRating").floatValue() : 0f,
                    doc.getString("userReview") != null ? doc.getString("userReview") : "",
                    doc.getString("reviewDate") != null ? doc.getString("reviewDate") : "",
                    doc.getString("beforeImage") != null ? doc.getString("beforeImage") : "",
                    doc.getString("afterImage") != null ? doc.getString("afterImage") : "",
                    doc.getLong("earnedPoints") != null ? doc.getLong("earnedPoints").intValue() : 0,
                    doc.getLong("totalPoints") != null ? doc.getLong("totalPoints").intValue() : 0,
                    doc.getString("nextAppointmentDate") != null ? doc.getString("nextAppointmentDate") : "",
                    doc.getString("nextAppointmentText") != null ? doc.getString("nextAppointmentText") : "",
                    doc.getString("cancelledAt") != null ? doc.getString("cancelledAt") : "",
                    doc.getString("cancelReason") != null ? doc.getString("cancelReason") : ""
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean addBooking(BookingHistoryEntity booking) {
        return uploadBooking(booking);
    }

    // --- User social (friends / following / followers) ---

    public static final String COL_USER_SOCIAL = "user_social";

    public boolean syncUserSocialFromJson(String jsonStr) {
        return forceSyncCollection(COL_USER_SOCIAL, jsonStr, "user_id", null);
    }

    public String fetchAllUserSocialAsJson() {
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection(COL_USER_SOCIAL).get());
            JSONArray array = new JSONArray();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                JSONObject obj = documentToSocialJson(doc);
                if (obj != null) {
                    array.put(obj);
                }
            }
            return array.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, List<String>> fetchUserSocial(String userId) {
        Map<String, List<String>> result = newEmptySocialMap();
        if (userId == null || userId.trim().isEmpty()) {
            return result;
        }
        try {
            DocumentSnapshot doc = Tasks.await(db.collection(COL_USER_SOCIAL).document(userId.trim()).get());
            if (!doc.exists()) {
                return result;
            }
            return mapSocialDocument(doc);
        } catch (Exception e) {
            e.printStackTrace();
            return result;
        }
    }

    public boolean saveUserSocial(String userId, Map<String, List<String>> socialData) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("user_id", userId.trim());
            map.put("friends", socialData.get("friends") != null ? socialData.get("friends") : new ArrayList<>());
            map.put("following", socialData.get("following") != null ? socialData.get("following") : new ArrayList<>());
            map.put("followers", socialData.get("followers") != null ? socialData.get("followers") : new ArrayList<>());
            map.put("friend_requests", socialData.get("friend_requests") != null ? socialData.get("friend_requests") : new ArrayList<>());
            map.put("suggested", socialData.get("suggested") != null ? socialData.get("suggested") : new ArrayList<>());
            Tasks.await(db.collection(COL_USER_SOCIAL).document(userId.trim()).set(map));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean applyFollowChange(String actorUserId, String targetUserId, boolean follow) {
        if (actorUserId == null || targetUserId == null
                || actorUserId.trim().isEmpty() || targetUserId.trim().isEmpty()) {
            return false;
        }
        try {
            String actorId = actorUserId.trim();
            String targetId = targetUserId.trim();

            Map<String, List<String>> actorSocial = fetchUserSocial(actorId);
            Map<String, List<String>> targetSocial = fetchUserSocial(targetId);

            List<String> actorFollowing = new ArrayList<>(actorSocial.get("following"));
            List<String> targetFollowers = new ArrayList<>(targetSocial.get("followers"));

            if (follow) {
                if (!actorFollowing.contains(targetId)) {
                    actorFollowing.add(targetId);
                }
                if (!targetFollowers.contains(actorId)) {
                    targetFollowers.add(actorId);
                }
            } else {
                actorFollowing.remove(targetId);
                targetFollowers.remove(actorId);
            }

            actorSocial.put("following", actorFollowing);
            targetSocial.put("followers", targetFollowers);

            boolean actorSaved = saveUserSocial(actorId, actorSocial);
            boolean targetSaved = saveUserSocial(targetId, targetSocial);
            return actorSaved && targetSaved;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, List<String>> newEmptySocialMap() {
        Map<String, List<String>> result = new HashMap<>();
        result.put("friends", new ArrayList<>());
        result.put("following", new ArrayList<>());
        result.put("followers", new ArrayList<>());
        result.put("friend_requests", new ArrayList<>());
        result.put("suggested", new ArrayList<>());
        return result;
    }

    private Map<String, List<String>> mapSocialDocument(DocumentSnapshot doc) {
        Map<String, List<String>> result = newEmptySocialMap();
        result.put("friends", toStringList(doc.get("friends")));
        result.put("following", toStringList(doc.get("following")));
        result.put("followers", toStringList(doc.get("followers")));
        result.put("friend_requests", toStringList(doc.get("friend_requests")));
        result.put("suggested", toStringList(doc.get("suggested")));
        return result;
    }

    private JSONObject documentToSocialJson(DocumentSnapshot doc) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("user_id", doc.getString("user_id") != null ? doc.getString("user_id") : doc.getId());
            obj.put("friends", new JSONArray(toStringList(doc.get("friends"))));
            obj.put("following", new JSONArray(toStringList(doc.get("following"))));
            obj.put("followers", new JSONArray(toStringList(doc.get("followers"))));
            obj.put("friend_requests", new JSONArray(toStringList(doc.get("friend_requests"))));
            obj.put("suggested", new JSONArray(toStringList(doc.get("suggested"))));
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> toStringList(Object raw) {
        List<String> list = new ArrayList<>();
        if (raw instanceof List<?>) {
            for (Object item : (List<?>) raw) {
                if (item != null) {
                    list.add(item.toString());
                }
            }
        }
        return list;
    }
}
