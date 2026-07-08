package com.veganbeauty.app.features.weather;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.SkinWeatherForecastBinding;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;
import com.veganbeauty.app.features.ai.SkinAiChatFragment;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;
import com.veganbeauty.app.utils.AvatarLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class SkinWeatherForecastFragment extends RootieFragment {

    private SkinWeatherForecastBinding _binding;
    private WeatherHeaderScrollHelper headerScrollHelper;
    private SkinWeatherForecastBinding getBinding() {
        return _binding;
    }

    private final String GEMINI_API_KEY = com.veganbeauty.app.BuildConfig.GEMINI_API_KEY;
    private final String WAQI_API_KEY = com.veganbeauty.app.BuildConfig.WAQI_API_KEY;
    private final double defaultLat = 10.8231;
    private final double defaultLng = 106.6297;
    private boolean isViewingHistory = false;
    private int weatherRequestId = 0;
    private int geminiRequestId = 0;
    private LocationManager activeLocationManager;
    private LocationListener activeLocationListener;

    private static final class WeatherSnapshot {
        final double temp;
        final int humidity;
        final double uv;
        final double pm25;
        final int usAqi;
        final boolean success;
        final boolean hasPm25;
        final int weatherCode;
        final String pm25Source;
        final String pm25Station;

        WeatherSnapshot(double temp, int humidity, double uv, double pm25, int usAqi,
                        boolean success, boolean hasPm25, int weatherCode,
                        String pm25Source, String pm25Station) {
            this.temp = temp;
            this.humidity = humidity;
            this.uv = uv;
            this.pm25 = pm25;
            this.usAqi = usAqi;
            this.success = success;
            this.hasPm25 = hasPm25;
            this.weatherCode = weatherCode;
            this.pm25Source = pm25Source;
            this.pm25Station = pm25Station;
        }

        static WeatherSnapshot fromSaved(SkinWeatherSnapshotManager.Snapshot saved) {
            return new WeatherSnapshot(
                    saved.temp, saved.humidity, saved.uv, saved.pm25, saved.usAqi,
                    saved.weatherSuccess, saved.hasPm25, saved.weatherCode,
                    saved.pm25Source, saved.pm25Station
            );
        }
    }

    private void runOnUiThreadSafe(Runnable action) {
        if (!isAdded() || _binding == null) return;
        androidx.fragment.app.FragmentActivity activity = getActivity();
        if (activity == null) return;
        activity.runOnUiThread(() -> {
            if (!isAdded() || _binding == null) return;
            try {
                action.run();
            } catch (Exception e) {
                android.util.Log.e("SkinWeatherForecast", "UI update failed", e);
            }
        });
    }

    private final ActivityResultLauncher<String[]> requestLocationLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                Boolean fineGranted = permissions.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseGranted = permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION);

                if ((fineGranted != null && fineGranted) || (coarseGranted != null && coarseGranted)) {
                    Toast.makeText(getContext(), "Đã cấp quyền định vị thành công!", Toast.LENGTH_SHORT).show();
                    getCurrentLocationAndLoadWeather();
                } else {
                    Toast.makeText(getContext(), "Quyền định vị bị từ chối. Sử dụng vị trí mặc định: TP. Hồ Chí Minh.", Toast.LENGTH_LONG).show();
                    loadWeatherForCoordinates(defaultLat, defaultLng);
                }
            }
    );

    private final ActivityResultLauncher<String> requestNotiPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (!isAdded()) return;
                if (granted) {
                    sendWeatherNotificationPreview(true);
                } else {
                    Toast.makeText(requireContext(), "Cần bật quyền thông báo để nhận lời khuyên thời tiết & da", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinWeatherForecastBinding.inflate(inflater, container, false);
        return getBinding().getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        setupToolbar();
        setupScrollHideHeader();
        setupBottomNavigation();
        loadUserProfileData();
        loadCachedWeatherSnapshot();
        checkLocationPermissionsAndLoad();
        setupFeedbackButtons();

        getBinding().cardAiInsight.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new SkinAiChatFragment())
                    .addToBackStack(null)
                    .commit();
        });

        getBinding().btnExitHistory.setOnClickListener(v -> {
            isViewingHistory = false;
            getBinding().layoutHistoryBanner.setVisibility(View.GONE);
            checkLocationPermissionsAndLoad();
        });

        setupSkinWeatherNotificationSwitch();
    }

    private void setupSkinWeatherNotificationSwitch() {
        Context context = requireContext();
        boolean isEnabled = ProfileSession.isSkinWeatherNotiEnabled(context);
        updateSwitchUI(getBinding().switchSkinWeatherForecast, getBinding().switchSkinWeatherForecastThumb, isEnabled);

        getBinding().btnToggleSkinWeatherNoti.setOnClickListener(v -> {
            boolean nextState = !ProfileSession.isSkinWeatherNotiEnabled(context);
            if (nextState && !areNotificationsAllowed(context)) {
                requestNotificationPermissionThenPreview();
                ProfileSession.setSkinWeatherNotiEnabled(context, true);
                updateSwitchUI(getBinding().switchSkinWeatherForecast, getBinding().switchSkinWeatherForecastThumb, true);
                DailySkinWeatherScheduler.enableAndSync(context);
                return;
            }
            ProfileSession.setSkinWeatherNotiEnabled(context, nextState);
            updateSwitchUI(getBinding().switchSkinWeatherForecast, getBinding().switchSkinWeatherForecastThumb, nextState);
            if (nextState) {
                DailySkinWeatherScheduler.enableAndSync(context);
                Toast.makeText(context, "Đã bật thông báo thời tiết & da lúc 07:00 sáng mỗi ngày", Toast.LENGTH_SHORT).show();
            } else {
                DailySkinWeatherScheduler.cancelDailyNotification(context);
                Toast.makeText(context, "Đã tắt thông báo thời tiết và da hàng ngày", Toast.LENGTH_SHORT).show();
            }
        });

        getBinding().btnTestWeatherNoti.setOnClickListener(v -> {
            if (!ProfileSession.isSkinWeatherNotiEnabled(context)) {
                Toast.makeText(context, "Hãy bật thông báo thời tiết & da trước", Toast.LENGTH_SHORT).show();
                return;
            }
            requestNotificationPermissionThenPreview();
        });
    }

    private void requestNotificationPermissionThenPreview() {
        Context context = requireContext();
        if (!ProfileSession.isNotiEnabled(context)) {
            Toast.makeText(context, "Hãy bật thông báo trong Cài đặt tài khoản trước", Toast.LENGTH_LONG).show();
            return;
        }
        if (areNotificationsAllowed(context)) {
            sendWeatherNotificationPreview(true);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotiPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            Toast.makeText(context, "Vui lòng bật thông báo cho Rootie trong Cài đặt hệ thống", Toast.LENGTH_LONG).show();
        }
    }

    private boolean areNotificationsAllowed(Context context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void sendWeatherNotificationPreview(boolean force) {
        Context context = requireContext();
        if (!areNotificationsAllowed(context)) {
            return;
        }
        Toast.makeText(context, "Đang gửi thông báo thử...", Toast.LENGTH_SHORT).show();
        DailySkinWeatherScheduler.triggerNow(context, force);
    }

    private void updateSwitchUI(FrameLayout container, ImageView thumb, boolean enabled) {
        if (enabled) {
            container.setBackgroundResource(R.drawable.ic_switch_track_on);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.END;
            lp.setMarginStart(0);
            lp.setMarginEnd((int) (2 * getResources().getDisplayMetrics().density));
            thumb.setLayoutParams(lp);
        } else {
            container.setBackgroundResource(R.drawable.ic_switch_track_off);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START;
            lp.setMarginEnd(0);
            lp.setMarginStart((int) (2 * getResources().getDisplayMetrics().density));
            thumb.setLayoutParams(lp);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (_binding != null) {
            Context context = requireContext();
            boolean isEnabled = ProfileSession.isSkinWeatherNotiEnabled(context);
            updateSwitchUI(getBinding().switchSkinWeatherForecast, getBinding().switchSkinWeatherForecastThumb, isEnabled);
            loadUserProfileData();
        }
    }

    private void loadCachedWeatherSnapshot() {
        SkinWeatherSnapshotManager.loadFromFirestore(requireContext(), cached -> runOnUiThreadSafe(() -> {
            if (!isAdded() || _binding == null || cached == null || isViewingHistory) return;
            if (cached.isFresh(30L * 60L * 1000L)) {
                updateWeatherUI(
                        WeatherSnapshot.fromSaved(cached),
                        cached.city,
                        cached.lat,
                        cached.lng
                );
            }
        }));
    }

    private void setupScrollHideHeader() {
        int bottomPad = (int) getResources().getDimension(R.dimen.home_nav_bar_height);
        headerScrollHelper = new WeatherHeaderScrollHelper(
                getBinding().layoutWeatherHeader,
                getBinding().scrollContent,
                bottomPad
        );
        headerScrollHelper.attach();
    }

    private void setupToolbar() {
        getBinding().btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        getBinding().btnHistory.setOnClickListener(v -> showHistoryDialog());
        getBinding().btnNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadUserProfileData() {
        String username = ProfileSession.getFullName(requireContext());

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour <= 10) greeting = "CHÀO BUỔI SÁNG";
        else if (hour >= 11 && hour <= 13) greeting = "CHÀO BUỔI TRƯA";
        else if (hour >= 14 && hour <= 17) greeting = "CHÀO BUỔI CHIỀU";
        else greeting = "CHÀO BUỔI TỐI";

        getBinding().tvGreeting.setText(greeting);
        getBinding().tvUsername.setText(username);

        String avatarUrl = ProfileSession.getAvatar(requireContext());
        AvatarLoader.loadAvatar(getBinding().ivAvatar, avatarUrl);
    }

    private void updateRoutineLabels(SkinWeatherProfileHelper.UserSkinProfile skinProfile, double temp, int humidity, double uv, int pm25) {
        if (!isAdded() || _binding == null) return;
        try {
        String skinType = skinProfile.skinType;
        String skinLower = skinType.toLowerCase();
        boolean isOily = skinLower.contains("dầu");
        boolean isSensitive = skinLower.contains("nhạy cảm") || skinLower.contains("kích ứng");
        boolean isDry = skinLower.contains("khô");
        boolean isCombination = skinLower.contains("hỗn hợp");
        boolean isAging = skinLower.contains("lão hóa");
        boolean isDehydrated = skinLower.contains("mất nước");

        Map<String, SkinWeatherProductMatcher.ProductMatch> matchedProducts =
                SkinWeatherProductMatcher.matchProductsForWeatherAndSkin(
                        requireContext(), temp, humidity, skinType, skinProfile.flaggedGroups);

        // 1. Cleanser
        SkinWeatherProductMatcher.ProductMatch matchedCleanser = matchedProducts.get("Cleanser");
        String cleanserName = matchedCleanser != null ? matchedCleanser.getName() : 
            (isOily ? "Gel rửa mặt BHA/Salicylic" :
             isSensitive ? "Sữa rửa mặt không bọt" :
             isDry ? "Sữa rửa mặt cấp ẩm" :
             isCombination ? "Gel rửa mặt cân bằng" :
             isAging ? "Sữa rửa mặt chống lão hóa" :
             isDehydrated ? "Sữa rửa mặt Amino Acid" : "Sữa rửa mặt cân bằng pH");

        String cleanserSub = matchedCleanser != null ? matchedCleanser.getNotes() + " (Phù hợp: " + matchedCleanser.getSuitabilityScore() + "%)" :
            (isOily ? "Kiềm dầu & làm sạch sâu bã nhờn" :
             isSensitive ? "Cực kỳ dịu nhẹ cho da nhạy cảm" :
             isDry ? "Làm sạch dịu nhẹ, giữ ẩm tự nhiên" :
             isCombination ? "Sạch sâu vùng chữ T, dịu nhẹ vùng má" :
             isAging ? "Sạch sâu nhẹ nhàng kèm tinh chất phục hồi" :
             isDehydrated ? "Không làm khô căng da sau khi rửa" : "Duy trì độ pH sinh lý lý tưởng cho da");

        String finalCleanserSub = pm25 >= 0 && pm25 > 55 ? cleanserSub + " & sạch sâu bụi mịn PM2.5" : cleanserSub;

        // 2. Serum
        SkinWeatherProductMatcher.ProductMatch matchedSerum = matchedProducts.get("Serum");
        String serumName = matchedSerum != null ? matchedSerum.getName() :
            (isOily ? "Niacinamide 10% Serum" :
             isSensitive ? "Serum phục hồi B5" :
             isDry ? "Hyaluronic Acid (HA) Serum" :
             isCombination ? "Serum HA + Niacinamide" :
             isAging ? "Retinol / Peptide Serum" :
             isDehydrated ? "Serum HA cấp nước sâu" : "Vitamin C Serum");

        String serumSub = matchedSerum != null ? matchedSerum.getNotes() + " (Phù hợp: " + matchedSerum.getSuitabilityScore() + "%)" :
            (isOily ? "Thu nhỏ lỗ chân lông, điều tiết dầu" :
             isSensitive ? "Làm dịu kích ứng, phục hồi hàng rào da" :
             isDry ? "Cấp nước đa tầng, căng mọng da" :
             isCombination ? "Cân bằng dầu nước tối ưu" :
             isAging ? "Tăng sinh collagen, mờ nếp nhăn" :
             isDehydrated ? "Bơm nước căng mọng tế bào da" : "Làm sáng và đều màu da tự nhiên");

        // 3. Moisturizer
        SkinWeatherProductMatcher.ProductMatch matchedMoisturizer = matchedProducts.get("Moisturizer");
        String moisturizerName = matchedMoisturizer != null ? matchedMoisturizer.getName() :
            (isOily ? "Dưỡng ẩm dạng Gel" :
             isSensitive ? "Kem dưỡng Ceramide phục hồi" :
             isDry ? "Kem dưỡng ẩm Cream" :
             isCombination ? "Lotion dưỡng ẩm mỏng nhẹ" :
             isAging ? "Kem dưỡng săn chắc da" :
             isDehydrated ? "Gel-cream khóa nước" : "Lotion dưỡng ẩm nhẹ");

        String moisturizerSub = matchedMoisturizer != null ? matchedMoisturizer.getNotes() + " (Phù hợp: " + matchedMoisturizer.getSuitabilityScore() + "%)" :
            (isOily ? "Thấm nhanh, mỏng nhẹ, không bóng nhờn" :
             isSensitive ? "Củng cố lớp màng bảo vệ tự nhiên" :
             isDry ? "Khóa ẩm sâu, ngăn ngừa bong tróc da" :
             isCombination ? "Cấp ẩm đầy đủ mà không gây bí da" :
             isAging ? "Nuôi dưỡng sâu, tăng cường độ đàn hồi" :
             isDehydrated ? "Khóa nước dưới da mà không bết dính" : "Lotion dưỡng ẩm nhẹ");

        String finalMoisturizerSub = humidity < 40 ? moisturizerSub + " (Khóa ẩm tăng cường vì thời tiết khô)" :
                                     temp >= 33 ? moisturizerSub + " (Thoa lớp mỏng nhẹ tránh bí tắc ngày nóng)" : moisturizerSub;

        // 4. Sunscreen
        SkinWeatherProductMatcher.ProductMatch matchedSunscreen = matchedProducts.get("Sunscreen");
        String sunscreenName = matchedSunscreen != null ? matchedSunscreen.getName() :
            (isOily ? "Kem chống nắng kiềm dầu" :
             isSensitive ? "KCN vật lý thuần chay" :
             isDry ? "Kem chống nắng dưỡng ẩm" :
             isCombination ? "KCN kiềm dầu dịu nhẹ" :
             isAging ? "Kem chống nắng chống lão hóa" :
             isDehydrated ? "Kem chống nắng cấp nước" : "Kem chống nắng phổ rộng");

        String sunscreenSub = matchedSunscreen != null ? matchedSunscreen.getNotes() + " (Phù hợp: " + matchedSunscreen.getSuitabilityScore() + "%)" :
            (isOily ? "Finish khô thoáng, không bít tắc" :
             isSensitive ? "Chống nắng dịu nhẹ, không cay mắt" :
             isDry ? "Bảo vệ da khô khỏi mất nước" :
             isCombination ? "Không gây mụn vùng chữ T" :
             isAging ? "Ngăn ngừa sạm nám do tia UV" :
             isDehydrated ? "Vừa bảo vệ vừa cấp nước làm dịu da" : "Bảo vệ toàn diện trước tia UVA & UVB");

        String finalSunscreenSub = uv >= 8.0 ? sunscreenSub + " (Tia UV nguy hiểm: " + String.format(Locale.US, "%.1f", uv) + " - thoa lại sau mỗi 2h)" :
                                   uv >= 5.0 ? sunscreenSub + " (Tia UV cao: " + String.format(Locale.US, "%.1f", uv) + " - thoa lại sau mỗi 3h)" : sunscreenSub;

        setupRoutineCard(getBinding().layoutRoutineItem1, cleanserName, finalCleanserSub, matchedProducts.get("Cleanser"));
        setupRoutineCard(getBinding().layoutRoutineItem2, serumName, serumSub, matchedProducts.get("Serum"));
        setupRoutineCard(getBinding().layoutRoutineItem3, moisturizerName, finalMoisturizerSub, matchedProducts.get("Moisturizer"));
        setupRoutineCard(getBinding().layoutRoutineItem4, sunscreenName, finalSunscreenSub, matchedProducts.get("Sunscreen"));
        } catch (Exception e) {
            android.util.Log.w("SkinWeatherForecast", "Routine label update failed", e);
        }
    }

    private void setupRoutineCard(View card, String name, String sub, SkinWeatherProductMatcher.ProductMatch product) {
        if (card instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) card;
            if (vg.getChildCount() > 1 && vg.getChildAt(1) instanceof LinearLayout) {
                LinearLayout titleLayout = (LinearLayout) vg.getChildAt(1);
                if (titleLayout.getChildCount() > 0 && titleLayout.getChildAt(0) instanceof TextView) {
                    ((TextView) titleLayout.getChildAt(0)).setText(name);
                }
                if (titleLayout.getChildCount() > 1 && titleLayout.getChildAt(1) instanceof TextView) {
                    ((TextView) titleLayout.getChildAt(1)).setText(sub);
                }
            }
        }
        card.setOnClickListener(v -> navigateToProductDetail(product != null ? product.getId() : null, name));
    }

    private void navigateToProductDetail(String productId, String productName) {
        final Context appContext = getContext();
        if (appContext == null) return;
        new Thread(() -> {
            ProductEntity product = null;
            try {
                if (productId != null && !productId.isEmpty()) {
                    product = RootieDatabase.getDatabase(appContext).productDao().getProductById(productId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final ProductEntity resolved = product;
            runOnUiThreadSafe(() -> {
                if (resolved != null) {
                    ProductDetailLauncher.open(this, resolved);
                } else {
                    Toast.makeText(getContext(), "Sản phẩm không có sẵn trên cửa hàng!", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void checkLocationPermissionsAndLoad() {
        int finePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION);

        if (finePermission == PackageManager.PERMISSION_GRANTED || coarsePermission == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndLoadWeather();
        } else {
            requestLocationLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void stopLocationUpdates() {
        if (activeLocationManager != null && activeLocationListener != null) {
            try {
                activeLocationManager.removeUpdates(activeLocationListener);
            } catch (Exception ignored) {
            }
        }
        activeLocationManager = null;
        activeLocationListener = null;
    }

    private void getCurrentLocationAndLoadWeather() {
        try {
            LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Location location = null;
            if (isNetworkEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (location == null && isGpsEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (location != null) {
                loadWeatherForCoordinates(location.getLatitude(), location.getLongitude());
            } else {
                loadWeatherForCoordinates(defaultLat, defaultLng);

                stopLocationUpdates();
                activeLocationManager = locationManager;
                activeLocationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location loc) {
                        loadWeatherForCoordinates(loc.getLatitude(), loc.getLongitude());
                        stopLocationUpdates();
                    }
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}
                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                };

                List<String> providers = locationManager.getProviders(true);
                for (String provider : providers) {
                    locationManager.requestLocationUpdates(provider, 0L, 0f, activeLocationListener, Looper.getMainLooper());
                }
            }
        } catch (SecurityException e) {
            loadWeatherForCoordinates(defaultLat, defaultLng);
        }
    }

    private String getCityName(Context context, double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                if (address.getLocality() != null) return address.getLocality();
                if (address.getSubAdminArea() != null) return address.getSubAdminArea();
                if (address.getAdminArea() != null) return address.getAdminArea();
            }
        } catch (Exception e) {
            // fallback
        }
        return "Thành phố Hồ Chí Minh";
    }

    private void loadWeatherForCoordinates(double lat, double lng) {
        final Context appContext = getContext();
        if (appContext == null) return;
        final int requestId = ++weatherRequestId;

        new Thread(() -> {
            String cityName = getCityName(appContext, lat, lng);
            WeatherSnapshot snapshot;
            try {
                snapshot = fetchWeatherSnapshot(lat, lng);
            } catch (Exception e) {
                android.util.Log.e("SkinWeatherForecast", "Weather fetch failed", e);
                snapshot = new WeatherSnapshot(0, 0, 0, -1.0, -1, false, false, -1, "", "");
            }

            final WeatherSnapshot result = snapshot;
            final String resolvedCity = cityName;
            runOnUiThreadSafe(() -> {
                if (requestId != weatherRequestId) return;
                if (result.success) {
                    updateWeatherUI(result, resolvedCity, lat, lng, true);
                } else {
                    useFallbackWeatherData(resolvedCity, lat, lng);
                }
            });
        }).start();
    }

    private WeatherSnapshot fetchWeatherSnapshot(double lat, double lng) throws Exception {
        double temp = 0;
        int humidity = 0;
        double uv = 0;
        double pm25 = -1.0;
        int usAqi = -1;
        int weatherCode = -1;
        boolean success = false;
        boolean hasPm25 = false;
        String pm25Source = "";
        String pm25Station = "";

        String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lng
                + "&current=temperature_2m,relative_humidity_2m,weather_code,uv_index&timezone=auto";
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream is = connection.getInputStream();
            Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";
            is.close();

            JSONObject json = new JSONObject(response);
            JSONObject current = json.getJSONObject("current");
            temp = current.getDouble("temperature_2m");
            humidity = current.getInt("relative_humidity_2m");
            weatherCode = current.optInt("weather_code", -1);
            if (current.has("uv_index") && !current.isNull("uv_index")) {
                uv = current.getDouble("uv_index");
            }
            success = true;
        }
        connection.disconnect();

        if (success) {
            AirQualityFetcher.Reading aqReading = AirQualityFetcher.fetch(lat, lng, WAQI_API_KEY);
            if (aqReading.hasData()) {
                pm25 = aqReading.pm25UgM3;
                hasPm25 = true;
                usAqi = aqReading.usAqi > 0
                        ? aqReading.usAqi
                        : WeatherDisplayHelper.computeUsAqiFromPm25(pm25);
                pm25Source = aqReading.source.name();
                pm25Station = aqReading.stationName != null ? aqReading.stationName : "";
            }
        }

        return new WeatherSnapshot(temp, humidity, uv, pm25, usAqi, success, hasPm25, weatherCode, pm25Source, pm25Station);
    }

    private void useFallbackWeatherData(String cityName, double lat, double lng) {
        updateWeatherUI(new WeatherSnapshot(0, 0, 0, -1.0, -1, false, false, -1, "", ""), cityName, lat, lng, true);
    }

    private void updateWeatherUI(WeatherSnapshot snapshot, String cityName, double lat, double lng) {
        updateWeatherUI(snapshot, cityName, lat, lng, false);
    }

    private void updateWeatherUI(WeatherSnapshot snapshot, String cityName, double lat, double lng, boolean shouldPersist) {
        if (!isAdded() || _binding == null) return;

        double temp = snapshot.temp;
        int humidity = snapshot.humidity;
        double uv = snapshot.uv;
        boolean hasPm25 = snapshot.hasPm25;
        WeatherDisplayHelper.Pm25Display pm25Display = WeatherDisplayHelper.formatPm25(snapshot.pm25, hasPm25, snapshot.usAqi);
        int pm25Val = pm25Display.numericValue;

        try {
        String weatherCondition = snapshot.success
                ? WeatherDisplayHelper.weatherCodeToDescription(snapshot.weatherCode, temp)
                : "KHÔNG CÓ DỮ LIỆU";

        if (shouldPersist && !isViewingHistory) {
            SkinWeatherSnapshotManager.saveAndSync(requireContext(), new SkinWeatherSnapshotManager.Snapshot(
                    temp, humidity, uv, snapshot.pm25, snapshot.usAqi,
                    lat, lng, snapshot.weatherCode, snapshot.success, hasPm25,
                    cityName, weatherCondition, snapshot.pm25Source, snapshot.pm25Station,
                    System.currentTimeMillis()
            ));
        }

        getBinding().tvLocation.setText(cityName);
        getBinding().tvTemperature.setText(snapshot.success ? String.valueOf((int) Math.round(temp)) : "--");
        getBinding().tvWeatherCondition.setText(weatherCondition);
        getBinding().tvWeatherCondition.setTextColor(ContextCompat.getColor(requireContext(),
            snapshot.success
                ? WeatherDisplayHelper.weatherConditionColorRes(snapshot.weatherCode, temp)
                : R.color.gray_dark));

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isNight = hour < 6 || hour >= 18;
        if (isNight) {
            getBinding().ivWeatherIcon.setImageResource(R.drawable.ic_moon);
            getBinding().ivWeatherIcon.setColorFilter(Color.parseColor("#90A4AE"));
        } else {
            getBinding().ivWeatherIcon.setImageResource(R.drawable.ic_sun);
            getBinding().ivWeatherIcon.setColorFilter(Color.parseColor("#F0C43D"));
        }

        getBinding().tvHumidity.setText(snapshot.success ? humidity + "%" : "--");
        String humidityLevel = !snapshot.success ? "Chưa có dữ liệu" :
                humidity < 40 ? "Thấp" : humidity <= 65 ? "Trung bình" : "Cao";
        getBinding().tvHumidityLevel.setText(humidityLevel);
        getBinding().tvHumidityCaption.setText(snapshot.success ? "không khí" : "");

        int humidityColorRes = !snapshot.success ? R.color.gray_dark :
                humidity < 40 ? R.color.status_level_orange : R.color.status_level_blue;
        int humidityBgRes = !snapshot.success ? R.drawable.bg_card_status_blue :
                humidity < 40 ? R.drawable.bg_card_status_orange : R.drawable.bg_card_status_blue;
        int humidityColorVal = ContextCompat.getColor(requireContext(), humidityColorRes);
        int humidityCaptionColor = ContextCompat.getColor(requireContext(),
                snapshot.success ? R.color.gray_dark : R.color.gray_dark);
        getBinding().tvHumidity.setTextColor(humidityColorVal);
        getBinding().tvHumidityLevel.setTextColor(humidityColorVal);
        getBinding().tvHumidityCaption.setTextColor(humidityCaptionColor);
        getBinding().layoutHumidityBox.setBackgroundResource(humidityBgRes);

        WeatherDisplayHelper.UvDisplay uvDisplay = WeatherDisplayHelper.formatUv(uv);
        getBinding().tvUvIndex.setText(snapshot.success ? uvDisplay.valueText : "--");
        getBinding().tvUvLevel.setText(snapshot.success ? uvDisplay.levelText : "Chưa có dữ liệu");
        getBinding().tvUvCaption.setText(snapshot.success ? "chỉ số UV" : "");
        int uvColorVal = ContextCompat.getColor(requireContext(), snapshot.success ? uvDisplay.colorRes : R.color.gray_dark);
        int uvBgRes = snapshot.success ? uvDisplay.bgRes : R.drawable.bg_card_status_blue;
        getBinding().tvUvIndex.setTextColor(uvColorVal);
        getBinding().tvUvLevel.setTextColor(uvColorVal);
        getBinding().tvUvCaption.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark));
        getBinding().layoutUvBox.setBackgroundResource(uvBgRes);

        getBinding().tvDustIndex.setText(pm25Display.valueText);
        getBinding().tvDustUnit.setText(hasPm25 ? "µg/m³ PM2.5" : "PM2.5");
        getBinding().tvDustLevel.setText(hasPm25 ? pm25Display.levelText : "Chưa có dữ liệu");
        getBinding().tvDustAqi.setText(hasPm25 ? buildPm25AqiText(pm25Display, snapshot.pm25Source) : "");
        int dustColorVal = ContextCompat.getColor(requireContext(), pm25Display.colorRes);
        int dustCaptionColor = ContextCompat.getColor(requireContext(),
                hasPm25 ? R.color.gray_dark : R.color.gray_dark);
        getBinding().tvDustIndex.setTextColor(hasPm25 ? dustColorVal : ContextCompat.getColor(requireContext(), R.color.gray_dark));
        getBinding().tvDustLevel.setTextColor(hasPm25 ? dustColorVal : ContextCompat.getColor(requireContext(), R.color.gray_dark));
        getBinding().tvDustUnit.setTextColor(dustCaptionColor);
        getBinding().tvDustAqi.setTextColor(dustCaptionColor);
        getBinding().layoutDustBox.setBackgroundResource(pm25Display.bgRes);

        String warningMsg;
        if (!snapshot.success) {
            warningMsg = "Không tải được dữ liệu thời tiết. Vui lòng kiểm tra kết nối mạng và thử lại.";
        } else if (uv >= 8) {
            warningMsg = "Chỉ số UV hiện tại ở mức rất cao (" + String.format(Locale.US, "%.1f", uv) + "). Bạn hãy bôi kem chống nắng kỹ và che chắn khi ra ngoài!";
        } else if (humidity < 40) {
            warningMsg = "Độ ẩm không khí hôm nay khá thấp (" + humidity + "%). Da bạn sẽ mất nước nhanh, hãy chú ý cấp khóa ẩm!";
        } else if (hasPm25 && pm25Val > 55) {
            warningMsg = "Bụi mịn PM2.5: " + pm25Display.valueText + " µg/m³ (" + pm25Display.aqiText + "). Hạn chế tiếp xúc không khí ô nhiễm và làm sạch da khi về nhà.";
        } else {
            warningMsg = "Theo dõi thời tiết để điều chỉnh chu trình chăm da. Hãy bảo vệ da khỏi tia UV và uống đủ nước!";
        }
        getBinding().tvWarningText.setText(warningMsg);

        int warningBgRes, warningTextColRes;
        if (!snapshot.success) {
            warningBgRes = R.drawable.bg_card_status_orange;
            warningTextColRes = R.color.warning_text_orange;
        } else if (uv >= 8 || (hasPm25 && pm25Val > 55)) {
            warningBgRes = R.drawable.bg_card_status_red;
            warningTextColRes = R.color.warning_text_red;
        } else if (humidity < 40 || uv >= 5.0 || (hasPm25 && pm25Val > 35)) {
            warningBgRes = R.drawable.bg_card_status_orange;
            warningTextColRes = R.color.warning_text_orange;
        } else {
            warningBgRes = R.drawable.bg_card_status_yellow;
            warningTextColRes = R.color.warning_text_yellow;
        }
        
        getBinding().layoutWarningBox.setBackgroundResource(warningBgRes);
        int warningTextColorVal = ContextCompat.getColor(requireContext(), warningTextColRes);
        getBinding().tvWarningText.setTextColor(warningTextColorVal);
        getBinding().ivWarningIcon.setColorFilter(warningTextColorVal);

        SkinWeatherProfileHelper.UserSkinProfile skinProfile = SkinWeatherProfileHelper.load(requireContext());
        SkinWeatherProfileHelper.TodaySkinMetrics todayMetrics = SkinWeatherProfileHelper.computeTodayMetrics(
                skinProfile, temp, humidity, uv, pm25Val, hasPm25);

        getBinding().tvSkinStatusTitle.setText(SkinWeatherProfileHelper.buildSkinStatusSectionTitle(skinProfile));
        getBinding().tvSkinStatusSubtitle.setText(SkinWeatherProfileHelper.buildSkinStatusSectionSubtitle(skinProfile));

        SkinWeatherProfileHelper.PersonalizedAdvice initialAdvice = SkinWeatherProfileHelper.buildRuleBasedAdvice(
                skinProfile, todayMetrics, temp, humidity, uv, pm25Val, hasPm25, cityName);
        applyPersonalizedAdvice(initialAdvice);

        String skinType = skinProfile.skinType;
        int oilyPercent = todayMetrics.oilyPercent;
        int hydrationPercent = todayMetrics.hydrationPercent;
        int sensitivityPercent = todayMetrics.sensitivityPercent;

        String skinLower = skinType.toLowerCase();
        boolean isOily = skinLower.contains("dầu");
        boolean isSensitive = skinLower.contains("nhạy cảm") || skinLower.contains("kích ứng");
        boolean isDry = skinLower.contains("khô");
        boolean isCombination = skinLower.contains("hỗn hợp");
        boolean isAging = skinLower.contains("lão hóa");
        boolean isDehydrated = skinLower.contains("mất nước");

        getBinding().tvOilyValue.setText(oilyPercent + "%");
        getBinding().progressOily.setProgress(oilyPercent);
        int oilyColor = ContextCompat.getColor(requireContext(), oilyPercent < 40 ? R.color.status_level_green : oilyPercent <= 70 ? R.color.status_level_yellow : R.color.status_level_red);
        getBinding().tvOilyValue.setTextColor(oilyColor);
        getBinding().progressOily.setProgressTintList(android.content.res.ColorStateList.valueOf(oilyColor));

        getBinding().tvHydrationValue.setText(hydrationPercent + "%");
        getBinding().progressHydration.setProgress(hydrationPercent);
        int hydrationColor = ContextCompat.getColor(requireContext(), hydrationPercent < 40 ? R.color.status_level_red : hydrationPercent <= 65 ? R.color.status_level_yellow : R.color.status_level_green);
        getBinding().tvHydrationValue.setTextColor(hydrationColor);
        getBinding().progressHydration.setProgressTintList(android.content.res.ColorStateList.valueOf(hydrationColor));

        getBinding().tvSensitivityValue.setText(sensitivityPercent + "%");
        getBinding().progressSensitivity.setProgress(sensitivityPercent);
        int sensitivityColor = ContextCompat.getColor(requireContext(), sensitivityPercent < 30 ? R.color.status_level_green : sensitivityPercent <= 60 ? R.color.status_level_yellow : R.color.status_level_red);
        getBinding().tvSensitivityValue.setTextColor(sensitivityColor);
        getBinding().progressSensitivity.setProgressTintList(android.content.res.ColorStateList.valueOf(sensitivityColor));

        String uvImpact, hydrationImpact, dustImpact;
        boolean highPm25 = hasPm25 && pm25Val > 55;
        if (isOily) {
            uvImpact = uv >= 6.0 ? "Tăng bã nhờn & sạm da" : "Tăng tiết bã nhờn";
            hydrationImpact = humidity < 55 ? "Dầu nước mất cân bằng" : "Độ ẩm cân bằng";
            dustImpact = highPm25 ? "Nguy cơ tắc nghẽn mụn" : hasPm25 ? "Bám dính dầu nhờn" : "Chưa có dữ liệu PM2.5";
        } else if (isDry) {
            uvImpact = uv >= 6.0 ? "Dễ rát & sạm da khô" : "Dễ mất ẩm bong tróc";
            hydrationImpact = humidity < 55 ? "Thiếu nước nghiêm trọng" : "Giảm khô căng nhẹ";
            dustImpact = highPm25 ? "Hàng rào bảo vệ yếu" : hasPm25 ? "Khô ngứa do khói bụi" : "Chưa có dữ liệu PM2.5";
        } else if (isSensitive) {
            uvImpact = uv >= 5.0 ? "Nguy cơ đỏ rát cao" : "Dễ kích ứng nhẹ";
            hydrationImpact = humidity < 55 ? "Màng ẩm tổn thương" : "Đủ ẩm dễ chịu";
            dustImpact = highPm25 ? "Mẩn ngứa bít tắc" : hasPm25 ? "Bám dính bụi bẩn" : "Chưa có dữ liệu PM2.5";
        } else if (isCombination) {
            uvImpact = uv >= 6.0 ? "Vùng chữ T nhờn rát" : "Vùng chữ T tiết dầu";
            hydrationImpact = humidity < 55 ? "Hai bên má khô rát" : "Cân bằng vùng má";
            dustImpact = highPm25 ? "Bít tắc vùng chữ T" : hasPm25 ? "Tích tụ bụi nhẹ" : "Chưa có dữ liệu PM2.5";
        } else if (isAging) {
            uvImpact = uv >= 6.0 ? "Tia UV gây sạm nám" : "Đẩy nhanh lão hóa";
            hydrationImpact = humidity < 55 ? "Mất nước nếp nhăn sâu" : "Giữ ẩm tế bào";
            dustImpact = highPm25 ? "Tổn thương gốc tự do" : hasPm25 ? "Tác nhân ô nhiễm" : "Chưa có dữ liệu PM2.5";
        } else if (isDehydrated) {
            uvImpact = uv >= 6.0 ? "Rát sạm khô căng" : "Nếp nhăn giả do UV";
            hydrationImpact = humidity < 45 ? "Mất nước tế bào sâu" : "Cải thiện tình trạng khô";
            dustImpact = highPm25 ? "Tắc tuyến bã nhờn" : hasPm25 ? "Bụi gây khô bề mặt" : "Chưa có dữ liệu PM2.5";
        } else {
            uvImpact = uv >= 6.0 ? "Chống nắng tối đa" : "Bảo vệ dịu nhẹ";
            hydrationImpact = humidity < 55 ? "Cần cấp ẩm nhẹ" : "Duy trì ẩm tốt";
            dustImpact = highPm25 ? "Làm sạch sâu bụi mịn" : hasPm25 ? "Làm sạch bình thường" : "Chưa có dữ liệu PM2.5";
        }

        getBinding().tvUvImpact.setText(uvImpact);
        getBinding().tvHydrationImpact.setText(hydrationImpact);
        getBinding().tvDustImpact.setText(dustImpact);

        updateRoutineLabels(skinProfile, temp, humidity, uv, pm25Val);
        fetchGeminiAdvice(skinProfile, todayMetrics, temp, humidity, uv, pm25Val, hasPm25, cityName);
        } catch (Exception e) {
            android.util.Log.e("SkinWeatherForecast", "Failed to render weather UI", e);
        }
    }

    private void applyPersonalizedAdvice(SkinWeatherProfileHelper.PersonalizedAdvice advice) {
        if (!isAdded() || _binding == null || advice == null) return;
        getBinding().tvAlertText.setText(advice.headline);
        getBinding().tvAlertSubtext.setText(advice.subtext);
        getBinding().tvAiInsightDesc.setText(advice.insight);
    }

    private void fetchGeminiAdvice(SkinWeatherProfileHelper.UserSkinProfile skinProfile,
                                   SkinWeatherProfileHelper.TodaySkinMetrics todayMetrics,
                                   double temp, int humidity, double uv, int pm25,
                                   boolean hasPm25, String cityName) {
        if (!isAdded() || _binding == null) return;

        final int oily = todayMetrics.oilyPercent;
        final int hydration = todayMetrics.hydrationPercent;
        final int sensitivity = todayMetrics.sensitivityPercent;
        final String skinType = skinProfile.skinType;

        if (GEMINI_API_KEY == null || GEMINI_API_KEY.trim().isEmpty() || GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY_HERE")) {
            SkinWeatherProfileHelper.PersonalizedAdvice advice = SkinWeatherProfileHelper.buildRuleBasedAdvice(
                    skinProfile, todayMetrics, temp, humidity, uv, pm25, hasPm25, cityName);
            applyPersonalizedAdvice(advice);
            saveDiagnosticRecord(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity, advice.insight);
            return;
        }

        final int requestId = ++geminiRequestId;
        final String prompt = SkinWeatherProfileHelper.buildGeminiPrompt(
                skinProfile, todayMetrics, temp, humidity, uv, pm25, hasPm25, cityName);
        final SharedPreferences prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        final String adviceCacheKey = com.veganbeauty.app.features.ai.SkinAiAssistantHelper.buildAdviceCacheKey(
                skinProfile, temp, humidity, uv, pm25, cityName);

        new Thread(() -> {
            String textResult = null;
            try {
                String urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;
                HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setDoOutput(true);

                JSONObject requestJson = new JSONObject();
                JSONArray partsArray = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);
                partsArray.put(textPart);

                JSONArray contentsArray = new JSONArray();
                JSONObject contentsObj = new JSONObject();
                contentsObj.put("parts", partsArray);
                contentsArray.put(contentsObj);
                requestJson.put("contents", contentsArray);

                JSONObject systemInstruction = new JSONObject();
                JSONArray systemParts = new JSONArray();
                JSONObject sysTextPart = new JSONObject();
                sysTextPart.put("text", "Bạn là trợ lý da liễu thông minh của ROOTIE (Vegan Skincare). Luôn dựa trên HỒ SƠ DA ĐÃ LƯU được gửi kèm, không đưa lời khuyên chung chung. Trả lời tiếng Việt, ngắn gọn, thực tế, thuần chay.");
                systemParts.put(sysTextPart);
                systemInstruction.put("parts", systemParts);
                requestJson.put("systemInstruction", systemInstruction);

                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", 0.7);
                generationConfig.put("maxOutputTokens", 350);
                requestJson.put("generationConfig", generationConfig);

                java.io.OutputStream os = connection.getOutputStream();
                os.write(requestJson.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = connection.getInputStream();
                    Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    is.close();

                    JSONObject json = new JSONObject(response);
                    JSONArray candidates = json.getJSONArray("candidates");
                    if (candidates.length() > 0) {
                        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        if (parts.length() > 0) {
                            textResult = parts.getJSONObject(0).getString("text").trim();
                            prefs.edit()
                                    .putString("SAVED_CACHED_AI_INSIGHT", textResult)
                                    .putString("SAVED_CACHED_AI_INSIGHT_CONTEXT", adviceCacheKey)
                                    .apply();
                        }
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                android.util.Log.w("SkinWeatherForecast", "Gemini advice failed", e);
            }

            final String resolved = textResult;
            runOnUiThreadSafe(() -> {
                if (requestId != geminiRequestId) return;
                SkinWeatherProfileHelper.PersonalizedAdvice advice;
                if (resolved != null && !resolved.trim().isEmpty()) {
                    advice = SkinWeatherProfileHelper.parseGeminiAdvice(
                            resolved, skinProfile, todayMetrics, temp, humidity, uv, pm25, hasPm25, cityName);
                } else {
                    String cached = prefs.getString("SAVED_CACHED_AI_INSIGHT", null);
                    String cachedKey = prefs.getString("SAVED_CACHED_AI_INSIGHT_CONTEXT", "");
                    if (cached != null && !cached.trim().isEmpty()
                            && adviceCacheKey.equals(cachedKey)) {
                        advice = SkinWeatherProfileHelper.parseGeminiAdvice(
                                cached, skinProfile, todayMetrics, temp, humidity, uv, pm25, hasPm25, cityName);
                    } else {
                        advice = SkinWeatherProfileHelper.buildRuleBasedAdvice(
                                skinProfile, todayMetrics, temp, humidity, uv, pm25, hasPm25, cityName);
                    }
                }
                applyPersonalizedAdvice(advice);
                saveDiagnosticRecord(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity, advice.insight);
            });
        }).start();
    }

    private void saveDiagnosticRecord(double temp, int humidity, double uv, int pm25, String cityName, String skinType, int oily, int hydration, int sensitivity, String insightText) {
        if (isViewingHistory) return;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date());

            List<SkinWeatherDiagnostic.RoutineItem> routineItems = new ArrayList<>();
            String[] categoryNames = {"Cleanser", "Serum", "Moisturizer", "Sunscreen"};
            View[] layoutIds = {
                    getBinding().layoutRoutineItem1,
                    getBinding().layoutRoutineItem2,
                    getBinding().layoutRoutineItem3,
                    getBinding().layoutRoutineItem4
            };

            for (int i = 0; i < layoutIds.length; i++) {
                View card = layoutIds[i];
                if (card instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) card;
                    if (vg.getChildCount() > 1 && vg.getChildAt(1) instanceof LinearLayout) {
                        LinearLayout titleLayout = (LinearLayout) vg.getChildAt(1);
                        String pName = titleLayout.getChildCount() > 0 && titleLayout.getChildAt(0) instanceof TextView ? ((TextView) titleLayout.getChildAt(0)).getText().toString() : "";
                        String pDesc = titleLayout.getChildCount() > 1 && titleLayout.getChildAt(1) instanceof TextView ? ((TextView) titleLayout.getChildAt(1)).getText().toString() : "";
                        routineItems.add(new SkinWeatherDiagnostic.RoutineItem(categoryNames[i], pName, pDesc));
                    }
                }
            }

            SkinWeatherDiagnostic diagnostic = new SkinWeatherDiagnostic(
                    UUID.randomUUID().toString(),
                    System.currentTimeMillis(),
                    dateStr,
                    cityName,
                    (int) temp,
                    humidity,
                    uv,
                    pm25,
                    skinType,
                    oily,
                    hydration,
                    sensitivity,
                    insightText,
                    routineItems
            );

            SkinWeatherHistoryManager.saveDiagnostic(requireContext(), diagnostic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupFeedbackButtons() {
        android.graphics.drawable.Drawable activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.com_bg_tab_active);
        android.graphics.drawable.Drawable inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.quiz_bg_pill_suitable);
        if (activeBg == null || inactiveBg == null) {
            return;
        }

        int colorAppropriate = Color.parseColor("#67814D");
        int colorLighter = Color.parseColor("#D88B2A");
        int colorUnsuitable = Color.parseColor("#E35B5B");

        int bgAppropriate = Color.parseColor("#EAF3E8");
        int bgLighter = Color.parseColor("#FDF7ED");
        int bgUnsuitable = Color.parseColor("#FEEFEE");

        LinearLayout btnAppropriate = getBinding().btnSuitAppropriate;
        LinearLayout btnLighter = getBinding().btnSuitLighter;
        LinearLayout btnUnsuitable = getBinding().btnSuitUnsuitable;

        TextView tvAppropriate = getBinding().tvSuitAppropriate;
        TextView tvLighter = getBinding().tvSuitLighter;
        TextView tvUnsuitable = getBinding().tvSuitUnsuitable;

        ImageView imgAppropriate = (ImageView) btnAppropriate.getChildAt(0);
        ImageView imgLighter = (ImageView) btnLighter.getChildAt(0);
        ImageView imgUnsuitable = (ImageView) btnUnsuitable.getChildAt(0);

        Runnable resetButtons = () -> {
            btnAppropriate.setBackground(inactiveBg);
            btnAppropriate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgAppropriate));
            tvAppropriate.setTextColor(colorAppropriate);
            imgAppropriate.setColorFilter(colorAppropriate);

            btnLighter.setBackground(inactiveBg);
            btnLighter.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgLighter));
            tvLighter.setTextColor(colorLighter);
            imgLighter.setColorFilter(colorLighter);

            btnUnsuitable.setBackground(inactiveBg);
            btnUnsuitable.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgUnsuitable));
            tvUnsuitable.setTextColor(colorUnsuitable);
            imgUnsuitable.setColorFilter(colorUnsuitable);
        };

        btnAppropriate.setOnClickListener(v -> {
            resetButtons.run();
            btnAppropriate.setBackground(activeBg);
            btnAppropriate.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_level_green));
            tvAppropriate.setTextColor(Color.WHITE);
            imgAppropriate.setColorFilter(Color.WHITE);
            Toast.makeText(getContext(), "Cảm ơn bạn đã phản hồi! Rootie sẽ tiếp tục duy trì chu trình này.", Toast.LENGTH_SHORT).show();
        });

        btnLighter.setOnClickListener(v -> {
            resetButtons.run();
            btnLighter.setBackground(activeBg);
            btnLighter.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_level_yellow));
            tvLighter.setTextColor(Color.WHITE);
            imgLighter.setColorFilter(Color.WHITE);
            Toast.makeText(getContext(), "Ghi nhận! Chu trình chăm da tiếp theo sẽ mỏng nhẹ và tối giản hơn.", Toast.LENGTH_SHORT).show();
        });

        btnUnsuitable.setOnClickListener(v -> {
            resetButtons.run();
            btnUnsuitable.setBackground(activeBg);
            btnUnsuitable.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_level_red));
            tvUnsuitable.setTextColor(Color.WHITE);
            imgUnsuitable.setColorFilter(Color.WHITE);
            Toast.makeText(getContext(), "Rootie AI đang ghi nhận và điều chỉnh lại sản phẩm phù hợp hơn.", Toast.LENGTH_SHORT).show();
        });

        resetButtons.run();
        btnAppropriate.setBackground(activeBg);
        btnAppropriate.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_level_green));
        tvAppropriate.setTextColor(Color.WHITE);
        imgAppropriate.setColorFilter(Color.WHITE);
    }

    private void setupBottomNavigation() {
        BottomNavHelper.setup(
                this,
                getBinding().getRoot(),
                R.id.nav_myskin,
                tabId -> BottomNavHelper.navigate(this, tabId)
        );
    }

    @Override
    public void onDestroyView() {
        weatherRequestId++;
        geminiRequestId++;
        stopLocationUpdates();
        super.onDestroyView();
        _binding = null;
    }

    private void showHistoryDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_skin_weather_history, null);
        dialog.setContentView(dialogView);

        RecyclerView rvHistory = dialogView.findViewById(R.id.rv_weather_history);
        TextView btnClose = dialogView.findViewById(R.id.btn_close_history);

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<SkinWeatherDiagnostic> localHistory = SkinWeatherHistoryManager.getHistory(requireContext());
        WeatherHistoryAdapter adapter = new WeatherHistoryAdapter(localHistory, selectedDiagnostic -> {
            displayHistoricalDiagnostic(selectedDiagnostic);
            dialog.dismiss();
            return null;
        });
        rvHistory.setAdapter(adapter);

        SkinWeatherHistoryManager.syncFromFirestore(requireContext(), syncedList -> {
            if (isAdded() && _binding != null) {
                rvHistory.setAdapter(new WeatherHistoryAdapter(syncedList, selectedDiagnostic -> {
                    displayHistoricalDiagnostic(selectedDiagnostic);
                    dialog.dismiss();
                    return null;
                }));
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void displayHistoricalDiagnostic(SkinWeatherDiagnostic diagnostic) {
        isViewingHistory = true;

        getBinding().layoutHistoryBanner.setVisibility(View.VISIBLE);
        getBinding().tvHistoryBannerText.setText("Bạn đang xem dữ liệu lịch sử ngày " + diagnostic.getDate());
        getBinding().tvLocation.setText(diagnostic.getCity());

        getBinding().tvTemperature.setText(String.valueOf((int) diagnostic.getTemperature()));
        String weatherCondition = diagnostic.getTemperature() >= 33 ? "NẮNG NÓNG GAY GẮT" :
                                  diagnostic.getTemperature() >= 28 ? "NẮNG NHIỀU, OI NHẸ" : "MÁT MẺ, DỄ CHỊU";
        getBinding().tvWeatherCondition.setText(weatherCondition);
        getBinding().tvWeatherCondition.setTextColor(ContextCompat.getColor(requireContext(), 
            diagnostic.getTemperature() >= 33 ? R.color.status_level_red :
            diagnostic.getTemperature() >= 28 ? R.color.status_level_yellow : R.color.secondary));

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isNight = hour < 6 || hour >= 18;
        if (isNight) {
            getBinding().ivWeatherIcon.setImageResource(R.drawable.ic_moon);
            getBinding().ivWeatherIcon.setColorFilter(Color.parseColor("#90A4AE"));
        } else {
            getBinding().ivWeatherIcon.setImageResource(R.drawable.ic_sun);
            getBinding().ivWeatherIcon.setColorFilter(Color.parseColor("#F0C43D"));
        }

        getBinding().tvHumidity.setText(diagnostic.getHumidity() + "%");
        String humidityLevel = diagnostic.getHumidity() < 40 ? "Thấp" : diagnostic.getHumidity() <= 65 ? "Trung bình" : "Cao";
        getBinding().tvHumidityLevel.setText(humidityLevel);
        getBinding().tvHumidityCaption.setText("không khí");

        int humidityColorRes = diagnostic.getHumidity() < 40 ? R.color.status_level_orange : R.color.status_level_blue;
        int humidityBgRes = diagnostic.getHumidity() < 40 ? R.drawable.bg_card_status_orange : R.drawable.bg_card_status_blue;
        int humidityColorVal = ContextCompat.getColor(requireContext(), humidityColorRes);
        getBinding().tvHumidity.setTextColor(humidityColorVal);
        getBinding().tvHumidityLevel.setTextColor(humidityColorVal);
        getBinding().tvHumidityCaption.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark));
        getBinding().layoutHumidityBox.setBackgroundResource(humidityBgRes);

        WeatherDisplayHelper.UvDisplay uvDisplay = WeatherDisplayHelper.formatUv(diagnostic.getUv());
        getBinding().tvUvIndex.setText(uvDisplay.valueText);
        getBinding().tvUvLevel.setText(uvDisplay.levelText);
        getBinding().tvUvCaption.setText("chỉ số UV");
        int uvColorVal = ContextCompat.getColor(requireContext(), uvDisplay.colorRes);
        getBinding().tvUvIndex.setTextColor(uvColorVal);
        getBinding().tvUvLevel.setTextColor(uvColorVal);
        getBinding().tvUvCaption.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark));
        getBinding().layoutUvBox.setBackgroundResource(uvDisplay.bgRes);

        boolean diagnosticHasPm25 = diagnostic.getPm25() >= 0;
        WeatherDisplayHelper.Pm25Display pm25Display = WeatherDisplayHelper.formatPm25(diagnostic.getPm25(), diagnosticHasPm25, -1);
        getBinding().tvDustIndex.setText(pm25Display.valueText);
        getBinding().tvDustUnit.setText(diagnosticHasPm25 ? "µg/m³ PM2.5" : "PM2.5");
        getBinding().tvDustLevel.setText(diagnosticHasPm25 ? pm25Display.levelText : "Chưa có dữ liệu");
        getBinding().tvDustAqi.setText(diagnosticHasPm25 ? pm25Display.aqiText : "");
        int dustColorVal = ContextCompat.getColor(requireContext(), pm25Display.colorRes);
        getBinding().tvDustIndex.setTextColor(diagnosticHasPm25 ? dustColorVal : ContextCompat.getColor(requireContext(), R.color.gray_dark));
        getBinding().tvDustLevel.setTextColor(diagnosticHasPm25 ? dustColorVal : ContextCompat.getColor(requireContext(), R.color.gray_dark));
        getBinding().tvDustUnit.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark));
        getBinding().tvDustAqi.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark));
        getBinding().layoutDustBox.setBackgroundResource(pm25Display.bgRes);

        int pm25Val = pm25Display.numericValue;
        boolean hasPm25 = pm25Display.hasData;

        String warningMsg = diagnostic.getUv() >= 8 ? "Hôm nay chỉ số UV ở mức rất cao (" + String.format(Locale.US, "%.1f", diagnostic.getUv()) + "). Bạn hãy bôi kem chống nắng kỹ và che chắn khi ra ngoài!" :
                            diagnostic.getHumidity() < 40 ? "Độ ẩm không khí hôm nay khá thấp (" + diagnostic.getHumidity() + "%). Da bạn sẽ mất nước nhanh, hãy chú ý cấp khóa ẩm!" :
                            "Hôm nay thời tiết khá nóng và độ ẩm trung bình. Hãy bảo vệ da khỏi tia UV hại và uống đủ nước!";
        getBinding().tvWarningText.setText(warningMsg);

        int warningBgRes, warningTextColRes;
        if (diagnostic.getUv() >= 8 || diagnostic.getPm25() >= 50) {
            warningBgRes = R.drawable.bg_card_status_red;
            warningTextColRes = R.color.warning_text_red;
        } else if (diagnostic.getHumidity() < 40 || diagnostic.getUv() >= 5.0 || diagnostic.getPm25() >= 25) {
            warningBgRes = R.drawable.bg_card_status_orange;
            warningTextColRes = R.color.warning_text_orange;
        } else {
            warningBgRes = R.drawable.bg_card_status_yellow;
            warningTextColRes = R.color.warning_text_yellow;
        }
        getBinding().layoutWarningBox.setBackgroundResource(warningBgRes);
        int warningTextColorVal = ContextCompat.getColor(requireContext(), warningTextColRes);
        getBinding().tvWarningText.setTextColor(warningTextColorVal);
        getBinding().ivWarningIcon.setColorFilter(warningTextColorVal);

        String alertText = diagnostic.getUv() >= 8 ? "Chỉ số UV hôm nay cực cao! Nhớ thoa lại kem chống nắng sau mỗi 2 giờ ra ngoài." :
                           diagnostic.getTemperature() >= 33 ? "Nhiệt độ nóng gay gắt, hãy dùng gel dưỡng ẩm mỏng nhẹ để tránh bít tắc lỗ chân lông." :
                           "Theo hồ sơ " + diagnostic.getSkinType() + " — duy trì routine phù hợp hôm nay.";
        getBinding().tvAlertText.setText(alertText);
        String insight = diagnostic.getInsight();
        if (insight != null && !insight.trim().isEmpty()) {
            getBinding().tvAlertSubtext.setText("Lịch sử ngày " + diagnostic.getDate() + " · " + diagnostic.getSkinType());
            getBinding().tvAiInsightDesc.setText(insight);
        }

        getBinding().tvSkinStatusTitle.setText("TÌNH TRẠNG DA HÔM NAY");
        getBinding().tvSkinStatusSubtitle.setText("Theo hồ sơ: " + diagnostic.getSkinType());

        getBinding().tvOilyValue.setText(diagnostic.getOilyPercent() + "%");
        getBinding().progressOily.setProgress(diagnostic.getOilyPercent());
        int oilyColor = ContextCompat.getColor(requireContext(), diagnostic.getOilyPercent() < 40 ? R.color.status_level_green : diagnostic.getOilyPercent() <= 70 ? R.color.status_level_yellow : R.color.status_level_red);
        getBinding().tvOilyValue.setTextColor(oilyColor);
        getBinding().progressOily.setProgressTintList(android.content.res.ColorStateList.valueOf(oilyColor));

        getBinding().tvHydrationValue.setText(diagnostic.getHydrationPercent() + "%");
        getBinding().progressHydration.setProgress(diagnostic.getHydrationPercent());
        int hydrationColor = ContextCompat.getColor(requireContext(), diagnostic.getHydrationPercent() < 40 ? R.color.status_level_red : diagnostic.getHydrationPercent() <= 65 ? R.color.status_level_yellow : R.color.status_level_green);
        getBinding().tvHydrationValue.setTextColor(hydrationColor);
        getBinding().progressHydration.setProgressTintList(android.content.res.ColorStateList.valueOf(hydrationColor));

        getBinding().tvSensitivityValue.setText(diagnostic.getSensitivityPercent() + "%");
        getBinding().progressSensitivity.setProgress(diagnostic.getSensitivityPercent());
        int sensitivityColor = ContextCompat.getColor(requireContext(), diagnostic.getSensitivityPercent() < 30 ? R.color.status_level_green : diagnostic.getSensitivityPercent() <= 60 ? R.color.status_level_yellow : R.color.status_level_red);
        getBinding().tvSensitivityValue.setTextColor(sensitivityColor);
        getBinding().progressSensitivity.setProgressTintList(android.content.res.ColorStateList.valueOf(sensitivityColor));

        boolean isOily = diagnostic.getSkinType().toLowerCase().contains("dầu");
        boolean isDry = diagnostic.getSkinType().toLowerCase().contains("khô");
        boolean isSensitive = diagnostic.getSkinType().toLowerCase().contains("nhạy cảm") || diagnostic.getSkinType().toLowerCase().contains("kích ứng");
        boolean isCombination = diagnostic.getSkinType().toLowerCase().contains("hỗn hợp");
        boolean isAging = diagnostic.getSkinType().toLowerCase().contains("lão hóa");
        boolean isDehydrated = diagnostic.getSkinType().toLowerCase().contains("mất nước");

        String uvImpact, hydrationImpact, dustImpact;
        if (isOily) {
            uvImpact = diagnostic.getUv() >= 6.0 ? "Tăng bã nhờn & sạm da" : "Tăng tiết bã nhờn";
            hydrationImpact = diagnostic.getHumidity() < 55 ? "Dầu nước mất cân bằng" : "Độ ẩm cân bằng";
            dustImpact = diagnostic.getPm25() >= 50 ? "Nguy cơ tắc nghẽn mụn" : "Bám dính dầu nhờn";
        } else if (isDry) {
            uvImpact = diagnostic.getUv() >= 6.0 ? "Dễ rát & sạm da khô" : "Dễ mất ẩm bong tróc";
            hydrationImpact = diagnostic.getHumidity() < 55 ? "Thiếu nước nghiêm trọng" : "Giảm khô căng nhẹ";
            dustImpact = diagnostic.getPm25() >= 50 ? "Hàng rào bảo vệ yếu" : "Khô ngứa do khói bụi";
        } else if (isSensitive) {
            uvImpact = diagnostic.getUv() >= 5.0 ? "Nguy cơ đỏ rát cao" : "Dễ kích ứng nhẹ";
            hydrationImpact = diagnostic.getHumidity() < 55 ? "Màng ẩm tổn thương" : "Đủ ẩm dễ chịu";
            dustImpact = diagnostic.getPm25() >= 50 ? "Mẩn ngứa bít tắc" : "Bám dính bụi bẩn";
        } else if (isCombination) {
            uvImpact = diagnostic.getUv() >= 6.0 ? "Vùng chữ T nhờn rát" : "Vùng chữ T tiết dầu";
            hydrationImpact = diagnostic.getHumidity() < 55 ? "Hai bên má khô rát" : "Cân bằng vùng má";
            dustImpact = diagnostic.getPm25() >= 50 ? "Bít tắc vùng chữ T" : "Tích tụ bụi nhẹ";
        } else if (isAging) {
            uvImpact = diagnostic.getUv() >= 6.0 ? "Tia UV gây sạm nám" : "Đẩy nhanh lão hóa";
            hydrationImpact = diagnostic.getHumidity() < 55 ? "Mất nước nếp nhăn sâu" : "Giữ ẩm tế bào";
            dustImpact = diagnostic.getPm25() >= 50 ? "Tổn thương gốc tự do" : "Tác nhân ô nhiễm";
        } else if (isDehydrated) {
            uvImpact = diagnostic.getUv() >= 6.0 ? "Rát sạm khô căng" : "Nếp nhăn giả do UV";
            hydrationImpact = diagnostic.getHumidity() < 45 ? "Mất nước tế bào sâu" : "Cải thiện tình trạng khô";
            dustImpact = diagnostic.getPm25() >= 50 ? "Tắc tuyến bã nhờn" : "Bụi gây khô bề mặt";
        } else {
            uvImpact = diagnostic.getUv() >= 6.0 ? "Chống nắng tối đa" : "Bảo vệ dịu nhẹ";
            hydrationImpact = diagnostic.getHumidity() < 55 ? "Cần cấp ẩm nhẹ" : "Duy trì ẩm tốt";
            dustImpact = diagnostic.getPm25() >= 50 ? "Làm sạch sâu bụi mịn" : "Làm sạch bình thường";
        }

        getBinding().tvUvImpact.setText(uvImpact);
        getBinding().tvHydrationImpact.setText(hydrationImpact);
        getBinding().tvDustImpact.setText(dustImpact);

        List<SkinWeatherDiagnostic.RoutineItem> routineItems = diagnostic.getRecommendedRoutine();
        View[] layoutIds = {
                getBinding().layoutRoutineItem1,
                getBinding().layoutRoutineItem2,
                getBinding().layoutRoutineItem3,
                getBinding().layoutRoutineItem4
        };
        for (int i = 0; i < layoutIds.length; i++) {
            if (i < routineItems.size()) {
                View card = layoutIds[i];
                SkinWeatherDiagnostic.RoutineItem item = routineItems.get(i);
                if (card instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) card;
                    if (vg.getChildCount() > 1 && vg.getChildAt(1) instanceof LinearLayout) {
                        LinearLayout titleLayout = (LinearLayout) vg.getChildAt(1);
                        if (titleLayout.getChildCount() > 0 && titleLayout.getChildAt(0) instanceof TextView) ((TextView) titleLayout.getChildAt(0)).setText(item.getProductName());
                        if (titleLayout.getChildCount() > 1 && titleLayout.getChildAt(1) instanceof TextView) ((TextView) titleLayout.getChildAt(1)).setText(item.getDescription());
                    }
                }
                card.setOnClickListener(v -> navigateToProductDetail(null, item.getProductName()));
            }
        }

        getBinding().tvAiInsightDesc.setText(diagnostic.getInsight());
    }

    private String buildPm25AqiText(WeatherDisplayHelper.Pm25Display pm25Display, String pm25Source) {
        String text = pm25Display.aqiText;
        String sourceLabel = SkinWeatherSnapshotManager.sourceLabel(pm25Source);
        if (!sourceLabel.isEmpty()) {
            text += " · " + sourceLabel;
        }
        return text;
    }

    private class WeatherHistoryAdapter extends RecyclerView.Adapter<WeatherHistoryAdapter.ViewHolder> {
        private final List<SkinWeatherDiagnostic> items;
        private final kotlin.jvm.functions.Function1<SkinWeatherDiagnostic, kotlin.Unit> onItemClick;

        public WeatherHistoryAdapter(List<SkinWeatherDiagnostic> items, kotlin.jvm.functions.Function1<SkinWeatherDiagnostic, kotlin.Unit> onItemClick) {
            this.items = items;
            this.onItemClick = onItemClick;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvCity, tvTemp, tvHumidity, tvUv, tvInsight;
            ViewHolder(View view) {
                super(view);
                tvDate = view.findViewById(R.id.tv_history_item_date);
                tvCity = view.findViewById(R.id.tv_history_item_city);
                tvTemp = view.findViewById(R.id.tv_history_item_temp);
                tvHumidity = view.findViewById(R.id.tv_history_item_humidity);
                tvUv = view.findViewById(R.id.tv_history_item_uv);
                tvInsight = view.findViewById(R.id.tv_history_item_insight);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_skin_weather_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SkinWeatherDiagnostic item = items.get(position);
            holder.tvDate.setText(item.getDate());
            holder.tvCity.setText(item.getCity());
            holder.tvTemp.setText(((int) item.getTemperature()) + "°C");
            holder.tvHumidity.setText("Độ ẩm: " + item.getHumidity() + "%");
            holder.tvUv.setText("UV: " + String.format(Locale.US, "%.1f", item.getUv()));
            holder.tvInsight.setText(item.getInsight());

            holder.itemView.setOnClickListener(v -> onItemClick.invoke(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
