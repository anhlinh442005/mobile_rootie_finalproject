package com.veganbeauty.app.data.local;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;

import com.veganbeauty.app.data.local.entities.AffiliateInfo;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.CommunityProduct;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.data.local.entities.KeyIngredient;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderItem;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalJsonReader {

    private final Context context;

    public LocalJsonReader(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    private String readAssetFile(String fileName) throws Exception {
        InputStream is = context.getAssets().open(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString().replace("\uFEFF", "");
    }

    private String readFile(File file) throws Exception {
        BufferedReader reader = new BufferedReader(new java.io.FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    private void writeFile(File file, String content) throws Exception {
        java.io.FileWriter writer = new java.io.FileWriter(file);
        writer.write(content);
        writer.close();
    }

    public Map<String, List<String>> getSocialDataForUser(String userId) {
        Map<String, List<String>> result = new HashMap<>();
        result.put("friends", new ArrayList<>());
        result.put("following", new ArrayList<>());
        result.put("followers", new ArrayList<>());
        result.put("friend_requests", new ArrayList<>());
        result.put("suggested", new ArrayList<>());

        try {
            String jsonString = readAssetFile("User_com_friend.json");
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (userId.equals(obj.optString("user_id"))) {
                    String[] keys = {"friends", "following", "followers", "friend_requests", "suggested"};
                    for (String key : keys) {
                        if (obj.has(key)) {
                            JSONArray jsonArray = obj.getJSONArray(key);
                            List<String> list = new ArrayList<>();
                            for (int j = 0; j < jsonArray.length(); j++) {
                                list.add(jsonArray.getString(j));
                            }
                            result.put(key, list);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public Map<String, List<String>> getMutualFriendsForUsers(Set<String> myFriends, List<String> targetUserIds) {
        Map<String, List<String>> result = new HashMap<>();
        try {
            String jsonString = readAssetFile("User_com_friend.json");
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String uid = obj.optString("user_id");
                if (targetUserIds.contains(uid)) {
                    JSONArray friendsArray = obj.optJSONArray("friends");
                    List<String> mutuals = new ArrayList<>();
                    if (friendsArray != null) {
                        for (int j = 0; j < friendsArray.length(); j++) {
                            String fid = friendsArray.getString(j);
                            if (myFriends.contains(fid)) {
                                mutuals.add(fid);
                            }
                        }
                    }
                    result.put(uid, mutuals);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<String> getFriendsForUser(String userId) {
        try {
            String jsonString = readAssetFile("User_com_friend.json");
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (userId.equals(obj.optString("user_id"))) {
                    JSONArray friendsArray = obj.optJSONArray("friends");
                    List<String> list = new ArrayList<>();
                    if (friendsArray != null) {
                        for (int j = 0; j < friendsArray.length(); j++) {
                            list.add(friendsArray.getString(j));
                        }
                    }
                    return list;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<String> getShowcaseProductsForUser(String userId) {
        Set<String> list = new HashSet<>();

        // 1. Get from affiliate_product.json
        try {
            File file = new File(context.getFilesDir(), "affiliate_product_local.json");
            JSONObject root;
            if (file.exists()) {
                root = new JSONObject(readFile(file));
            } else {
                root = new JSONObject(readAssetFile("affiliate_product.json"));
            }
            JSONArray arr = root.optJSONArray("affiliate_products");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    if (userId.equals(obj.optString("userId"))) {
                        JSONArray productsArr = obj.optJSONArray("products");
                        if (productsArr != null) {
                            for (int j = 0; j < productsArr.length(); j++) {
                                JSONObject p = productsArr.getJSONObject(j);
                                if (p.optBoolean("affiliate_display", true)) {
                                    list.add(p.optString("productId"));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Get from orders where referrerUserId == userId
        try {
            List<OrderEntity> allOrders = getAllOrders();
            for (OrderEntity order : allOrders) {
                if (order.isAffiliate() && order.getAffiliate() != null && userId.equals(order.getAffiliate().getReferrerUserId())) {
                    for (OrderItem item : order.getItems()) {
                        list.add(item.getProductId());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Get from community posts where authorId == userId and has linked products
        try {
            List<CommunityPostEntity> allPosts = getCommunityPosts();
            for (CommunityPostEntity post : allPosts) {
                if (userId.equals(post.getAuthorId())) {
                    String ids = post.getLinkedProductIds();
                    if (ids != null && !ids.isEmpty()) {
                        String[] parts = ids.split(",");
                        for (String part : parts) {
                            if (!part.trim().isEmpty()) {
                                list.add(part.trim());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>(list);
    }

    public List<ProductEntity> getAllProducts() {
        try {
            String jsonString = readAssetFile("products.json");
            JSONObject root = new JSONObject(jsonString);
            JSONArray jsonArray = root.getJSONArray("products");
            List<ProductEntity> productList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Object categoryIdRaw = obj.opt("categoryId");
                String categoryIdsStr = "";
                if (categoryIdRaw instanceof JSONArray) {
                    JSONArray arr = (JSONArray) categoryIdRaw;
                    List<String> list = new ArrayList<>();
                    for (int j = 0; j < arr.length(); j++) {
                        list.add(arr.getString(j));
                    }
                    categoryIdsStr = android.text.TextUtils.join(",", list);
                } else if (categoryIdRaw instanceof String) {
                    categoryIdsStr = (String) categoryIdRaw;
                }

                JSONArray albumArr = obj.optJSONArray("album");
                List<String> albumList = new ArrayList<>();
                if (albumArr != null) {
                    for (int j = 0; j < albumArr.length(); j++) {
                        albumList.add(albumArr.getString(j));
                    }
                }

                JSONArray keyArr = obj.optJSONArray("keyIngredients");
                List<KeyIngredient> keyIngredientsList = new ArrayList<>();
                if (keyArr != null) {
                    for (int j = 0; j < keyArr.length(); j++) {
                        JSONObject keyObj = keyArr.getJSONObject(j);
                        keyIngredientsList.add(new KeyIngredient(
                                keyObj.optString("name", ""),
                                keyObj.optString("description", "")
                        ));
                    }
                }

                JSONArray detailedArr = obj.optJSONArray("detailedIngredients");
                List<String> detailedIngredientsList = new ArrayList<>();
                if (detailedArr != null) {
                    for (int j = 0; j < detailedArr.length(); j++) {
                        detailedIngredientsList.add(detailedArr.getString(j));
                    }
                }

                JSONArray idealArr = obj.optJSONArray("idealFor");
                List<String> idealForList = new ArrayList<>();
                if (idealArr != null) {
                    for (int j = 0; j < idealArr.length(); j++) {
                        idealForList.add(idealArr.getString(j));
                    }
                }

                JSONArray benefitsArr = obj.optJSONArray("benefits");
                List<String> benefitsList = new ArrayList<>();
                if (benefitsArr != null) {
                    for (int j = 0; j < benefitsArr.length(); j++) {
                        benefitsList.add(benefitsArr.getString(j));
                    }
                }

                productList.add(new ProductEntity(
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("sku"),
                        obj.optString("barcode", ""),
                        obj.getLong("price"),
                        obj.has("originalPrice") ? obj.getLong("originalPrice") : null,
                        obj.getString("category"),
                        categoryIdsStr,
                        obj.optString("brand", ""),
                        obj.getInt("stock"),
                        obj.optString("description", ""),
                        obj.optString("mainImage", ""),
                        obj.optString("suitableFor", ""),
                        obj.optString("origin", ""),
                        obj.optString("expiryDate", ""),
                        obj.optBoolean("newProduct", false) || obj.optBoolean("isNew", false),
                        albumList,
                        obj.optString("mainIngredientsSummary", ""),
                        obj.optString("allergyInformation", ""),
                        keyIngredientsList,
                        detailedIngredientsList,
                        obj.optString("storyDescription", ""),
                        obj.optString("storyImage", ""),
                        idealForList,
                        benefitsList,
                        obj.optString("usage", ""),
                        obj.optString("usageAmount", ""),
                        obj.optString("scent", ""),
                        obj.optString("notes", ""),
                        (float) obj.optDouble("rating", 0.0),
                        obj.optInt("sold", 0)
                ));
            }
            return productList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<CommunityProduct> getProducts() {
        List<ProductEntity> all = getAllProducts();
        List<CommunityProduct> list = new ArrayList<>();
        for (ProductEntity p : all) {
            list.add(new CommunityProduct(
                    p.getId(),
                    p.getName(),
                    p.getMainImage(),
                    (int) p.getPrice(),
                    p.getOriginalPrice() != null ? p.getOriginalPrice().intValue() : null,
                    p.getRating(),
                    p.getSold()
            ));
        }
        return list;
    }

    public List<UserEntity> getUsers() {
        try {
            String jsonString = readAssetFile("users.json");
            JSONArray jsonArray = new JSONArray(jsonString);
            List<UserEntity> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String avatar = obj.optString("avatar", null);
                if (avatar != null && avatar.trim().isEmpty()) {
                    avatar = null;
                }
                if (avatar == null) {
                    avatar = "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png";
                }
                list.add(new UserEntity(
                        obj.optString("user_id", obj.optString("id", UUID.randomUUID().toString())),
                        obj.optString("username", ""),
                        obj.optString("full_name", ""),
                        obj.optString("email", ""),
                        obj.optString("phone", ""),
                        obj.optString("password", ""),
                        avatar,
                        obj.optString("primary_image", null)
                ));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<CommunityPostEntity> getCommunityPosts() {
        try {
            String jsonString = readAssetFile("community_posts.json");
            Map<String, UserEntity> usersMap = new HashMap<>();
            for (UserEntity u : getUsers()) {
                usersMap.put(u.getUser_id(), u);
            }
            List<CommunityPostEntity> assetPosts = new ArrayList<>(parsePosts(jsonString, usersMap));

            File localFile = new File(context.getFilesDir(), "local_posts.json");
            if (localFile.exists()) {
                String localJsonString = readFile(localFile);
                List<CommunityPostEntity> localPosts = parsePosts(localJsonString, usersMap);
                assetPosts.addAll(0, localPosts);
            }

            return assetPosts;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<CommunityPostEntity> getCommunityNews() {
        try {
            String jsonString = readAssetFile("community_news.json");
            Map<String, UserEntity> usersMap = new HashMap<>();
            for (UserEntity u : getUsers()) {
                usersMap.put(u.getUser_id(), u);
            }
            return parsePosts(jsonString, usersMap);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<CommunityPostEntity> parsePosts(String jsonString, Map<String, UserEntity> usersMap) {
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(jsonString);
        } catch (Exception e) {
            try {
                JSONObject root = new JSONObject(jsonString);
                jsonArray = root.getJSONArray("posts");
            } catch (Exception ex) {
                return new ArrayList<>();
            }
        }

        List<CommunityPostEntity> postList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject obj = jsonArray.getJSONObject(i);
                JSONObject authorObj = obj.optJSONObject("author");
                JSONObject reactionsObj = obj.optJSONObject("reactions");

                JSONArray mediaArr = obj.optJSONArray("media");
                List<String> mediaUrls = new ArrayList<>();
                if (mediaArr != null) {
                    for (int j = 0; j < mediaArr.length(); j++) {
                        JSONObject mediaObj = mediaArr.getJSONObject(j);
                        mediaUrls.add(mediaObj.getString("url"));
                    }
                }

                JSONArray linkedProductsArr = obj.optJSONArray("linked_products");
                List<String> linkedProducts = new ArrayList<>();
                if (linkedProductsArr != null) {
                    for (int j = 0; j < linkedProductsArr.length(); j++) {
                        linkedProducts.add(linkedProductsArr.getString(j));
                    }
                }

                String contentStr = obj.has("content") ? obj.getString("content") : obj.optString("message", "");

                String mediaUrlsStr = "";
                if (!mediaUrls.isEmpty()) {
                    mediaUrlsStr = android.text.TextUtils.join(",", mediaUrls);
                } else {
                    JSONObject imageObj = obj.optJSONObject("image");
                    if (imageObj != null) {
                        mediaUrlsStr = imageObj.optString("uri", "");
                    } else {
                        JSONArray imageArr = obj.optJSONArray("image");
                        if (imageArr != null) {
                            List<String> arrUrls = new ArrayList<>();
                            for (int j = 0; j < imageArr.length(); j++) {
                                JSONObject imgItem = imageArr.getJSONObject(j);
                                String uri = imgItem.optString("uri", "");
                                if (!uri.isEmpty()) arrUrls.add(uri);
                            }
                            mediaUrlsStr = android.text.TextUtils.join(",", arrUrls);
                        }
                    }
                }

                int timestampInt = obj.optInt("timestamp", 0);
                String createdAtStr = timestampInt > 0 ? String.valueOf(timestampInt) : obj.optString("created_at", "");

                int reupsCount = obj.optInt("reups_count", 0);

                String authorId = "";
                if (authorObj != null) {
                    authorId = authorObj.optString("user_id", authorObj.optString("id", ""));
                }

                UserEntity realUser = usersMap.get(authorId);

                String authorUsername = (realUser != null) ? realUser.getUsername() : (authorObj != null ? authorObj.optString("username") : "");
                String authorDisplayName = "";
                if (realUser != null && realUser.getFull_name() != null && !realUser.getFull_name().trim().isEmpty()) {
                    authorDisplayName = realUser.getFull_name();
                } else if (realUser != null && realUser.getUsername() != null) {
                    authorDisplayName = realUser.getUsername();
                } else if (authorObj != null) {
                    authorDisplayName = authorObj.optString("display_name", authorObj.optString("username", ""));
                }

                String authorAvatarUrl;
                if ("rootie_official".equals(authorId) || "rootie_vn".equals(authorId)) {
                    authorAvatarUrl = "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1781257994/favicon_r7kqwf.png";
                } else {
                    String avatarFromPost = null;
                    if (authorObj != null) {
                        avatarFromPost = authorObj.optString("avatar_url", "");
                        if (avatarFromPost.isEmpty()) {
                            avatarFromPost = authorObj.optString("profile_picture_url", "");
                        }
                        if (avatarFromPost.isEmpty()) avatarFromPost = null;
                    }
                    if (avatarFromPost != null) {
                        authorAvatarUrl = avatarFromPost;
                    } else if (realUser != null && realUser.getAvatar() != null && !realUser.getAvatar().isEmpty()) {
                        authorAvatarUrl = realUser.getAvatar();
                    } else {
                        authorAvatarUrl = "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png";
                    }
                }

                int likesCount = obj.optInt("reactions_count", 0);
                if (reactionsObj != null) {
                    likesCount = reactionsObj.optInt("like", likesCount);
                }

                int commentsCount = obj.optInt("comments_count", 0);
                JSONArray commentsArr = obj.optJSONArray("comments");
                if (commentsArr != null) {
                    commentsCount = commentsArr.length();
                }

                postList.add(new CommunityPostEntity(
                        obj.optString("post_id", UUID.randomUUID().toString()),
                        authorId,
                        authorUsername,
                        authorDisplayName,
                        authorAvatarUrl,
                        contentStr,
                        createdAtStr,
                        likesCount,
                        commentsCount,
                        reupsCount,
                        obj.optString("skin_type", null),
                        obj.optString("concern", null),
                        mediaUrlsStr,
                        obj.optString("type", null),
                        android.text.TextUtils.join(",", linkedProducts)
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return postList;
    }

    public List<ReelEntity> getReels() {
        try {
            String jsonString = readAssetFile("community_reels_fb.json");
            Map<String, UserEntity> usersMap = new HashMap<>();
            for (UserEntity u : getUsers()) {
                usersMap.put(u.getUser_id(), u);
            }
            JSONArray jsonArray = new JSONArray(jsonString);
            List<ReelEntity> reelList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    JSONObject authorObj = obj.optJSONObject("author");
                    JSONObject statsObj = obj.optJSONObject("stats");
                    JSONObject videoObj = obj.optJSONObject("video");

                    String authorId = authorObj != null ? authorObj.optString("user_id") : "";
                    UserEntity realUser = usersMap.get(authorId);
                    String authorUsername = (realUser != null && realUser.getUsername() != null) ? realUser.getUsername() : (authorObj != null ? authorObj.optString("username") : "");
                    String authorDisplayName = (realUser != null && realUser.getFull_name() != null && !realUser.getFull_name().trim().isEmpty()) ? realUser.getFull_name() : (realUser != null ? realUser.getUsername() : (authorObj != null ? authorObj.optString("display_name") : ""));

                    String authorAvatarUrl = "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png";
                    if (realUser != null && realUser.getAvatar() != null && !realUser.getAvatar().trim().isEmpty()) {
                        authorAvatarUrl = realUser.getAvatar();
                    } else if (authorObj != null) {
                        String temp = authorObj.optString("avatar_url", authorObj.optString("avatar"));
                        if (temp != null && !temp.trim().isEmpty()) {
                            authorAvatarUrl = temp;
                        }
                    }

                    reelList.add(new ReelEntity(
                            obj.optString("video_id", UUID.randomUUID().toString()),
                            obj.optString("caption", ""),
                            authorId,
                            authorUsername,
                            authorDisplayName,
                            authorAvatarUrl,
                            statsObj != null ? statsObj.optInt("likes") : obj.optInt("likes_count", 0),
                            statsObj != null ? statsObj.optInt("comments") : obj.optInt("comment_count", 0),
                            statsObj != null ? statsObj.optInt("shares") : obj.optInt("share_count", 0),
                            videoObj != null ? videoObj.optString("thumbnail") : obj.optString("thumbnail_url", "")
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return reelList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<YtVideoEntity> getExploreVideos() {
        try {
            String jsonString = readAssetFile("community_video_yt.json");
            JSONArray jsonArray = new JSONArray(jsonString);
            List<YtVideoEntity> videoList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    JSONArray typeArray = obj.optJSONArray("Type");
                    List<String> types = new ArrayList<>();
                    if (typeArray != null) {
                        for (int j = 0; j < typeArray.length(); j++) {
                            types.add(typeArray.getString(j));
                        }
                    }
                    YtVideoEntity entity = new YtVideoEntity(
                            obj.optString("_id", UUID.randomUUID().toString()),
                            obj.optString("title", ""),
                            obj.optString("url", ""),
                            obj.optString("long_description", obj.optString("short_description", obj.optString("title", ""))),
                            obj.optString("username", "Unknown User"),
                            obj.optString("avatar", null),
                            android.text.TextUtils.join(",", types),
                            obj.optInt("likes", 100 + (int)(Math.random() * 4900))
                    );

                    JSONArray hashArr = obj.optJSONArray("hashtags");
                    List<String> hashList = new ArrayList<>();
                    if (hashArr != null) {
                        for (int j = 0; j < hashArr.length(); j++) {
                            hashList.add(hashArr.getString(j));
                        }
                    }
                    entity.setHashtags(android.text.TextUtils.join(" ", hashList));

                    JSONArray kwArr = obj.optJSONArray("keywords");
                    List<String> kwList = new ArrayList<>();
                    if (kwArr != null) {
                        for (int j = 0; j < kwArr.length(); j++) {
                            kwList.add("#" + kwArr.getString(j).replace(" ", ""));
                        }
                    }
                    entity.setKeywords(android.text.TextUtils.join(" ", kwList));

                    videoList.add(entity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return videoList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<NotificationItem> getAllNotifications() {
        try {
            String jsonString = readAssetFile("notification_account.json");
            JSONArray jsonArray = new JSONArray(jsonString);
            List<NotificationItem> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                list.add(new NotificationItem(
                        obj.getString("id"),
                        obj.getString("title"),
                        obj.getString("content"),
                        obj.getString("time"),
                        obj.getString("category"),
                        obj.optString("tag").isEmpty() ? null : obj.optString("tag"),
                        obj.optString("voucherCode").isEmpty() ? null : obj.optString("voucherCode"),
                        obj.optString("actionText").isEmpty() ? null : obj.optString("actionText"),
                        obj.optBoolean("isRead", false),
                        obj.getString("section"),
                        obj.getString("iconResName"),
                        obj.optString("notificationType").isEmpty() ? null : obj.optString("notificationType"),
                        obj.optString("orderId").isEmpty() ? null : obj.optString("orderId"),
                        obj.optString("scheduleId").isEmpty() ? null : obj.optString("scheduleId")
                ));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<OrderEntity> getAllOrders() {
        try {
            String jsonString = readAssetFile("orders.json");
            JSONObject root = new JSONObject(jsonString);
            JSONArray jsonArray = root.getJSONArray("orders");
            List<OrderEntity> orderList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    JSONArray itemsArray = obj.getJSONArray("items");
                    List<OrderItem> orderItems = new ArrayList<>();
                    for (int j = 0; j < itemsArray.length(); j++) {
                        JSONObject itemObj = itemsArray.getJSONObject(j);
                        orderItems.add(new OrderItem(
                                itemObj.getString("productId"),
                                itemObj.getString("productName"),
                                itemObj.getString("productImage"),
                                itemObj.optInt("quantity", 1),
                                itemObj.optLong("price", 0L)
                        ));
                    }

                    boolean isAffiliate = obj.optBoolean("isAffiliate", false);
                    JSONObject affObj = obj.optJSONObject("affiliate");
                    AffiliateInfo affiliateInfo = null;
                    if (isAffiliate && affObj != null) {
                        affiliateInfo = new AffiliateInfo(
                                affObj.optBoolean("isAffiliateOrder", true),
                                affObj.optString("affiliate_id", ""),
                                affObj.optString("affiliateCode", ""),
                                affObj.optString("referrerUserId", ""),
                                affObj.optString("referrerName", ""),
                                affObj.optString("sourceType", "community_post"),
                                affObj.has("sourcePostId") ? affObj.optString("sourcePostId") : null,
                                affObj.optDouble("commissionRate", 0.0),
                                affObj.optLong("commissionAmount", 0L),
                                affObj.optString("commissionStatus", "pending")
                        );
                    }

                    orderList.add(new OrderEntity(
                            obj.getString("id"),
                            obj.optString("userId", "").isEmpty() ? null : obj.optString("userId"),
                            obj.getString("orderDate"),
                            obj.getString("orderTime"),
                            obj.getString("status"),
                            obj.getLong("totalAmount"),
                            obj.optLong("subTotal", obj.getLong("totalAmount")),
                            orderItems,
                            obj.optBoolean("isGuest", false),
                            obj.optString("shippingName", "Nguyễn Văn A"),
                            obj.optString("shippingPhone", "090 123 4567"),
                            obj.optString("shippingAddress", "123 Đường Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh"),
                            obj.optLong("shippingCost", 30000L),
                            obj.optLong("voucherDiscount", 0L),
                            obj.optString("paymentMethod", "Thanh toán qua Ví MoMo"),
                            obj.has("expectedDeliveryTime") ? obj.optString("expectedDeliveryTime") : null,
                            obj.has("deliveryDate") ? obj.optString("deliveryDate") : null,
                            isAffiliate,
                            affiliateInfo,
                            obj.optBoolean("hasReview", false),
                            obj.optInt("reviewStars", 0),
                            obj.has("reviewText") ? obj.optString("reviewText") : null,
                            obj.has("reviewImage") ? obj.optString("reviewImage") : null,
                            obj.optBoolean("isAnonymous", false),
                            obj.optBoolean("recommendToFriends", false),
                            obj.optString("billingName", "").isEmpty() ? null : obj.optString("billingName"),
                            obj.optString("billingPhone", "").isEmpty() ? null : obj.optString("billingPhone"),
                            obj.optString("billingEmail", "").isEmpty() ? null : obj.optString("billingEmail")
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return orderList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<IngredientEntity> getIngredients() {
        try {
            String jsonString = readAssetFile("ingredient.json");
            JSONArray jsonArray = new JSONArray(jsonString);
            List<IngredientEntity> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                JSONArray typesArr = obj.optJSONArray("types");
                List<String> typesList = new ArrayList<>();
                if (typesArr != null) {
                    for (int j = 0; j < typesArr.length(); j++) {
                        typesList.add(typesArr.getString(j));
                    }
                }
                list.add(new IngredientEntity(
                        obj.optString("slug", UUID.randomUUID().toString()),
                        obj.optString("name", ""),
                        obj.optString("scientific_name", ""),
                        obj.optString("image", ""),
                        obj.optString("origin", ""),
                        obj.optString("description", ""),
                        obj.optString("uses", ""),
                        typesList
                ));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<CommunityBlogEntity> getCommunityBlogs(int limit, int offset) {
        List<CommunityBlogEntity> list = new ArrayList<>();
        try {
            InputStream inputStream = context.getAssets().open("community_blog.json");
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            reader.beginArray();
            int skipped = 0;
            while (reader.hasNext() && list.size() < limit) {
                if (skipped < offset) {
                    reader.skipValue();
                    skipped++;
                    continue;
                }

                String id = "";
                String title = "";
                String shortDesc = "";
                String imageUrl = "";
                String publishedAt = "";

                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    switch (name) {
                        case "_id":
                            id = reader.nextString();
                            break;
                        case "title":
                            title = reader.nextString();
                            break;
                        case "shortDescription":
                            shortDesc = reader.nextString();
                            break;
                        case "publishedAt":
                            publishedAt = reader.nextString();
                            break;
                        case "primaryImage":
                            if (reader.peek() == JsonToken.NULL) {
                                reader.nextNull();
                            } else {
                                reader.beginObject();
                                while (reader.hasNext()) {
                                    if ("url".equals(reader.nextName())) {
                                        if (reader.peek() == JsonToken.NULL) {
                                            reader.nextNull();
                                        } else {
                                            imageUrl = reader.nextString();
                                        }
                                    } else {
                                        reader.skipValue();
                                    }
                                }
                                reader.endObject();
                            }
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }
                reader.endObject();
                if (id.isEmpty()) id = UUID.randomUUID().toString();
                list.add(new CommunityBlogEntity(id, title, shortDesc, imageUrl, publishedAt));
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public String getMessagesData() {
        try {
            File file = new File(context.getFilesDir(), "community_message.json");
            android.content.SharedPreferences prefs = context.getSharedPreferences("RootiePrefs", Context.MODE_PRIVATE);
            boolean isRefreshed = prefs.getBoolean("is_msg_refreshed_v2", false);
            if (!file.exists() || !isRefreshed) {
                String data = readAssetFile("community_message.json");
                writeFile(file, data);
                prefs.edit().putBoolean("is_msg_refreshed_v2", true).apply();
            }
            return readFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    public void saveMessagesData(String jsonString) {
        try {
            File file = new File(context.getFilesDir(), "community_message.json");
            writeFile(file, jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getRawPostsJson() {
        try {
            String assetStr = readAssetFile("community_posts.json");
            JSONObject assetRoot;
            try {
                assetRoot = new JSONObject(assetStr);
            } catch (Exception e) {
                JSONArray array = new JSONArray(assetStr);
                assetRoot = new JSONObject();
                assetRoot.put("posts", array);
            }
            JSONArray assetPostsArray = assetRoot.optJSONArray("posts");
            if (assetPostsArray == null) assetPostsArray = new JSONArray();

            File localFile = new File(context.getFilesDir(), "local_posts.json");
            if (localFile.exists()) {
                JSONArray localArray = new JSONArray(readFile(localFile));
                JSONArray combinedArray = new JSONArray();
                for (int i = 0; i < localArray.length(); i++) {
                    combinedArray.put(localArray.getJSONObject(i));
                }
                for (int i = 0; i < assetPostsArray.length(); i++) {
                    combinedArray.put(assetPostsArray.getJSONObject(i));
                }
                assetRoot.put("posts", combinedArray);
            }
            return assetRoot.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    public void saveLocalPost(CommunityPostEntity post) {
        try {
            File file = new File(context.getFilesDir(), "local_posts.json");
            JSONArray postsArray = new JSONArray();
            if (file.exists()) {
                postsArray = new JSONArray(readFile(file));
            }

            JSONObject postObj = new JSONObject();
            postObj.put("post_id", post.getPostId());
            postObj.put("content", post.getContent());
            postObj.put("created_at", post.getCreatedAt());
            postObj.put("reactions_count", post.getLikesCount());
            postObj.put("comments_count", post.getCommentsCount());
            postObj.put("reups_count", post.getReupsCount());
            postObj.put("skin_type", post.getSkinType() != null ? post.getSkinType() : "");
            postObj.put("concern", post.getConcern() != null ? post.getConcern() : "");
            postObj.put("type", post.getType() != null ? post.getType() : "");

            JSONObject authorObj = new JSONObject();
            authorObj.put("user_id", post.getAuthorId());
            authorObj.put("username", post.getAuthorUsername());
            authorObj.put("display_name", post.getAuthorDisplayName());
            authorObj.put("avatar_url", post.getAuthorAvatarUrl() != null ? post.getAuthorAvatarUrl() : "");
            postObj.put("author", authorObj);

            if (post.getMediaUrlsString() != null && !post.getMediaUrlsString().isEmpty()) {
                JSONArray mediaArray = new JSONArray();
                for (String url : post.getMediaUrlsString().split(",")) {
                    JSONObject mediaObj = new JSONObject();
                    mediaObj.put("url", url);
                    mediaArray.put(mediaObj);
                }
                postObj.put("media", mediaArray);
            }

            if (post.getLinkedProductIds() != null && !post.getLinkedProductIds().isEmpty()) {
                JSONArray linkedArray = new JSONArray();
                for (String id : post.getLinkedProductIds().split(",")) {
                    linkedArray.put(id);
                }
                postObj.put("linked_products", linkedArray);
            }

            JSONArray newArray = new JSONArray();
            newArray.put(postObj);
            for (int i = 0; i < postsArray.length(); i++) {
                newArray.put(postsArray.getJSONObject(i));
            }

            writeFile(file, newArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getRawNewsJson() {
        try { return readAssetFile("community_news.json"); } catch (Exception e) { return "[]"; }
    }
    public String getRawUsersJson() {
        try { return readAssetFile("users.json"); } catch (Exception e) { return "[]"; }
    }
    public String getRawReelsJson() {
        try { return readAssetFile("community_reels_fb.json"); } catch (Exception e) { return "[]"; }
    }
    public String getRawIngredientsJson() {
        try { return readAssetFile("ingredient.json"); } catch (Exception e) { return "[]"; }
    }
    public String getRawProductsJson() {
        try { return readAssetFile("products.json"); } catch (Exception e) { return "{}"; }
    }
    public String getRawSkinHistoryJson() {
        try { return readAssetFile("skin_history.json"); } catch (Exception e) { return "[]"; }
    }
    public String getRawSkinBookingsJson() {
        try { return readAssetFile("skin_bookings.json"); } catch (Exception e) { return "{}"; }
    }

    public List<StoreEntity> getStores() {
        return getAllStores();
    }

    public List<StoreEntity> getAllStores() {
        List<StoreEntity> list = new ArrayList<>();
        try {
            String jsonString = readAssetFile("rootie_stores.json");
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                JSONObject idObj = obj.optJSONObject("_id");
                String id = (idObj != null) ? idObj.optString("$oid", UUID.randomUUID().toString()) : UUID.randomUUID().toString();

                String storeCode = obj.optString("ma_cua_hang", "");
                String storeName = obj.optString("ten_cua_hang", "");
                String type = obj.optString("loai_hinh", "Cửa hàng mỹ phẩm");

                JSONObject addressObj = obj.optJSONObject("dia_chi");
                String address = addressObj != null ? addressObj.optString("dia_chi_day_du", "") : "";
                String province = addressObj != null ? addressObj.optString("tinh_thanh", "") : "";
                String district = addressObj != null ? addressObj.optString("quan_huyen", "") : "";
                String ward = addressObj != null ? addressObj.optString("phuong_xa", "") : "";
                String street = addressObj != null ? addressObj.optString("duong", "") : "";
                String number = addressObj != null ? addressObj.optString("so_nha", "") : "";

                JSONObject toaDoObj = obj.optJSONObject("toa_do");
                double lat = toaDoObj != null ? toaDoObj.optDouble("lat", 0.0) : 0.0;
                double lng = toaDoObj != null ? toaDoObj.optDouble("lng", 0.0) : 0.0;

                JSONObject contactObj = obj.optJSONObject("thong_tin_lien_he");
                String email = contactObj != null ? contactObj.optString("email", "") : "";
                JSONArray phoneArray = contactObj != null ? contactObj.optJSONArray("so_dien_thoai") : null;
                String phone = "";
                if (phoneArray != null && phoneArray.length() > 0) {
                    List<String> tempPhones = new ArrayList<>();
                    for (int j = 0; j < phoneArray.length(); j++) {
                        tempPhones.add(phoneArray.getString(j));
                    }
                    phone = android.text.TextUtils.join(",", tempPhones);
                } else if (contactObj != null) {
                    phone = contactObj.optString("so_dien_thoai", "");
                }

                JSONObject timeObj = obj.optJSONObject("thoi_gian_hoat_dong");
                JSONObject thu26Obj = timeObj != null ? timeObj.optJSONObject("thu_2_6") : null;
                String openHours = "08:00 - 22:00";
                if (thu26Obj != null) {
                    openHours = thu26Obj.optString("mo_cua", "08:00") + " - " + thu26Obj.optString("dong_cua", "22:00");
                }

                JSONArray imagesArray = obj.optJSONArray("hinh_anh");
                String imageUrl = "";
                if (imagesArray != null && imagesArray.length() > 0) {
                    imageUrl = imagesArray.getString(0);
                }

                JSONArray tienNghiArray = obj.optJSONArray("tien_nghi");
                String tienNghiStr = "";
                if (tienNghiArray != null) {
                    List<String> tempTienNghi = new ArrayList<>();
                    for (int j = 0; j < tienNghiArray.length(); j++) {
                        tempTienNghi.add(tienNghiArray.getString(j));
                    }
                    tienNghiStr = android.text.TextUtils.join(",", tempTienNghi);
                }

                int randomSeed = id.hashCode();
                double mockDistance = 1.0 + (Math.abs(randomSeed) % 140) / 10.0;

                String[] hoursParts = openHours.split(" - ");
                String moCua = hoursParts.length > 0 ? hoursParts[0] : "08:00";
                String dongCua = hoursParts.length > 1 ? hoursParts[1] : "22:00";

                list.add(new StoreEntity(
                        id, storeCode, storeName, type, number, street, ward, district, province, address, lat, lng, phone, email, moCua, dongCua, obj.optString("trang_thai", "Đang hoạt động"), obj.optBoolean("isActive", true), tienNghiStr, imageUrl, mockDistance
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static List<BookingHistoryEntity> cachedBookings = null;
    private static JSONArray cachedSkinHistory = null;

    public JSONArray getSkinHistory() {
        if (cachedSkinHistory == null) {
            try {
                File file = new File(context.getFilesDir(), "skin_history.json");
                String jsonString;
                if (file.exists()) {
                    jsonString = readFile(file);
                } else {
                    jsonString = readAssetFile("skin_history.json");
                }
                cachedSkinHistory = new JSONArray(jsonString);
            } catch (Exception e) {
                e.printStackTrace();
                cachedSkinHistory = new JSONArray();
            }
        }
        return cachedSkinHistory;
    }

    public void addSkinHistory(JSONObject item) {
        if (cachedSkinHistory == null) {
            getSkinHistory();
        }
        JSONArray newArray = new JSONArray();
        newArray.put(item);
        for (int i = 0; i < cachedSkinHistory.length(); i++) {
            try {
                newArray.put(cachedSkinHistory.getJSONObject(i));
            } catch (Exception e) { e.printStackTrace(); }
        }
        cachedSkinHistory = newArray;

        try {
            File file = new File(context.getFilesDir(), "skin_history.json");
            writeFile(file, newArray.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<BookingHistoryEntity> getUserBookingHistory(String email) {
        if (cachedBookings == null) {
            List<BookingHistoryEntity> list = new ArrayList<>();
            try {
                File file = new File(context.getFilesDir(), "local_bookings.json");
                String jsonString;
                if (file.exists()) {
                    jsonString = readFile(file);
                } else {
                    jsonString = readAssetFile("skin_bookings.json");
                }
                JSONObject root = new JSONObject(jsonString);
                JSONArray jsonArray = root.getJSONArray("bookings");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject histObj = jsonArray.getJSONObject(i);
                    String dateDisplay = histObj.optString("dateDisplay", "");
                    String time = histObj.optString("time", "");
                    String status = histObj.optString("status", "");
                    String cancelReason = histObj.optString("cancelReason", "");

                    if ("Sắp diễn ra".equals(status)) {
                        try {
                            Pattern regex = Pattern.compile("(\\d+)\\s+tháng\\s+(\\d+),\\s+(\\d+)");
                            Matcher match = regex.matcher(dateDisplay);
                            if (match.find()) {
                                int day = Integer.parseInt(match.group(1));
                                int month = Integer.parseInt(match.group(2));
                                int year = Integer.parseInt(match.group(3));

                                String[] timeParts = time.split(" - ");
                                String startTime = timeParts.length > 0 ? timeParts[0].trim() : "";
                                String[] hm = startTime.split(":");
                                int hour = hm.length > 0 ? Integer.parseInt(hm[0]) : 0;
                                int minute = hm.length > 1 ? Integer.parseInt(hm[1]) : 0;

                                Calendar bookingCal = Calendar.getInstance();
                                bookingCal.set(year, month - 1, day, hour, minute, 0);

                                if (bookingCal.getTime().before(new Date())) {
                                    status = "Đã huỷ";
                                    if (cancelReason.isEmpty()) {
                                        cancelReason = "Hệ thống tự động huỷ do đã quá hạn lịch hẹn.";
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    List<String> results = new ArrayList<>();
                    JSONArray resultsArray = histObj.optJSONArray("skinResults");
                    if (resultsArray != null) {
                        for (int j = 0; j < resultsArray.length(); j++) {
                            results.add(resultsArray.getString(j));
                        }
                    }

                    list.add(new BookingHistoryEntity(
                            histObj.optString("id", ""),
                            histObj.optString("userId", ""),
                            histObj.optString("userName", ""),
                            histObj.optString("userPhone", ""),
                            histObj.optString("userEmail", ""),
                            histObj.optString("serviceName", ""),
                            dateDisplay,
                            histObj.optString("monthDisplay", ""),
                            histObj.optString("dayOfWeek", ""),
                            time,
                            histObj.optString("duration", ""),
                            histObj.optString("storeName", ""),
                            histObj.optString("storeAddress", ""),
                            histObj.optString("storePhone", ""),
                            histObj.optString("storeImage", ""),
                            histObj.optString("note", ""),
                            status,
                            histObj.optString("policy", ""),
                            histObj.optString("createdAt", ""),
                            histObj.optString("completedAt", ""),
                            results,
                            histObj.optString("consultantName", ""),
                            histObj.optString("consultantAvatar", ""),
                            (float) histObj.optDouble("consultantRating", 0.0),
                            (float) histObj.optDouble("userRating", 0.0),
                            histObj.optString("userReview", ""),
                            histObj.optString("reviewDate", ""),
                            histObj.optString("beforeImage", ""),
                            histObj.optString("afterImage", ""),
                            histObj.optInt("earnedPoints", 0),
                            histObj.optInt("totalPoints", 0),
                            histObj.optString("nextAppointmentDate", ""),
                            histObj.optString("nextAppointmentText", ""),
                            histObj.optString("cancelledAt", ""),
                            cancelReason
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            cachedBookings = list;
        }
        List<BookingHistoryEntity> filtered = new ArrayList<>();
        for (BookingHistoryEntity b : cachedBookings) {
            if (email.equals(b.getUserEmail())) {
                filtered.add(b);
            }
        }
        return filtered;
    }

    private void saveBookingsToLocalFile(List<BookingHistoryEntity> bookings) {
        try {
            JSONObject root = new JSONObject();
            JSONArray array = new JSONArray();
            for (BookingHistoryEntity booking : bookings) {
                JSONObject obj = new JSONObject();
                obj.put("id", booking.getId());
                obj.put("userId", booking.getUserId());
                obj.put("userName", booking.getUserName());
                obj.put("userPhone", booking.getUserPhone());
                obj.put("userEmail", booking.getUserEmail());
                obj.put("serviceName", booking.getServiceName());
                obj.put("dateDisplay", booking.getDateDisplay());
                obj.put("monthDisplay", booking.getMonthDisplay());
                obj.put("dayOfWeek", booking.getDayOfWeek());
                obj.put("time", booking.getTime());
                obj.put("duration", booking.getDuration());
                obj.put("storeName", booking.getStoreName());
                obj.put("storeAddress", booking.getStoreAddress());
                obj.put("storePhone", booking.getStorePhone());
                obj.put("storeImage", booking.getStoreImage());
                obj.put("note", booking.getNote());
                obj.put("status", booking.getStatus());
                obj.put("policy", booking.getPolicy());
                obj.put("createdAt", booking.getCreatedAt());
                obj.put("completedAt", booking.getCompletedAt());

                JSONArray skinResultsArray = new JSONArray();
                for (String res : booking.getSkinResults()) {
                    skinResultsArray.put(res);
                }
                obj.put("skinResults", skinResultsArray);

                obj.put("consultantName", booking.getConsultantName());
                obj.put("consultantAvatar", booking.getConsultantAvatar());
                obj.put("consultantRating", booking.getConsultantRating());
                obj.put("userRating", booking.getUserRating());
                obj.put("userReview", booking.getUserReview());
                obj.put("reviewDate", booking.getReviewDate());
                obj.put("beforeImage", booking.getBeforeImage());
                obj.put("afterImage", booking.getAfterImage());
                obj.put("earnedPoints", booking.getEarnedPoints());
                obj.put("totalPoints", booking.getTotalPoints());
                obj.put("nextAppointmentDate", booking.getNextAppointmentDate());
                obj.put("nextAppointmentText", booking.getNextAppointmentText());
                obj.put("cancelledAt", booking.getCancelledAt());
                obj.put("cancelReason", booking.getCancelReason());
                array.put(obj);
            }
            root.put("bookings", array);
            File file = new File(context.getFilesDir(), "local_bookings.json");
            writeFile(file, root.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateBookingStatus(String bookingId, String newStatus, String cancelReason) {
        if (cachedBookings != null) {
            for (int i = 0; i < cachedBookings.size(); i++) {
                BookingHistoryEntity b = cachedBookings.get(i);
                if (b.getId().equals(bookingId)) {
                    cachedBookings.set(i, new BookingHistoryEntity(
                            b.getId(), b.getUserId(), b.getUserName(), b.getUserPhone(), b.getUserEmail(), b.getServiceName(),
                            b.getDateDisplay(), b.getMonthDisplay(), b.getDayOfWeek(), b.getTime(), b.getDuration(), b.getStoreName(),
                            b.getStoreAddress(), b.getStorePhone(), b.getStoreImage(), b.getNote(), newStatus, b.getPolicy(),
                            b.getCreatedAt(), b.getCompletedAt(), b.getSkinResults(), b.getConsultantName(), b.getConsultantAvatar(),
                            b.getConsultantRating(), b.getUserRating(), b.getUserReview(), b.getReviewDate(), b.getBeforeImage(),
                            b.getAfterImage(), b.getEarnedPoints(), b.getTotalPoints(), b.getNextAppointmentDate(), b.getNextAppointmentText(),
                            b.getCancelledAt(), cancelReason
                    ));
                    break;
                }
            }
            saveBookingsToLocalFile(cachedBookings);
        }
    }

    public void addBooking(BookingHistoryEntity booking) {
        if (cachedBookings == null) {
            getUserBookingHistory(booking.getUserEmail());
        }
        if (cachedBookings != null) {
            cachedBookings.add(0, booking);
            saveBookingsToLocalFile(cachedBookings);
        }
    }
}
