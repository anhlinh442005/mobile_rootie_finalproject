package com.veganbeauty.app.data.local;

import android.content.Context;
import com.veganbeauty.app.data.local.entities.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LocalJsonReader {
    private final Context context;

    public LocalJsonReader(Context context) {
        this.context = context;
    }

    private String readAssetFile(String fileName) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(fileName), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<OrderEntity> getAllOrders() {
        List<OrderEntity> orderList = new ArrayList<>();
        try {
            String jsonString = readAssetFile("orders.json");
            if (jsonString == null)
                return orderList;
            JSONObject root = new JSONObject(jsonString);
            org.json.JSONArray jsonArray = root.getJSONArray("orders");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                OrderEntity order = new OrderEntity(obj.getString("id"));
                order.setUserId(obj.getString("userId"));
                order.setStatus(obj.getString("status"));
                order.setOrderDate(obj.getString("orderDate"));
                order.setOrderTime(obj.getString("orderTime"));
                order.setTotalAmount(obj.getLong("totalAmount"));
                order.setSubTotal(obj.optLong("subTotal", obj.getLong("totalAmount")));
                order.setPaymentMethod(obj.getString("paymentMethod"));
                order.setShippingName(obj.getString("shippingName"));
                order.setShippingPhone(obj.getString("shippingPhone"));
                order.setShippingAddress(obj.getString("shippingAddress"));
                order.setShippingCost(obj.optLong("shippingCost", 0L));
                order.setVoucherDiscount(obj.optLong("voucherDiscount", 0L));
                order.setHasReview(obj.optBoolean("hasReview", false));
                order.setAffiliate(obj.optBoolean("isAffiliate", false));

                org.json.JSONArray itemsArray = obj.getJSONArray("items");
                List<OrderEntity.OrderItem> items = new ArrayList<>();
                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject itemObj = itemsArray.getJSONObject(j);
                    OrderEntity.OrderItem item = new OrderEntity.OrderItem();
                    item.setProductId(itemObj.getString("productId"));
                    item.setProductName(itemObj.getString("productName"));
                    item.setProductImage(itemObj.getString("productImage"));
                    item.setQuantity(itemObj.getInt("quantity"));
                    item.setPrice(itemObj.getLong("price"));
                    items.add(item);
                }
                order.setItems(items);

                if (order.isAffiliate) {
                    JSONObject affObj = obj.optJSONObject("affiliate");
                    if (affObj != null) {
                        OrderEntity.AffiliateInfo aff = new OrderEntity.AffiliateInfo();
                        aff.setAffiliate_id(affObj.getString("affiliate_id"));
                        aff.setReferrerUserId(affObj.getString("referrerUserId"));
                        aff.setCommissionAmount(affObj.getLong("commissionAmount"));
                        aff.setCommissionStatus(affObj.getString("commissionStatus"));
                        order.setAffiliateInfo(aff);
                    }
                }
                orderList.add(order);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orderList;
    }

    public Context getContext() {
        return context;
    }

    public org.json.JSONArray getSkinHistory() {
        return new org.json.JSONArray();
    }

    public String getRawUsersJson() {
        return readAssetFile("users.json");
    }

    public String getRawSkinHistoryJson() {
        return readAssetFile("skin_history.json");
    }

    public String getRawSkinBookingsJson() {
        return readAssetFile("skin_bookings.json");
    }

    public List<UserEntity> getUsers() {
        try {
            String json = getRawUsersJson();
            if (json == null) return new ArrayList<>();
            org.json.JSONArray jsonArray = new org.json.JSONArray(json);
            List<UserEntity> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject obj = jsonArray.getJSONObject(i);
                String avatar = obj.optString("avatar", null);
                if (avatar == null || avatar.trim().isEmpty()) {
                    avatar = "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png";
                }
                list.add(new UserEntity(
                        obj.optString("user_id", obj.optString("id", java.util.UUID.randomUUID().toString())),
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
            String json = readAssetFile("community_posts.json");
            if (json == null) return new ArrayList<>();
            return parsePosts(json, getUsers());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private List<CommunityPostEntity> parsePosts(String jsonString, List<UserEntity> usersList) {
        java.util.Map<String, UserEntity> usersMap = new java.util.HashMap<>();
        for (UserEntity u : usersList) {
            usersMap.put(u.getUser_id(), u);
        }

        org.json.JSONArray jsonArray;
        try {
            jsonArray = new org.json.JSONArray(jsonString);
        } catch (Exception e) {
            try {
                org.json.JSONObject root = new org.json.JSONObject(jsonString);
                jsonArray = root.getJSONArray("posts");
            } catch (Exception ex) {
                return new ArrayList<>();
            }
        }

        List<CommunityPostEntity> postList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            org.json.JSONObject obj = jsonArray.optJSONObject(i);
            if (obj == null) continue;

            org.json.JSONObject authorObj = obj.optJSONObject("author");
            org.json.JSONObject reactionsObj = obj.optJSONObject("reactions");

            List<String> mediaUrls = new ArrayList<>();
            org.json.JSONArray mediaArr = obj.optJSONArray("media");
            if (mediaArr != null) {
                for (int j = 0; j < mediaArr.length(); j++) {
                    org.json.JSONObject mediaObj = mediaArr.optJSONObject(j);
                    if (mediaObj != null) mediaUrls.add(mediaObj.optString("url", ""));
                }
            }

            List<String> linkedProducts = new ArrayList<>();
            org.json.JSONArray linkedProductsArr = obj.optJSONArray("linked_products");
            if (linkedProductsArr != null) {
                for (int j = 0; j < linkedProductsArr.length(); j++) {
                    linkedProducts.add(linkedProductsArr.optString(j, ""));
                }
            }

            String contentStr = obj.has("content") ? obj.optString("content", "") : obj.optString("message", "");

            String mediaUrlsStr = "";
            if (!mediaUrls.isEmpty()) {
                mediaUrlsStr = String.join(",", mediaUrls);
            } else {
                org.json.JSONObject imageObj = obj.optJSONObject("image");
                if (imageObj != null) {
                    mediaUrlsStr = imageObj.optString("uri", "");
                } else {
                    org.json.JSONArray imageArr = obj.optJSONArray("image");
                    if (imageArr != null) {
                        List<String> arrUrls = new ArrayList<>();
                        for (int j = 0; j < imageArr.length(); j++) {
                            org.json.JSONObject imgItem = imageArr.optJSONObject(j);
                            if (imgItem != null) {
                                String uri = imgItem.optString("uri", "");
                                if (!uri.isEmpty()) arrUrls.add(uri);
                            }
                        }
                        mediaUrlsStr = String.join(",", arrUrls);
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

            String authorUsername = "";
            if (realUser != null && !realUser.getUsername().isEmpty()) {
                authorUsername = realUser.getUsername();
            } else if (authorObj != null) {
                authorUsername = authorObj.optString("username", "");
            }

            String authorDisplayName = "";
            if (realUser != null && realUser.getFull_name() != null && !realUser.getFull_name().trim().isEmpty()) {
                authorDisplayName = realUser.getFull_name();
            } else if (realUser != null && realUser.getUsername() != null && !realUser.getUsername().isEmpty()) {
                authorDisplayName = realUser.getUsername();
            } else if (authorObj != null) {
                authorDisplayName = authorObj.optString("display_name", authorObj.optString("username", ""));
            }

            String authorAvatarUrl = "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png";
            if ("rootie_official".equals(authorId) || "rootie_vn".equals(authorId)) {
                authorAvatarUrl = "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1781257994/favicon_r7kqwf.png";
            } else {
                String avatarFromPost = null;
                if (authorObj != null) {
                    avatarFromPost = authorObj.optString("avatar_url", "");
                    if (avatarFromPost.isEmpty()) {
                        avatarFromPost = authorObj.optString("profile_picture_url", "");
                    }
                }
                if (avatarFromPost != null && !avatarFromPost.isEmpty()) {
                    authorAvatarUrl = avatarFromPost;
                } else if (realUser != null && realUser.getAvatar() != null && !realUser.getAvatar().isEmpty()) {
                    authorAvatarUrl = realUser.getAvatar();
                }
            }

            int likesCount = 0;
            if (reactionsObj != null) {
                likesCount = reactionsObj.optInt("like", obj.optInt("reactions_count", 0));
            } else {
                likesCount = obj.optInt("reactions_count", 0);
            }

            int commentsCount = 0;
            org.json.JSONArray commentsArr = obj.optJSONArray("comments");
            if (commentsArr != null) {
                commentsCount = commentsArr.length();
            } else {
                commentsCount = obj.optInt("comments_count", 0);
            }

            postList.add(new CommunityPostEntity(
                    obj.optString("post_id", java.util.UUID.randomUUID().toString()),
                    authorId,
                    authorUsername,
                    authorDisplayName,
                    authorAvatarUrl,
                    contentStr,
                    createdAtStr,
                    likesCount,
                    commentsCount,
                    reupsCount,
                    obj.optString("skin_type", ""),
                    obj.optString("concern", ""),
                    mediaUrlsStr,
                    obj.optString("type", ""),
                    String.join(",", linkedProducts)
            ));
        }
        return postList;
    }

    public List<ReelEntity> getReels() {
        try {
            String json = readAssetFile("community_reels_fb.json");
            if (json == null) return new ArrayList<>();
            org.json.JSONArray jsonArray = new org.json.JSONArray(json);
            List<ReelEntity> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject obj = jsonArray.getJSONObject(i);
                org.json.JSONObject authorObj = obj.optJSONObject("author");
                org.json.JSONObject statsObj = obj.optJSONObject("stats");
                list.add(new ReelEntity(
                        obj.optString("video_id", java.util.UUID.randomUUID().toString()),
                        obj.optString("caption", ""),
                        authorObj != null ? authorObj.optString("user_id", "") : "",
                        authorObj != null ? authorObj.optString("username", "") : "",
                        authorObj != null ? authorObj.optString("display_name", "") : "",
                        authorObj != null ? authorObj.optString("avatar_url", "") : "",
                        statsObj != null ? statsObj.optInt("likes", 0) : obj.optInt("likes_count", 0),
                        statsObj != null ? statsObj.optInt("comments", 0) : 0,
                        statsObj != null ? statsObj.optInt("shares", 0) : 0,
                        obj.optString("thumbnail_url", "")
                ));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<String> getShowcaseProductsForUser(String userId) {
        return new ArrayList<>();
    }

    public void saveLocalPost(com.veganbeauty.app.data.local.entities.CommunityPostEntity post) {
    }

    public java.util.Map<String, List<String>> getMutualFriendsForUsers(List<String> myFriends,
            List<String> allTargetIds) {
        return new java.util.HashMap<>();
    }

    public List<YtVideoEntity> getExploreVideos() {
        try {
            String json = readAssetFile("community_video_yt.json");
            if (json == null) return new ArrayList<>();
            org.json.JSONArray jsonArray = new org.json.JSONArray(json);
            List<YtVideoEntity> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject obj = jsonArray.getJSONObject(i);
                String desc = obj.optString("short_description", "");
                if (desc.isEmpty()) desc = obj.optString("long_description", "");
                
                String username = obj.optString("username", "");
                org.json.JSONObject authorObj = obj.optJSONObject("author");
                if (authorObj != null) {
                    username = authorObj.optString("username", "");
                }
                
                list.add(new YtVideoEntity(
                        obj.optString("_id", java.util.UUID.randomUUID().toString()),
                        obj.optString("title", ""),
                        obj.optString("url", ""),
                        desc,
                        username,
                        obj.optString("avatarUrl", ""),
                        obj.optString("type", "video"),
                        (int)(Math.random() * 5000),
                        (int)(Math.random() * 500),
                        (int)(Math.random() * 100)
                ));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<IngredientEntity> getIngredients() {
        try {
            String json = readAssetFile("ingredient.json");
            if (json == null) return new ArrayList<>();
            org.json.JSONArray jsonArray = new org.json.JSONArray(json);
            List<IngredientEntity> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject obj = jsonArray.getJSONObject(i);
                List<String> types = new ArrayList<>();
                org.json.JSONArray typesArr = obj.optJSONArray("types");
                if (typesArr != null) {
                    for (int j = 0; j < typesArr.length(); j++) {
                        types.add(typesArr.getString(j));
                    }
                }
                list.add(new IngredientEntity(
                        obj.optString("slug", java.util.UUID.randomUUID().toString()),
                        obj.optString("name", ""),
                        obj.optString("scientific_name", obj.optString("scientificName", "")),
                        obj.optString("image", ""),
                        obj.optString("origin", ""),
                        obj.optString("description", ""),
                        obj.optString("uses", ""),
                        types
                ));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<NotificationItem> getAllNotifications() {
        return new ArrayList<>();
    }

    public List<BookingHistoryEntity> getUserBookingHistory(String email) {
        return new ArrayList<>();
    }

    public List<StoreEntity> getAllStores() {
        try {
            String json = readAssetFile("rootie_stores.json");
            if (json == null) return new ArrayList<>();
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<StoreEntity>>(){}.getType();
            return new com.google.gson.Gson().fromJson(json, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<com.veganbeauty.app.data.local.entities.CommunityProduct> getProducts() {
        return new ArrayList<>();
    }

    public List<com.veganbeauty.app.data.local.entities.CommunityBlogEntity> getCommunityBlogs(int limit, int offset) {
        List<CommunityBlogEntity> list = new ArrayList<>();
        try {
            java.io.InputStream inputStream = context.getAssets().open("community_blog.json");
            android.util.JsonReader reader = new android.util.JsonReader(new java.io.InputStreamReader(inputStream, "UTF-8"));
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
                    if ("_id".equals(name)) {
                        id = reader.nextString();
                    } else if ("title".equals(name)) {
                        title = reader.nextString();
                    } else if ("shortDescription".equals(name)) {
                        shortDesc = reader.nextString();
                    } else if ("publishedAt".equals(name)) {
                        if (reader.peek() == android.util.JsonToken.NULL) {
                            reader.nextNull();
                        } else {
                            publishedAt = reader.nextString();
                        }
                    } else if ("primaryImage".equals(name)) {
                        if (reader.peek() == android.util.JsonToken.NULL) {
                            reader.nextNull();
                        } else {
                            reader.beginObject();
                            while(reader.hasNext()){
                                if("url".equals(reader.nextName())) {
                                    if (reader.peek() == android.util.JsonToken.NULL) {
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
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
                list.add(new CommunityBlogEntity(id, title, shortDesc, imageUrl, publishedAt));
            }
            reader.close();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<com.veganbeauty.app.data.local.entities.CommunityPostEntity> getCommunityNews() {
        return new ArrayList<>();
    }

    public java.util.Map<String, java.util.List<String>> getSocialDataForUser(String userId) {
        return new java.util.HashMap<>();
    }

    public List<ProductEntity> getAllProducts() {
        try {
            String json = readAssetFile("products.json");
            if (json == null) return new ArrayList<>();
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONArray jsonArray = root.getJSONArray("products");
            List<ProductEntity> productList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject obj = jsonArray.getJSONObject(i);
                
                Object categoryIdRaw = obj.opt("categoryId");
                String categoryIdsStr = "";
                if (categoryIdRaw instanceof org.json.JSONArray) {
                    org.json.JSONArray catArray = (org.json.JSONArray) categoryIdRaw;
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < catArray.length(); j++) {
                        sb.append(catArray.getString(j));
                        if (j < catArray.length() - 1) sb.append(",");
                    }
                    categoryIdsStr = sb.toString();
                } else if (categoryIdRaw instanceof String) {
                    categoryIdsStr = (String) categoryIdRaw;
                }

                List<String> albumList = new ArrayList<>();
                org.json.JSONArray albumArr = obj.optJSONArray("album");
                if (albumArr != null) {
                    for (int j = 0; j < albumArr.length(); j++) {
                        albumList.add(albumArr.getString(j));
                    }
                }

                List<com.veganbeauty.app.data.local.entities.KeyIngredient> keyIngredientsList = new ArrayList<>();
                org.json.JSONArray keyArr = obj.optJSONArray("keyIngredients");
                if (keyArr != null) {
                    for (int j = 0; j < keyArr.length(); j++) {
                        org.json.JSONObject keyObj = keyArr.getJSONObject(j);
                        keyIngredientsList.add(new com.veganbeauty.app.data.local.entities.KeyIngredient(
                                keyObj.optString("name", ""),
                                keyObj.optString("description", "")
                        ));
                    }
                }

                List<String> detailedIngredientsList = new ArrayList<>();
                org.json.JSONArray detailedArr = obj.optJSONArray("detailedIngredients");
                if (detailedArr != null) {
                    for (int j = 0; j < detailedArr.length(); j++) {
                        detailedIngredientsList.add(detailedArr.getString(j));
                    }
                }

                List<String> idealForList = new ArrayList<>();
                org.json.JSONArray idealArr = obj.optJSONArray("idealFor");
                if (idealArr != null) {
                    for (int j = 0; j < idealArr.length(); j++) {
                        idealForList.add(idealArr.getString(j));
                    }
                }

                List<String> benefitsList = new ArrayList<>();
                org.json.JSONArray benefitsArr = obj.optJSONArray("benefits");
                if (benefitsArr != null) {
                    for (int j = 0; j < benefitsArr.length(); j++) {
                        benefitsList.add(benefitsArr.getString(j));
                    }
                }

                Long originalPrice = obj.has("originalPrice") ? obj.getLong("originalPrice") : null;
                boolean isNew = obj.optBoolean("newProduct", false) || obj.optBoolean("isNew", false);

                ProductEntity p = new ProductEntity(
                        obj.optString("id", ""),
                        obj.optString("name", ""),
                        obj.optString("sku", ""),
                        obj.optString("barcode", ""),
                        obj.optLong("price", 0),
                        originalPrice,
                        obj.optString("category", ""),
                        obj.optString("brand", ""),
                        obj.optInt("stock", 0),
                        obj.optString("description", ""),
                        obj.optString("mainImage", ""),
                        obj.optString("suitableFor", ""),
                        obj.optString("origin", ""),
                        obj.optString("expiryDate", ""),
                        isNew,
                        categoryIdsStr,
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
                );
                productList.add(p);
            }
            return productList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<String> getFriendsForUser(String username) {
        return new ArrayList<>();
    }

    public void updateBookingStatus(String id, String status, String reason) {
    }

    public void addBooking(BookingHistoryEntity booking) {
    }
}
