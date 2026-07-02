package com.veganbeauty.app.data.local;

import android.content.Context;

import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.utils.RootieBrandHelper;
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.CommunityProduct;
import com.veganbeauty.app.data.local.entities.IngredientEntity;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class LocalJsonReader {
    private static final String LOCAL_BOOKINGS_FILE = "local_bookings.json";
    private static final String SEED_BOOKINGS_ASSET = "skin_bookings.json";

    private final Context context;

    private static List<ProductEntity> cachedProducts;
    private static Map<String, ProductEntity> cachedProductsById;

    public LocalJsonReader(Context context) {
        this.context = context.getApplicationContext();
    }

    public static void clearProductCache() {
        synchronized (LocalJsonReader.class) {
            cachedProducts = null;
            cachedProductsById = null;
        }
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

    public String readAsset(String fileName) {
        return readAssetFile(fileName);
    }

    public List<OrderEntity> getAllOrders() {
        List<OrderEntity> orderList = new ArrayList<>();
        try {
            String jsonString = readAssetFile("orders.json");
            if (jsonString == null) return orderList;
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

                JSONObject affObj = obj.optJSONObject("affiliate");
                if (affObj != null) {
                    OrderEntity.AffiliateInfo aff = new OrderEntity.AffiliateInfo();
                    aff.setAffiliate_id(affObj.getString("affiliate_id"));
                    aff.setReferrerUserId(affObj.getString("referrerUserId"));
                    aff.setCommissionAmount(affObj.getLong("commissionAmount"));
                    aff.setCommissionStatus(affObj.getString("commissionStatus"));
                    order.setAffiliate(aff);
                    order.setAffiliate(true);
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

    public String getRawUsersJson() {
        return readAssetFile("users.json");
    }

    public String getRawSkinHistoryJson() {
        return readAssetFile("skin_history.json");
    }

    public org.json.JSONArray getSkinHistory() {
        try {
            String json = getRawSkinHistoryJson();
            if (json == null) return new org.json.JSONArray();
            return new org.json.JSONArray(json);
        } catch (Exception e) {
            return new org.json.JSONArray();
        }
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
                String userId = obj.optString("user_id", obj.optString("id", java.util.UUID.randomUUID().toString()));
                String avatar = obj.optString("avatar", null);
                if (RootieBrandHelper.isRootieUser(userId)) {
                    avatar = RootieBrandHelper.AVATAR_URL;
                } else if (avatar == null || avatar.trim().isEmpty()) {
                    avatar = "";
                }
                list.add(new UserEntity(
                        userId,
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
        Map<String, UserEntity> usersMap = new HashMap<>();
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
            if (authorId.isEmpty()) {
                authorId = obj.optString("user_id", obj.optString("author_id", ""));
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

            String authorAvatarUrl = "";
            if (RootieBrandHelper.isRootieUser(authorId)) {
                authorAvatarUrl = RootieBrandHelper.AVATAR_URL;
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

            int likesCount;
            if (reactionsObj != null) {
                likesCount = reactionsObj.optInt("like", obj.optInt("reactions_count", 0));
            } else {
                likesCount = obj.optInt("reactions_count", 0);
            }

            int commentsCount;
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
                    obj.optString("type", "Kiến thức"),
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
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return com.veganbeauty.app.features.community.affiliate.AffiliateProductsHelper
                .getShowcaseProductIds(context, userId.trim());
    }

    public void saveLocalPost(CommunityPostEntity post) {
    }

    public Map<String, List<String>> getMutualFriendsForUsers(List<String> myFriends,
            List<String> allTargetIds) {
        return new HashMap<>();
    }

    public List<YtVideoEntity> getExploreVideos() {
        try {
            String json = readAssetFile("community_video_yt.json");
            if (json == null) return new ArrayList<>();
            org.json.JSONArray jsonArray = new org.json.JSONArray(json);
            List<YtVideoEntity> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject obj = jsonArray.getJSONObject(i);

                List<String> types = new ArrayList<>();
                org.json.JSONArray typeArray = obj.optJSONArray("Type");
                if (typeArray != null) {
                    for (int j = 0; j < typeArray.length(); j++) {
                        types.add(typeArray.optString(j, ""));
                    }
                }
                if (types.isEmpty()) {
                    String legacyType = obj.optString("type", "");
                    if (!legacyType.isEmpty()) types.add(legacyType);
                }

                String desc = obj.optString("long_description", "");
                if (desc.isEmpty()) desc = obj.optString("short_description", "");
                if (desc.isEmpty()) desc = obj.optString("title", "");

                String username = obj.optString("username", "Unknown User");
                org.json.JSONObject authorObj = obj.optJSONObject("author");
                if (authorObj != null && !authorObj.optString("username", "").isEmpty()) {
                    username = authorObj.optString("username", username);
                }

                String avatarUrl = obj.optString("avatar", "");
                if (avatarUrl.isEmpty()) avatarUrl = obj.optString("avatarUrl", "");

                YtVideoEntity entity = new YtVideoEntity(
                        obj.optString("_id", java.util.UUID.randomUUID().toString()),
                        obj.optString("title", ""),
                        obj.optString("url", ""),
                        desc,
                        username,
                        avatarUrl.isEmpty() ? null : avatarUrl,
                        String.join(",", types),
                        obj.optInt("likes", 100 + (int) (Math.random() * 4900)),
                        (int) (Math.random() * 500),
                        (int) (Math.random() * 100)
                );

                org.json.JSONArray hashArr = obj.optJSONArray("hashtags");
                if (hashArr != null) {
                    List<String> hashList = new ArrayList<>();
                    for (int j = 0; j < hashArr.length(); j++) {
                        hashList.add(hashArr.optString(j, ""));
                    }
                    entity.setHashtags(String.join(" ", hashList));
                }

                org.json.JSONArray kwArr = obj.optJSONArray("keywords");
                if (kwArr != null) {
                    List<String> kwList = new ArrayList<>();
                    for (int j = 0; j < kwArr.length(); j++) {
                        String kw = kwArr.optString(j, "").replace(" ", "");
                        if (!kw.isEmpty()) kwList.add("#" + kw);
                    }
                    entity.setKeywords(String.join(" ", kwList));
                }

                list.add(entity);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static boolean isNotebookVideo(YtVideoEntity video) {
        if (video == null || video.getType() == null) return false;
        return video.getType().toLowerCase().contains("notebook");
    }

    public List<YtVideoEntity> getNotebookVideos() {
        List<YtVideoEntity> notebooks = new ArrayList<>();
        for (YtVideoEntity video : getExploreVideos()) {
            if (isNotebookVideo(video)) {
                notebooks.add(video);
            }
        }
        return notebooks;
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

    public List<BookingHistoryEntity> getUserBookingHistory(String emailOrUserId) {
        if (emailOrUserId == null || emailOrUserId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String key = emailOrUserId.trim();
        if (key.contains("@")) {
            String normalizedEmail = key.toLowerCase(Locale.ROOT);
            List<BookingHistoryEntity> result = new ArrayList<>();
            for (BookingHistoryEntity booking : loadAllBookingsInternal()) {
                if (matchesUserEmail(booking, normalizedEmail)) {
                    result.add(booking);
                }
            }
            sortBookingsByNewest(result);
            return result;
        }
        List<BookingHistoryEntity> result = new ArrayList<>();
        for (BookingHistoryEntity booking : loadAllBookingsInternal()) {
            if (key.equalsIgnoreCase(booking.getUserId())) {
                result.add(booking);
            }
        }
        sortBookingsByNewest(result);
        return result;
    }

    public synchronized void addBooking(BookingHistoryEntity booking) {
        if (booking == null || booking.getId() == null || booking.getId().trim().isEmpty()) {
            return;
        }
        List<BookingHistoryEntity> all = loadAllBookingsInternal();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (booking.getId().equals(all.get(i).getId())) {
                all.set(i, booking);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(0, booking);
        }
        saveAllBookingsInternal(all);
    }

    public synchronized void updateBookingStatus(String id, String status, String reason) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        List<BookingHistoryEntity> all = loadAllBookingsInternal();
        boolean updated = false;
        for (BookingHistoryEntity booking : all) {
            if (id.equals(booking.getId())) {
                booking.setStatus(status != null ? status : booking.getStatus());
                booking.setCancelReason(reason != null ? reason : "");
                if ("Đã huỷ".equals(status) || "Đã hủy".equals(status)) {
                    booking.setCancelledAt(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date()));
                }
                updated = true;
                break;
            }
        }
        if (updated) {
            saveAllBookingsInternal(all);
        }
    }

    public synchronized void mergeBookingsFromRemote(List<BookingHistoryEntity> remoteBookings) {
        if (remoteBookings == null || remoteBookings.isEmpty()) {
            return;
        }
        List<BookingHistoryEntity> all = loadAllBookingsInternal();
        Map<String, Integer> indexById = new HashMap<>();
        for (int i = 0; i < all.size(); i++) {
            indexById.put(all.get(i).getId(), i);
        }

        for (BookingHistoryEntity remote : remoteBookings) {
            if (remote == null || remote.getId() == null || remote.getId().trim().isEmpty()) {
                continue;
            }
            Integer idx = indexById.get(remote.getId());
            if (idx == null) {
                all.add(remote);
                indexById.put(remote.getId(), all.size() - 1);
            } else {
                BookingHistoryEntity local = all.get(idx);
                if (shouldPreferRemoteBooking(local, remote)) {
                    all.set(idx, remote);
                }
            }
        }
        saveAllBookingsInternal(all);
    }

    private synchronized List<BookingHistoryEntity> loadAllBookingsInternal() {
        List<BookingHistoryEntity> fromLocal = readBookingsFromLocalFile();
        if (!fromLocal.isEmpty()) {
            return fromLocal;
        }

        String seedJson = readAssetFile(SEED_BOOKINGS_ASSET);
        if (seedJson != null && !seedJson.trim().isEmpty()) {
            List<BookingHistoryEntity> seeded = parseBookingsJson(seedJson);
            if (!seeded.isEmpty()) {
                saveAllBookingsInternal(seeded);
                return new ArrayList<>(seeded);
            }
        }
        return new ArrayList<>();
    }

    private List<BookingHistoryEntity> readBookingsFromLocalFile() {
        File file = new File(context.getFilesDir(), LOCAL_BOOKINGS_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return parseBookingsJson(sb.toString());
        } catch (Exception e) {
            android.util.Log.w("LocalJsonReader", "readBookingsFromLocalFile failed", e);
            return new ArrayList<>();
        }
    }

    private void saveAllBookingsInternal(List<BookingHistoryEntity> bookings) {
        try {
            JSONArray array = new JSONArray();
            for (BookingHistoryEntity booking : bookings) {
                array.put(bookingToJson(booking));
            }
            JSONObject root = new JSONObject();
            root.put("bookings", array);

            File file = new File(context.getFilesDir(), LOCAL_BOOKINGS_FILE);
            try (FileOutputStream fos = new FileOutputStream(file, false);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
        } catch (Exception e) {
            android.util.Log.e("LocalJsonReader", "saveAllBookingsInternal failed", e);
        }
    }

    private List<BookingHistoryEntity> parseBookingsJson(String json) {
        List<BookingHistoryEntity> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }
        try {
            String trimmed = json.trim();
            JSONArray array;
            if (trimmed.startsWith("[")) {
                array = new JSONArray(trimmed);
            } else {
                JSONObject root = new JSONObject(trimmed);
                array = root.optJSONArray("bookings");
                if (array == null) {
                    array = root.optJSONArray("skin_bookings");
                }
            }
            if (array == null) {
                return list;
            }
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) continue;
                BookingHistoryEntity entity = parseBookingObject(obj);
                if (entity != null) {
                    list.add(entity);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("LocalJsonReader", "parseBookingsJson failed", e);
        }
        return list;
    }

    private BookingHistoryEntity parseBookingObject(JSONObject obj) {
        try {
            String id = obj.optString("id", "");
            if (id.isEmpty()) {
                id = obj.optString("bookingId", UUID.randomUUID().toString());
            }

            List<String> skinResults = new ArrayList<>();
            JSONArray skinArr = obj.optJSONArray("skinResults");
            if (skinArr != null) {
                for (int j = 0; j < skinArr.length(); j++) {
                    skinResults.add(skinArr.optString(j, ""));
                }
            }

            String userEmail = obj.optString("userEmail", "");
            if (userEmail.isEmpty()) {
                userEmail = obj.optString("email", "");
            }

            return new BookingHistoryEntity(
                    id,
                    obj.optString("userId", ""),
                    obj.optString("userName", ""),
                    obj.optString("userPhone", ""),
                    userEmail,
                    obj.optString("serviceName", ""),
                    obj.optString("dateDisplay", ""),
                    obj.optString("monthDisplay", ""),
                    obj.optString("dayOfWeek", ""),
                    obj.optString("time", ""),
                    obj.optString("duration", ""),
                    obj.optString("storeName", ""),
                    obj.optString("storeAddress", ""),
                    obj.optString("storePhone", ""),
                    obj.optString("storeImage", ""),
                    obj.optString("note", ""),
                    obj.optString("status", "Chờ xác nhận"),
                    obj.optString("policy", ""),
                    obj.optString("createdAt", ""),
                    obj.optString("completedAt", ""),
                    skinResults,
                    obj.optString("consultantName", ""),
                    obj.optString("consultantAvatar", ""),
                    (float) obj.optDouble("consultantRating", 0),
                    (float) obj.optDouble("userRating", 0),
                    obj.optString("userReview", ""),
                    obj.optString("reviewDate", ""),
                    obj.optString("beforeImage", ""),
                    obj.optString("afterImage", ""),
                    obj.optInt("earnedPoints", 0),
                    obj.optInt("totalPoints", 0),
                    obj.optString("nextAppointmentDate", ""),
                    obj.optString("nextAppointmentText", ""),
                    obj.optString("cancelledAt", ""),
                    obj.optString("cancelReason", "")
            );
        } catch (Exception e) {
            android.util.Log.w("LocalJsonReader", "parseBookingObject failed", e);
            return null;
        }
    }

    private JSONObject bookingToJson(BookingHistoryEntity booking) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("id", booking.getId());
        obj.put("userId", booking.getUserId());
        obj.put("userName", booking.getUserName());
        obj.put("userPhone", booking.getUserPhone());
        obj.put("userEmail", booking.getUserEmail());
        obj.put("email", booking.getUserEmail());
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
        obj.put("skinResults", new JSONArray(booking.getSkinResults()));
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
        return obj;
    }

    private boolean matchesUserEmail(BookingHistoryEntity booking, String normalizedEmail) {
        if (booking.getUserEmail() == null) {
            return false;
        }
        return normalizedEmail.equals(booking.getUserEmail().trim().toLowerCase(Locale.ROOT));
    }

    private void sortBookingsByNewest(List<BookingHistoryEntity> bookings) {
        Collections.sort(bookings, (a, b) -> {
            int cmp = compareCreatedAt(b.getCreatedAt(), a.getCreatedAt());
            if (cmp != 0) return cmp;
            return b.getId().compareTo(a.getId());
        });
    }

    private int compareCreatedAt(String a, String b) {
        if (a == null || a.isEmpty()) return (b == null || b.isEmpty()) ? 0 : 1;
        if (b == null || b.isEmpty()) return -1;
        return b.compareTo(a);
    }

    private boolean shouldPreferRemoteBooking(BookingHistoryEntity local, BookingHistoryEntity remote) {
        if (isTerminalStatus(remote.getStatus()) && !isTerminalStatus(local.getStatus())) {
            return true;
        }
        if (isTerminalStatus(local.getStatus()) && !isTerminalStatus(remote.getStatus())) {
            return false;
        }
        return compareCreatedAt(remote.getCreatedAt(), local.getCreatedAt()) < 0;
    }

    private boolean isTerminalStatus(String status) {
        if (status == null) return false;
        return "Đã huỷ".equals(status) || "Đã hủy".equals(status) || "Đã hoàn thành".equals(status);
    }

    public List<StoreEntity> getAllStores() {
        List<StoreEntity> list = new ArrayList<>();
        try {
            String json = readAssetFile("rootie_stores.json");
            if (json == null) return list;
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                String id = obj.optString("ma_cua_hang", "store_" + i);
                org.json.JSONObject idObj = obj.optJSONObject("_id");
                if (idObj != null) {
                    id = idObj.optString("$oid", id);
                }
                if (id == null || id.isEmpty()) id = "store_" + i;

                org.json.JSONObject diaChi = obj.optJSONObject("dia_chi");
                String soNha = diaChi != null ? diaChi.optString("so_nha", "") : "";
                String duong = diaChi != null ? diaChi.optString("duong", "") : "";
                String phuongXa = diaChi != null ? diaChi.optString("phuong_xa", "") : "";
                String quanHuyen = diaChi != null ? diaChi.optString("quan_huyen", "") : "";
                String tinhThanh = diaChi != null ? diaChi.optString("tinh_thanh", "") : "";
                String diaChiDayDu = diaChi != null ? diaChi.optString("dia_chi_day_du", "") : "";

                org.json.JSONObject toaDo = obj.optJSONObject("toa_do");
                double lat = toaDo != null ? toaDo.optDouble("lat", 0) : 0;
                double lng = toaDo != null ? toaDo.optDouble("lng", 0) : 0;

                org.json.JSONObject lienHe = obj.optJSONObject("thong_tin_lien_he");
                String soDienThoai = "";
                if (lienHe != null) {
                    org.json.JSONArray phones = lienHe.optJSONArray("so_dien_thoai");
                    if (phones != null && phones.length() > 0) {
                        soDienThoai = phones.optString(0, "");
                    }
                }
                String email = lienHe != null ? lienHe.optString("email", "") : "";

                String moCua = "08:00";
                String dongCua = "21:00";
                org.json.JSONObject thoiGian = obj.optJSONObject("thoi_gian_hoat_dong");
                if (thoiGian != null) {
                    org.json.JSONObject t26 = thoiGian.optJSONObject("thu_2_6");
                    if (t26 != null) {
                        moCua = t26.optString("mo_cua", moCua);
                        dongCua = t26.optString("dong_cua", dongCua);
                    }
                }

                String tienNghi = "";
                org.json.JSONArray tienNghiArr = obj.optJSONArray("tien_nghi");
                if (tienNghiArr != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < tienNghiArr.length(); j++) {
                        if (j > 0) sb.append(", ");
                        sb.append(tienNghiArr.optString(j, ""));
                    }
                    tienNghi = sb.toString();
                }

                String imageUrl = "";
                org.json.JSONArray hinhAnh = obj.optJSONArray("hinh_anh");
                if (hinhAnh != null && hinhAnh.length() > 0) {
                    imageUrl = hinhAnh.optString(0, "");
                }

                list.add(new StoreEntity(
                        id,
                        obj.optString("ma_cua_hang", id),
                        obj.optString("ten_cua_hang", ""),
                        obj.optString("loai_hinh", ""),
                        soNha, duong, phuongXa, quanHuyen, tinhThanh, diaChiDayDu,
                        lat, lng,
                        soDienThoai, email,
                        moCua, dongCua,
                        obj.optString("trang_thai", "Đang hoạt động"),
                        obj.optBoolean("isActive", true),
                        tienNghi,
                        imageUrl,
                        0
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<CommunityProduct> getProducts() {
        List<CommunityProduct> result = new ArrayList<>();
        try {
            for (ProductEntity p : getAllProducts()) {
                String imageUrl = p.getMainImage();
                if (imageUrl == null || imageUrl.isEmpty()) {
                    List<String> album = p.getAlbum();
                    if (album != null && !album.isEmpty()) {
                        imageUrl = album.get(0);
                    } else {
                        imageUrl = "";
                    }
                }
                Integer originalPrice = p.getOriginalPrice() != null ? p.getOriginalPrice().intValue() : null;
                result.add(new CommunityProduct(
                        p.getId(),
                        p.getName(),
                        imageUrl,
                        (int) p.getPrice(),
                        originalPrice,
                        p.getRating(),
                        p.getSold()
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<CommunityBlogEntity> getCommunityBlogs(int limit, int offset) {
        List<CommunityBlogEntity> list = new ArrayList<>();
        try {
            java.io.InputStream inputStream = context.getAssets().open("community_blog.json");
            android.util.JsonReader reader = new android.util.JsonReader(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8));
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
                            while (reader.hasNext()) {
                                if ("url".equals(reader.nextName())) {
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

    public List<CommunityPostEntity> getCommunityNews() {
        try {
            String json = readAssetFile("community_news.json");
            if (json == null) return new ArrayList<>();
            return parsePosts(json, getUsers());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Map<String, List<String>> getSocialDataForUser(String userId) {
        Map<String, List<String>> result = new HashMap<>();
        result.put("friends", new ArrayList<>());
        result.put("following", new ArrayList<>());
        result.put("followers", new ArrayList<>());
        result.put("suggested", new ArrayList<>());
        try {
            String json = readAssetFile("User_com_friend.json");
            if (json == null) return result;
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                if (!userId.equals(obj.optString("user_id"))) continue;
                result.put("friends", jsonArrayToList(obj.optJSONArray("friends")));
                result.put("following", jsonArrayToList(obj.optJSONArray("following")));
                result.put("followers", jsonArrayToList(obj.optJSONArray("followers")));
                result.put("suggested", jsonArrayToList(obj.optJSONArray("suggested")));
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private List<String> jsonArrayToList(org.json.JSONArray arr) {
        List<String> list = new ArrayList<>();
        if (arr == null) return list;
        try {
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @androidx.annotation.Nullable
    public ProductEntity getProductById(String productId) {
        if (productId == null || productId.isEmpty()) return null;
        getAllProducts();
        return cachedProductsById != null ? cachedProductsById.get(productId) : null;
    }

    public List<ProductEntity> getAllProducts() {
        if (cachedProducts != null) return cachedProducts;
        synchronized (LocalJsonReader.class) {
            if (cachedProducts != null) return cachedProducts;
            cachedProducts = loadProductsFromJson();
            cachedProductsById = new HashMap<>();
            for (ProductEntity product : cachedProducts) {
                if (product.getId() != null && !product.getId().isEmpty()) {
                    cachedProductsById.put(product.getId(), product);
                }
            }
            return cachedProducts;
        }
    }

    private List<ProductEntity> loadProductsFromJson() {
        try {
            String json = readAssetFile("products.json");
            if (json == null) return new ArrayList<>();
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONArray jsonArray = root.getJSONArray("products");
            List<ProductEntity> productList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                org.json.JSONObject obj = jsonArray.getJSONObject(i);

                Object categoryIdRaw = obj.opt("categoryId");
                String categoryIdsStr = "";
                if (categoryIdRaw instanceof org.json.JSONArray) {
                    org.json.JSONArray catArray = (org.json.JSONArray) categoryIdRaw;
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < catArray.length(); j++) {
                        sb.append(catArray.optString(j, ""));
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
                        String albumUrl = albumArr.optString(j, "");
                        if (!albumUrl.isEmpty()) albumList.add(albumUrl);
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
                        detailedIngredientsList.add(detailedArr.optString(j, ""));
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

                String mainImage = obj.optString("mainImage", obj.optString("image", ""));
                if (mainImage.isEmpty() && !albumList.isEmpty()) {
                    mainImage = albumList.get(0);
                }

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
                        mainImage,
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
                } catch (Exception itemError) {
                    itemError.printStackTrace();
                }
            }
            return productList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<String> getFriendsForUser(String userId) {
        Map<String, List<String>> social = getSocialDataForUser(userId);
        List<String> friends = social.get("friends");
        return friends != null ? friends : new ArrayList<>();
    }

    public synchronized void updateBookingReview(String id, float rating, String reviewText, String reviewDate) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        List<BookingHistoryEntity> all = loadAllBookingsInternal();
        boolean updated = false;
        for (int i = 0; i < all.size(); i++) {
            BookingHistoryEntity booking = all.get(i);
            if (!id.equals(booking.getId())) {
                continue;
            }
            all.set(i, new BookingHistoryEntity(
                    booking.getId(),
                    booking.getUserId(),
                    booking.getUserName(),
                    booking.getUserPhone(),
                    booking.getUserEmail(),
                    booking.getServiceName(),
                    booking.getDateDisplay(),
                    booking.getMonthDisplay(),
                    booking.getDayOfWeek(),
                    booking.getTime(),
                    booking.getDuration(),
                    booking.getStoreName(),
                    booking.getStoreAddress(),
                    booking.getStorePhone(),
                    booking.getStoreImage(),
                    booking.getNote(),
                    booking.getStatus(),
                    booking.getPolicy(),
                    booking.getCreatedAt(),
                    booking.getCompletedAt(),
                    booking.getSkinResults(),
                    booking.getConsultantName(),
                    booking.getConsultantAvatar(),
                    booking.getConsultantRating(),
                    rating,
                    reviewText != null ? reviewText : booking.getUserReview(),
                    reviewDate != null ? reviewDate : booking.getReviewDate(),
                    booking.getBeforeImage(),
                    booking.getAfterImage(),
                    booking.getEarnedPoints(),
                    booking.getTotalPoints(),
                    booking.getNextAppointmentDate(),
                    booking.getNextAppointmentText(),
                    booking.getCancelledAt(),
                    booking.getCancelReason()
            ));
            updated = true;
            break;
        }
        if (updated) {
            saveAllBookingsInternal(all);
        }
    }

}
