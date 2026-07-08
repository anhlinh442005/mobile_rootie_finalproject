package com.veganbeauty.app.features.shop.store;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.features.account.notification.LocalSystemNotificationHelper;
import com.veganbeauty.app.utils.RootieLocationHelper;
import com.veganbeauty.app.utils.StoreCoordinateValidator;

import java.util.List;

public final class StoreProximityHelper {

    private static final String TAG = "StoreProximityHelper";
    private static final float PROXIMITY_RADIUS_METERS = 1500f;

    private StoreProximityHelper() {
    }

    public static final class NearestStoreResult {
        public final StoreEntity store;
        public final float distanceMeters;

        public NearestStoreResult(@NonNull StoreEntity store, float distanceMeters) {
            this.store = store;
            this.distanceMeters = distanceMeters;
        }
    }

    @Nullable
    public static NearestStoreResult findNearestStore(@NonNull Context context) {
        if (!hasLocationPermission(context)) {
            return null;
        }
        return findNearestStore(context, getBestLastKnownLocation(context));
    }

    @Nullable
    public static NearestStoreResult findNearestStore(@NonNull Context context, @Nullable Location location) {
        if (location == null) {
            return null;
        }

        List<StoreEntity> stores = new LocalJsonReader(context.getApplicationContext()).getAllStores();
        if (stores == null || stores.isEmpty()) {
            return null;
        }

        StoreEntity closestStore = null;
        float closestDistance = Float.MAX_VALUE;
        for (StoreEntity store : stores) {
            if (store == null || !store.isActive()) {
                continue;
            }
            if (store.getLat() == 0d && store.getLng() == 0d) {
                continue;
            }
            if (!StoreCoordinateValidator.isPlausible(store)) {
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
            return null;
        }
        return new NearestStoreResult(closestStore, closestDistance);
    }

    @NonNull
    public static String getStoreShortLabel(@Nullable StoreEntity store) {
        if (store == null) {
            return "Rootie";
        }
        String ten = store.getTenCuaHang();
        if (ten != null) {
            int idx = ten.lastIndexOf("Cơ sở ");
            if (idx >= 0) {
                return ten.substring(idx).trim();
            }
        }
        String ma = store.getMaCuaHang();
        if (ma != null && ma.startsWith("CH")) {
            try {
                int num = Integer.parseInt(ma.substring(2));
                return "Cơ sở " + num;
            } catch (NumberFormatException ignored) {
            }
        }
        return ten != null && !ten.isEmpty() ? ten : "Rootie";
    }

    @NonNull
    public static String formatNearestAreaLabel(@NonNull NearestStoreResult result) {
        return formatNearestAreaLabel(result, null);
    }

    @NonNull
    public static String formatNearestAreaLabel(@NonNull NearestStoreResult result, @Nullable Location userLocation) {
        String label = getStoreShortLabel(result.store);
        String distanceText;
        if (result.distanceMeters < 1000f) {
            distanceText = "Bạn ở gần " + label + " nhất (~" + Math.round(result.distanceMeters) + " m)";
        } else {
            float km = result.distanceMeters / 1000f;
            distanceText = "Bạn ở gần " + label + " nhất (~"
                    + String.format(java.util.Locale.getDefault(), "%.1f", km) + " km)";
        }
        if (userLocation != null && userLocation.hasAccuracy() && userLocation.getAccuracy() > 0f) {
            distanceText += " • GPS ±" + Math.round(userLocation.getAccuracy()) + " m";
        }
        return distanceText;
    }

    public static boolean hasLocationPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    public static Location getBestLastKnownLocation(@NonNull Context context) {
        Location accurate = RootieLocationHelper.getBestAvailableLocation(context);
        if (accurate != null) {
            return accurate;
        }
        return getLegacyBestLastKnownLocation(context);
    }

    @Nullable
    private static Location getLegacyBestLastKnownLocation(@NonNull Context context) {
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

        NearestStoreResult nearest = findNearestStore(appContext, location);
        if (nearest == null) {
            return;
        }

        boolean isMockLocation = location.getLatitude() == 10.775 && location.getLongitude() == 106.701;
        if (nearest.distanceMeters >= PROXIMITY_RADIUS_METERS && !isMockLocation) {
            return;
        }

        notifyNearStore(appContext, nearest.store);
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

}
