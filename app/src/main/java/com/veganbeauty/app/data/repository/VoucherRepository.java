package com.veganbeauty.app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.VoucherEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.features.profile.VoucherListAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VoucherRepository {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface VoucherCallback {
        void onLoaded(List<VoucherEntity> vouchers);
    }

    private VoucherRepository() {
    }

    public static void seedToFirestoreIfEmpty(Context context) {
        EXECUTOR.execute(() -> {
            try {
                FirestoreService firestore = new FirestoreService();
                if (firestore.isCollectionEmpty("vouchers")) {
                    String json = new LocalJsonReader(context.getApplicationContext()).readAsset("vouchers.json");
                    if (json != null) {
                        firestore.seedVouchersFromJson(json);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static List<VoucherEntity> loadActiveVouchersBlocking(Context context) {
        FirestoreService firestore = new FirestoreService();
        List<VoucherEntity> remote = firestore.fetchAllVouchers();
        if (!remote.isEmpty()) {
            return remote;
        }
        return new LocalJsonReader(context.getApplicationContext()).getVouchers();
    }

    public static void loadActiveVouchers(Context context, VoucherCallback callback) {
        EXECUTOR.execute(() -> {
            List<VoucherEntity> vouchers = loadActiveVouchersBlocking(context);
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onLoaded(vouchers));
            }
        });
    }

    public static List<VoucherListAdapter.VoucherItem> toListAdapterItems(List<VoucherEntity> entities) {
        List<VoucherListAdapter.VoucherItem> items = new ArrayList<>();
        if (entities == null) return items;
        for (VoucherEntity entity : entities) {
            if (!entity.isActive()) continue;
            String status = computeStatusFromExpiry(entity.getExpiryDate());
            items.add(new VoucherListAdapter.VoucherItem(
                    entity.getId(),
                    entity.getTitle(),
                    entity.getDescription(),
                    entity.getCode(),
                    status,
                    entity.getExpiryDate(),
                    entity.getType(),
                    false,
                    entity.getQuantity(),
                    (int) entity.getMinOrderValue(),
                    "Tất cả sản phẩm",
                    entity.getOfferType(),
                    (int) entity.getDiscountValue()
            ));
        }
        return items;
    }

    public static List<com.veganbeauty.app.features.profile.VoucherItem> toParcelableItems(List<VoucherEntity> entities) {
        List<com.veganbeauty.app.features.profile.VoucherItem> items = new ArrayList<>();
        if (entities == null) return items;
        for (VoucherEntity entity : entities) {
            if (!entity.isActive()) continue;
            String status = computeStatusFromExpiry(entity.getExpiryDate());
            items.add(new com.veganbeauty.app.features.profile.VoucherItem(
                    entity.getId(),
                    entity.getTitle(),
                    entity.getDescription(),
                    entity.getCode(),
                    status,
                    entity.getExpiryDate(),
                    entity.getType(),
                    false,
                    entity.getQuantity() != null ? entity.getQuantity() : 0,
                    entity.getMinOrderValue(),
                    "Tất cả sản phẩm",
                    entity.getOfferType(),
                    entity.getDiscountValue()
            ));
        }
        return items;
    }

    public static String computeStatusFromExpiry(String expiryStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date expiryDate = sdf.parse(expiryStr);
            if (expiryDate == null) return "valid";

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar expiry = Calendar.getInstance();
            expiry.setTime(expiryDate);
            expiry.set(Calendar.HOUR_OF_DAY, 0);
            expiry.set(Calendar.MINUTE, 0);
            expiry.set(Calendar.SECOND, 0);
            expiry.set(Calendar.MILLISECOND, 0);

            if (expiry.before(today)) {
                return "expired";
            } else if (expiry.equals(today)) {
                return "expiring";
            }
            return "valid";
        } catch (Exception e) {
            return "valid";
        }
    }
}
