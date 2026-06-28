package com.veganbeauty.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.features.community.affiliate.AffiliateHelper;

import org.json.JSONObject;

import java.util.List;

public final class AffiliateTrackingHelper {

    private static final String PREFS_NAME = "affiliate_prefs";
    private static final double COMMISSION_RATE = 0.05;

    private AffiliateTrackingHelper() {}

    public static class Attribution {
        public final String referrerUserId;
        public final String affiliateCode;
        public final String sourcePostId;
        public final String productId;

        public Attribution(String referrerUserId, String affiliateCode, String sourcePostId, String productId) {
            this.referrerUserId = referrerUserId;
            this.affiliateCode = affiliateCode;
            this.sourcePostId = sourcePostId;
            this.productId = productId;
        }
    }

    @Nullable
    public static Attribution findAttribution(Context context, List<CartItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        for (CartItemEntity item : items) {
            String raw = prefs.getString(item.getId(), null);
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            try {
                JSONObject json = new JSONObject(raw);
                String referrerUserId = json.optString("referrerUserId", "").trim();
                if (referrerUserId.isEmpty()) {
                    continue;
                }
                return new Attribution(
                        referrerUserId,
                        json.optString("affiliateCode", ""),
                        json.optString("sourcePostId", ""),
                        item.getId()
                );
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static void applyAffiliateAttribution(Context context, OrderEntity order, List<CartItemEntity> items) {
        Attribution attribution = findAttribution(context, items);
        if (attribution == null || order == null) {
            return;
        }

        long commissionBase = order.getSubTotal() > 0 ? order.getSubTotal() : order.getTotalAmount();
        long commissionAmount = Math.round(commissionBase * COMMISSION_RATE);

        OrderEntity.AffiliateInfo affiliateInfo = new OrderEntity.AffiliateInfo();
        affiliateInfo.setAffiliate_id(
                attribution.affiliateCode != null && !attribution.affiliateCode.isEmpty()
                        ? attribution.affiliateCode
                        : "AFF_" + attribution.referrerUserId + "_" + attribution.productId
        );
        affiliateInfo.setReferrerUserId(attribution.referrerUserId);
        affiliateInfo.setCommissionAmount(commissionAmount);
        affiliateInfo.setCommissionStatus("pending");

        order.setAffiliate(true);
        order.setAffiliate(affiliateInfo);
    }

    public static void recordAffiliateSideEffects(Context context, OrderEntity order) {
        if (order == null || !order.isAffiliate() || order.getAffiliate() == null) {
            return;
        }
        OrderEntity.AffiliateInfo affiliate = order.getAffiliate();
        List<OrderEntity.OrderItem> items = order.getItems();
        OrderEntity.OrderItem firstItem = items != null && !items.isEmpty() ? items.get(0) : null;

        AffiliateHelper.addAffiliateOrder(
                context.getApplicationContext(),
                affiliate.getReferrerUserId(),
                firstItem != null ? firstItem.getProductId() : "",
                firstItem != null ? firstItem.getProductName() : "Sản phẩm affiliate",
                firstItem != null ? firstItem.getProductImage() : "",
                order.getTotalAmount(),
                affiliate.getCommissionAmount(),
                order.getBillingEmail() != null ? order.getBillingEmail() : order.getShippingName()
        );
    }

    public static void clearAttributionForItems(Context context, List<CartItemEntity> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (CartItemEntity item : items) {
            editor.remove(item.getId());
        }
        editor.apply();
    }
}
