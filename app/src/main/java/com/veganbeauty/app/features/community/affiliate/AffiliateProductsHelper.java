package com.veganbeauty.app.features.community.affiliate;

import android.content.Context;

import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AffiliateProductsHelper {
    private static final String FILE_NAME = "affiliate_product_local.json";

    private static File getFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    private static JSONObject readData(Context context) {
        File file = getFile(context);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return new JSONObject(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("affiliate_product.json"), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String assetStr = sb.toString();
            JSONObject root = new JSONObject(assetStr);
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(assetStr.getBytes(StandardCharsets.UTF_8));
            }
            return root;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        JSONObject emptyObj = new JSONObject();
        try {
            emptyObj.put("affiliate_products", new JSONArray());
        } catch (Exception e) {}
        return emptyObj;
    }

    private static void writeData(Context context, JSONObject jsonObject) {
        File file = getFile(context);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONObject getUserData(Context context, String userId) {
        JSONObject root = readData(context);
        JSONArray arr = root.optJSONArray("affiliate_products");
        if (arr == null) {
            arr = new JSONArray();
            try {
                root.put("affiliate_products", arr);
            } catch (Exception e) {}
        }
        
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj != null && obj.optString("userId").equals(userId)) {
                return obj;
            }
        }
        
        JSONObject newObj = new JSONObject();
        try {
            newObj.put("userId", userId);
            newObj.put("products", new JSONArray());
            arr.put(newObj);
        } catch (Exception e) {}
        return newObj;
    }

    public static boolean isCompletedOrderStatus(@Nullable String status) {
        if (status == null) {
            return false;
        }
        String s = status.trim();
        return "Hoàn tất".equals(s) || "Đã giao hàng".equals(s) || "Thành công".equals(s);
    }

    public static boolean isProductDisplayed(Context context, String userId, String productId) {
        if (!isCompletedPurchaseProduct(context, userId, productId)) {
            return false;
        }
        Boolean explicit = getExplicitDisplayFlag(context, userId, productId);
        return explicit == null || explicit;
    }

    @Nullable
    private static Boolean getExplicitDisplayFlag(Context context, String userId, String productId) {
        JSONObject userData = getUserData(context, userId);
        JSONArray productsArr = userData.optJSONArray("products");
        if (productsArr == null) {
            return null;
        }

        for (int i = 0; i < productsArr.length(); i++) {
            JSONObject p = productsArr.optJSONObject(i);
            if (p != null && p.optString("productId").equals(productId)) {
                return p.optBoolean("affiliate_display", true);
            }
        }
        return null;
    }

    public static List<String> getShowcaseProductIds(Context context, String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String safeUserId = userId.trim();
        LinkedHashSet<String> eligibleIds = getCompletedPurchaseProductIds(context, safeUserId);

        List<String> result = new ArrayList<>();
        for (String productId : eligibleIds) {
            if (isProductDisplayed(context, safeUserId, productId)) {
                result.add(productId);
            }
        }
        return result;
    }

    /**
     * Product IDs from completed ("Hoàn tất") orders for this user —
     * merges Room (live orders) with assets/orders.json (demo seed).
     */
    public static LinkedHashSet<String> getCompletedPurchaseProductIds(Context context, String userId) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (context == null || userId == null || userId.trim().isEmpty()) {
            return ids;
        }
        addCompletedPurchaseProductIds(context, userId.trim(), ids);
        return ids;
    }

    private static void addCompletedPurchaseProductIds(Context context, String userId, Set<String> ids) {
        for (OrderEntity order : loadCompletedOrdersForUser(context, userId)) {
            addProductIdsFromOrder(order, ids);
        }
    }

    private static List<OrderEntity> loadCompletedOrdersForUser(Context context, String userId) {
        List<OrderEntity> result = new ArrayList<>();
        LinkedHashSet<String> seenOrderIds = new LinkedHashSet<>();

        try {
            List<OrderEntity> roomOrders = RootieDatabase.getDatabase(context)
                    .orderDao()
                    .getCompletedOrdersForUserSync(userId);
            if (roomOrders != null) {
                for (OrderEntity order : roomOrders) {
                    if (order == null) {
                        continue;
                    }
                    String orderId = order.getId();
                    if (orderId != null && !orderId.trim().isEmpty()) {
                        if (!seenOrderIds.add(orderId.trim())) {
                            continue;
                        }
                    }
                    result.add(order);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            for (OrderEntity order : new com.veganbeauty.app.data.local.LocalJsonReader(context).getAllOrders()) {
                if (order == null || !userId.equals(order.getUserId()) || !isCompletedOrderStatus(order.getStatus())) {
                    continue;
                }
                String orderId = order.getId();
                if (orderId != null && !orderId.trim().isEmpty()) {
                    if (!seenOrderIds.add(orderId.trim())) {
                        continue;
                    }
                }
                result.add(order);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private static void addProductIdsFromOrder(@Nullable OrderEntity order, Set<String> ids) {
        if (order == null || order.getItems() == null) {
            return;
        }
        for (OrderItem item : order.getItems()) {
            if (item == null) {
                continue;
            }
            String productId = item.getProductId();
            if (productId != null && !productId.trim().isEmpty()) {
                ids.add(productId.trim());
            }
        }
    }

    private static boolean isCompletedPurchaseProduct(Context context, String userId, String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        return getCompletedPurchaseProductIds(context, userId).contains(productId.trim());
    }

    /**
     * When a buyer order becomes completed, mark its products as available/displayed
     * in Community affiliate "Sản phẩm khả dụng".
     */
    public static void syncProductsFromCompletedOrder(Context context, @Nullable OrderEntity order) {
        if (context == null || order == null) {
            return;
        }
        if (!isCompletedOrderStatus(order.getStatus())) {
            return;
        }
        String userId = order.getUserId();
        if (userId == null || userId.trim().isEmpty() || order.getItems() == null) {
            return;
        }
        String safeUserId = userId.trim();
        for (OrderItem item : order.getItems()) {
            if (item == null) {
                continue;
            }
            String productId = item.getProductId();
            if (productId == null || productId.trim().isEmpty()) {
                continue;
            }
            // Force-write: order itself proves purchase eligibility.
            writeDisplayFlag(context, safeUserId, productId.trim(), true, true);
        }
    }

    public static void setProductDisplayed(Context context, String userId, String productId, boolean displayed) {
        writeDisplayFlag(context, userId, productId, displayed, false);
    }

    private static void writeDisplayFlag(
            Context context,
            String userId,
            String productId,
            boolean displayed,
            boolean forceAllow
    ) {
        try {
            JSONObject root = readData(context);
            JSONArray arr = root.optJSONArray("affiliate_products");
            if (arr == null) {
                arr = new JSONArray();
                root.put("affiliate_products", arr);
            }

            JSONObject userData = null;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null && obj.optString("userId").equals(userId)) {
                    userData = obj;
                    break;
                }
            }

            if (userData == null) {
                userData = new JSONObject();
                userData.put("userId", userId);
                userData.put("products", new JSONArray());
                arr.put(userData);
            }

            JSONArray productsArr = userData.optJSONArray("products");
            if (productsArr == null) {
                productsArr = new JSONArray();
                userData.put("products", productsArr);
            }

            boolean found = false;
            for (int i = 0; i < productsArr.length(); i++) {
                JSONObject p = productsArr.optJSONObject(i);
                if (p != null && p.optString("productId").equals(productId)) {
                    if (!forceAllow) {
                        p.put("affiliate_display", displayed);
                    }
                    // forceAllow (order sync): keep user's existing on/off choice
                    found = true;
                    break;
                }
            }

            if (!found) {
                if (!forceAllow && !isCompletedPurchaseProduct(context, userId, productId)) {
                    return;
                }
                JSONObject p = new JSONObject();
                p.put("productId", productId);
                p.put("affiliate_display", displayed);
                productsArr.put(p);
            }

            writeData(context, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
