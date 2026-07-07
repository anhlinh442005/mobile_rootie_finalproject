package com.veganbeauty.app.features.shop.store;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.features.account.notification.LocalSystemNotificationHelper;

import java.util.List;

public final class StoreProximityHelper {

    private static final String TAG = "StoreProximityHelper";
    private static final float PROXIMITY_RADIUS_METERS = 1500f;

    private StoreProximityHelper() {
    }

    public static void checkIfNearStore(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        if (!ProfileSession.isNotiEnabled(appContext) || !ProfileSession.isPromotionEnabled(appContext)) {
            return;
        }
        if (!LocalSystemNotificationHelper.canPost(appContext)) {
            return;
        }
        if (!hasLocationPermission(appContext)) {
            return;
        }

        Location location = getBestLastKnownLocation(appContext);
        if (location == null) {
            Log.d(TAG, "No last known location available for proximity check");
            return;
        }

        List<StoreEntity> stores = new LocalJsonReader(appContext).getAllStores();
        if (stores == null || stores.isEmpty()) {
            return;
        }

        StoreEntity closestStore = null;
        float closestDistance = Float.MAX_VALUE;
        for (StoreEntity store : stores) {
            if (store == null) {
                continue;
            }
            float[] results = new float[1];
            Location.distanceBetween(
                    location.getLatitude(),
                    location.getLongitude(),
                    store.getLat(),
                    store.getLng(),
                    results
            );
            float distance = results[0];
            if (distance < closestDistance) {
                closestDistance = distance;
                closestStore = store;
            }
        }

        if (closestStore == null) {
            return;
        }

        boolean isMockLocation = location.getLatitude() == 10.775 && location.getLongitude() == 106.701;
        if (closestDistance >= PROXIMITY_RADIUS_METERS && !isMockLocation) {
            return;
        }

        notifyNearStore(appContext, closestStore);
    }

    public static void notifyNearStore(Context context, StoreEntity store) {
        if (context == null || store == null || store.getId() == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        if (!ProfileSession.isNotiEnabled(appContext) || !ProfileSession.isPromotionEnabled(appContext)) {
            return;
        }
        if (!LocalSystemNotificationHelper.canPost(appContext)) {
            return;
        }

        String storeName = store.getTenCuaHang() != null ? store.getTenCuaHang() : "Rootie";
        String title = "Ưu đãi gần bạn!";
        String message = "Bạn đang ở gần cửa hàng " + storeName + ", vào săn deal ngay!";
        String stableId = LocalSystemNotificationHelper.dailyStableId("store_proximity_" + store.getId());

        LocalSystemNotificationHelper.dispatch(
                appContext,
                stableId,
                title,
                message,
                "Khuyến mãi",
                "promotion",
                null,
                "XEM NGAY"
        );
    }

    private static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static Location getBestLastKnownLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }

        Location best = null;
        String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER};
        for (String provider : providers) {
            try {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate == null) {
                    continue;
                }
                if (best == null || candidate.getTime() > best.getTime()) {
                    best = candidate;
                }
            } catch (SecurityException ignored) {
                return null;
            }
        }
        return best;
    }
}
