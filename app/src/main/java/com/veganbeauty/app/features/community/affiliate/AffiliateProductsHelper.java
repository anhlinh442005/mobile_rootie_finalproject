package com.veganbeauty.app.features.community.affiliate;

import android.content.Context;

import androidx.annotation.Nullable;

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
        LinkedHashSet<String> eligibleIds = new LinkedHashSet<>();
        addCompletedPurchaseProductIds(context, safeUserId, eligibleIds);

        List<String> result = new ArrayList<>();
        for (String productId : eligibleIds) {
            if (isProductDisplayed(context, safeUserId, productId)) {
                result.add(productId);
            }
        }
        return result;
    }

    private static void addCompletedPurchaseProductIds(Context context, String userId, Set<String> ids) {
        for (OrderEntity order : new com.veganbeauty.app.data.local.LocalJsonReader(context).getAllOrders()) {
            if (!userId.equals(order.getUserId()) || !"Hoàn tất".equals(order.getStatus())) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                String productId = item.getProductId();
                if (productId != null && !productId.trim().isEmpty()) {
                    ids.add(productId.trim());
                }
            }
        }
    }

    private static boolean isCompletedPurchaseProduct(Context context, String userId, String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        String safeProductId = productId.trim();
        for (OrderEntity order : new com.veganbeauty.app.data.local.LocalJsonReader(context).getAllOrders()) {
            if (!userId.equals(order.getUserId()) || !"Hoàn tất".equals(order.getStatus())) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                if (safeProductId.equals(item.getProductId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void setProductDisplayed(Context context, String userId, String productId, boolean displayed) {
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
                    p.put("affiliate_display", displayed);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                if (!isCompletedPurchaseProduct(context, userId, productId)) {
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
