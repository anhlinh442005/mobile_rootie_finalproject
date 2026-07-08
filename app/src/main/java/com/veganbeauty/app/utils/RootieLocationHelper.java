package com.veganbeauty.app.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

public final class RootieLocationHelper {

    private static final String TAG = "RootieLocationHelper";
    private static final long STALE_LOCATION_MS = 3 * 60 * 1000L;
    private static final float POOR_ACCURACY_METERS = 400f;

    @Nullable
    private static volatile Location cachedAccurateLocation;
    @Nullable
    private static LocationCallback activeCallback;

    private RootieLocationHelper() {
    }

    public interface AccurateLocationListener {
        void onLocation(@Nullable Location location);
    }

    public static boolean hasLocationPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isLocationEnabled(@NonNull Context context) {
        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            return false;
        }
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Lấy vị trí GPS mới nhất với độ chính xác cao (Fused Location).
     */
    public static void requestAccurateLocation(@NonNull Context context, @NonNull AccurateLocationListener listener) {
        Context appContext = context.getApplicationContext();
        if (!hasLocationPermission(appContext)) {
            listener.onLocation(null);
            return;
        }

        Location cached = getCachedIfFresh();
        if (cached != null) {
            listener.onLocation(cached);
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(appContext);
        CancellationTokenSource tokenSource = new CancellationTokenSource();
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    Location resolved = resolveBestLocation(cached, location);
                    if (resolved != null) {
                        cacheLocation(resolved);
                        listener.onLocation(resolved);
                        return;
                    }
                    fetchLastKnownFused(appContext, client, cached, listener);
                })
                .addOnFailureListener(error -> {
                    Log.d(TAG, "getCurrentLocation failed: " + error.getMessage());
                    fetchLastKnownFused(appContext, client, cached, listener);
                });
    }

    public static void startHighAccuracyUpdates(@NonNull Context context, @NonNull AccurateLocationListener listener) {
        Context appContext = context.getApplicationContext();
        if (!hasLocationPermission(appContext)) {
            listener.onLocation(null);
            return;
        }

        stopHighAccuracyUpdates(appContext);

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(appContext);
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(2f)
                .setMaxUpdateDelayMillis(4000L)
                .setWaitForAccurateLocation(true)
                .build();

        activeCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (!isUsable(location)) {
                    return;
                }
                cacheLocation(location);
                listener.onLocation(location);
            }
        };

        try {
            client.requestLocationUpdates(request, activeCallback, Looper.getMainLooper());
        } catch (SecurityException securityException) {
            Log.d(TAG, "requestLocationUpdates denied");
            listener.onLocation(getBestAvailableLocation(appContext));
        }
    }

    public static void stopHighAccuracyUpdates(@NonNull Context context) {
        if (activeCallback == null) {
            return;
        }
        try {
            LocationServices.getFusedLocationProviderClient(context.getApplicationContext())
                    .removeLocationUpdates(activeCallback);
        } catch (Exception ignored) {
        }
        activeCallback = null;
    }

    @Nullable
    public static Location getBestAvailableLocation(@NonNull Context context) {
        Location cached = getCachedIfFresh();
        if (cached != null) {
            return cached;
        }
        return getLegacyBestLastKnown(context);
    }

    @Nullable
    private static Location getCachedIfFresh() {
        Location cached = cachedAccurateLocation;
        if (cached == null) {
            return null;
        }
        long ageMs = System.currentTimeMillis() - cached.getTime();
        if (ageMs > STALE_LOCATION_MS) {
            return null;
        }
        return new Location(cached);
    }

    private static void cacheLocation(@NonNull Location location) {
        cachedAccurateLocation = new Location(location);
    }

    private static void fetchLastKnownFused(
            @NonNull Context appContext,
            @NonNull FusedLocationProviderClient client,
            @Nullable Location cached,
            @NonNull AccurateLocationListener listener
    ) {
        try {
            client.getLastLocation()
                    .addOnSuccessListener(location -> {
                        Location resolved = resolveBestLocation(cached, location);
                        if (resolved == null) {
                            resolved = getLegacyBestLastKnown(appContext);
                        }
                        if (resolved != null) {
                            cacheLocation(resolved);
                        }
                        listener.onLocation(resolved);
                    })
                    .addOnFailureListener(error -> {
                        Location resolved = resolveBestLocation(cached, getLegacyBestLastKnown(appContext));
                        listener.onLocation(resolved);
                    });
        } catch (SecurityException securityException) {
            listener.onLocation(resolveBestLocation(cached, getLegacyBestLastKnown(appContext)));
        }
    }

    @Nullable
    private static Location resolveBestLocation(@Nullable Location first, @Nullable Location second) {
        if (!isUsable(first)) {
            return isUsable(second) ? new Location(second) : null;
        }
        if (!isUsable(second)) {
            return new Location(first);
        }
        if (second.getTime() > first.getTime() + 15_000L) {
            return new Location(second);
        }
        if (first.hasAccuracy() && second.hasAccuracy() && second.getAccuracy() + 20f < first.getAccuracy()) {
            return new Location(second);
        }
        return new Location(first);
    }

    private static boolean isUsable(@Nullable Location location) {
        if (location == null) {
            return false;
        }
        if (location.getLatitude() == 0d && location.getLongitude() == 0d) {
            return false;
        }
        long ageMs = System.currentTimeMillis() - location.getTime();
        if (ageMs > 10 * 60 * 1000L) {
            return false;
        }
        return !location.hasAccuracy() || location.getAccuracy() <= POOR_ACCURACY_METERS;
    }

    @Nullable
    private static Location getLegacyBestLastKnown(@NonNull Context context) {
        if (!hasLocationPermission(context)) {
            return null;
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }

        Location best = null;
        String[] providers = {
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
        };
        for (String provider : providers) {
            try {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate == null) {
                    continue;
                }
                best = resolveBestLocation(best, candidate);
            } catch (SecurityException ignored) {
                return null;
            }
        }
        return best;
    }
}
