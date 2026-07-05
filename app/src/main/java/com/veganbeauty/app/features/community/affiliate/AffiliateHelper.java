package com.veganbeauty.app.features.community.affiliate;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class AffiliateHelper {
    private static final String FILE_NAME = "affiliates_local.json";

    public static JSONArray getAffiliateData(Context context) {
        File file = new File(context.getFilesDir(), FILE_NAME);
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
                e.printStackTrace();
            }
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("affiliates.json"), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONArray arr = root.optJSONArray("affiliates");
            if (arr == null) arr = new JSONArray();
            saveAffiliateData(context, arr);
            return arr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return new JSONArray();
    }

    public static void saveAffiliateData(Context context, JSONArray jsonArray) {
        File file = new File(context.getFilesDir(), FILE_NAME);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addAffiliateOrder(
            Context context,
            String referrerUserId,
            String productId,
            String productName,
            String productImage,
            long orderValue,
            long commissionAmount,
            String customerEmail
    ) {
        try {
            JSONArray affiliateArray = getAffiliateData(context);
            
            JSONObject userAffiliateObj = null;
            for (int i = 0; i < affiliateArray.length(); i++) {
                JSONObject obj = affiliateArray.getJSONObject(i);
                if (obj.optString("user_id").equals(referrerUserId)) {
                    userAffiliateObj = obj;
                    break;
                }
            }
            
            if (userAffiliateObj == null) {
                userAffiliateObj = new JSONObject();
                userAffiliateObj.put("user_id", referrerUserId);
                userAffiliateObj.put("total_revenue", 0L);
                userAffiliateObj.put("total_commission", 0L);
                userAffiliateObj.put("pending_commission", 0L);
                userAffiliateObj.put("successful_orders", 0);
                userAffiliateObj.put("new_customers", 0);
                userAffiliateObj.put("orders", new JSONArray());
                userAffiliateObj.put("withdrawals", new JSONArray());
                affiliateArray.put(userAffiliateObj);
            }
            
            long currentPending = userAffiliateObj.optLong("pending_commission", 0L);
            userAffiliateObj.put("pending_commission", currentPending + commissionAmount);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
            String orderDateStr = sdf.format(new Date());
            
            JSONObject newOrder = new JSONObject();
            newOrder.put("order_id", "RT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            newOrder.put("order_date", orderDateStr);
            newOrder.put("customer", customerEmail);
            newOrder.put("product_id", productId);
            newOrder.put("product_name", productName);
            newOrder.put("product_image", productImage);
            newOrder.put("order_value", orderValue);
            newOrder.put("commission", commissionAmount);
            newOrder.put("status", "Đang xử lý");
            
            JSONArray ordersArr = userAffiliateObj.optJSONArray("orders");
            if (ordersArr == null) ordersArr = new JSONArray();
            ordersArr.put(newOrder);
            userAffiliateObj.put("orders", ordersArr);
            
            saveAffiliateData(context, affiliateArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String addWithdrawal(Context context, String userId, long amount) {
        String wdId = UUID.randomUUID().toString();
        try {
            JSONArray affiliateArray = getAffiliateData(context);
            JSONObject userAffiliateObj = null;
            for (int i = 0; i < affiliateArray.length(); i++) {
                JSONObject obj = affiliateArray.getJSONObject(i);
                if (obj.optString("user_id").equals(userId)) {
                    userAffiliateObj = obj;
                    break;
                }
            }
            if (userAffiliateObj == null) {
                userAffiliateObj = new JSONObject();
                userAffiliateObj.put("user_id", userId);
                userAffiliateObj.put("total_revenue", 0L);
                userAffiliateObj.put("total_commission", 0L);
                userAffiliateObj.put("pending_commission", 0L);
                userAffiliateObj.put("successful_orders", 0);
                userAffiliateObj.put("new_customers", 0);
                userAffiliateObj.put("orders", new JSONArray());
                userAffiliateObj.put("withdrawals", new JSONArray());
                affiliateArray.put(userAffiliateObj);
            }
            
            JSONArray withdrawalsArr = userAffiliateObj.optJSONArray("withdrawals");
            if (withdrawalsArr == null) withdrawalsArr = new JSONArray();
            
            JSONObject newWd = new JSONObject();
            newWd.put("id", wdId);
            newWd.put("amount", amount);
            newWd.put("status", "Đang xử lý");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
            newWd.put("date", sdf.format(new Date()));
            
            withdrawalsArr.put(newWd);
            userAffiliateObj.put("withdrawals", withdrawalsArr);
            saveAffiliateData(context, affiliateArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wdId;
    }

    public static void updateWithdrawalStatus(Context context, String userId, String withdrawalId, String status) {
        try {
            JSONArray affiliateArray = getAffiliateData(context);
            for (int i = 0; i < affiliateArray.length(); i++) {
                JSONObject obj = affiliateArray.getJSONObject(i);
                if (obj.optString("user_id").equals(userId)) {
                    JSONArray withdrawalsArr = obj.optJSONArray("withdrawals");
                    if (withdrawalsArr != null) {
                        for (int j = 0; j < withdrawalsArr.length(); j++) {
                            JSONObject wd = withdrawalsArr.getJSONObject(j);
                            if (wd.optString("id").equals(withdrawalId)) {
                                wd.put("status", status);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
            saveAffiliateData(context, affiliateArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
