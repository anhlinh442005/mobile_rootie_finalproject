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
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.SkinWeatherForecastBinding;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;
import com.veganbeauty.app.features.ai.SkinAiChatFragment;
import com.veganbeauty.app.features.home.HomeFragment;
import com.veganbeauty.app.features.profile.AccountProfileFragment;
import com.veganbeauty.app.features.shop.home.ShopHomeFragment;
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment;
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

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.FlowKt;

public class SkinWeatherForecastFragment extends RootieFragment {

    private SkinWeatherForecastBinding _binding;
    private SkinWeatherForecastBinding getBinding() {
        return _binding;
    }

    private final String GEMINI_API_KEY = com.veganbeauty.app.BuildConfig.GEMINI_API_KEY;
    private final double defaultLat = 10.8231;
    private final double defaultLng = 106.6297;
    private boolean isViewingHistory = false;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinWeatherForecastBinding.inflate(inflater, container, false);
        return getBinding().getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        setupToolbar();
        setupBottomNavigation();
        loadUserProfileData();
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
        boolean isEnabled = ProfileSession.INSTANCE.isSkinWeatherNotiEnabled(context);
        updateSwitchUI(getBinding().switchSkinWeatherForecast, getBinding().switchSkinWeatherForecastThumb, isEnabled);

        getBinding().btnToggleSkinWeatherNoti.setOnClickListener(v -> {
            boolean nextState = !ProfileSession.INSTANCE.isSkinWeatherNotiEnabled(context);
            ProfileSession.INSTANCE.setSkinWeatherNotiEnabled(context, nextState);
            updateSwitchUI(getBinding().switchSkinWeatherForecast, getBinding().switchSkinWeatherForecastThumb, nextState);
            if (nextState) {
                DailySkinWeatherScheduler.scheduleDailyNotification(context);
                Toast.makeText(context, "Đã bật thông báo thời tiết và da lúc 06:30 sáng", Toast.LENGTH_SHORT).show();
                Intent testIntent = new Intent(context, DailySkinWeatherReceiver.class);
                context.sendBroadcast(testIntent);
            } else {
                DailySkinWeatherScheduler.cancelDailyNotification(context);
                Toast.makeText(context, "Đã tắt thông báo thời tiết và da hàng ngày", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSwitchUI(FrameLayout container, ImageView thumb, boolean enabled) {
        if (enabled) {
            container.setBackgroundResource(R.drawable.ic_switch_track_on_yellow);
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
            boolean isEnabled = ProfileSession.INSTANCE.isSkinWeatherNotiEnabled(requireContext());
            updateSwitchUI(getBinding().switchSkinWeatherForecast, getBinding().switchSkinWeatherForecastThumb, isEnabled);
            loadUserProfileData();
        }
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
        String username = ProfileSession.INSTANCE.getFullName(requireContext());

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour <= 10) greeting = "CHÀO BUỔI SÁNG";
        else if (hour >= 11 && hour <= 13) greeting = "CHÀO BUỔI TRƯA";
        else if (hour >= 14 && hour <= 17) greeting = "CHÀO BUỔI CHIỀU";
        else greeting = "CHÀO BUỔI TỐI";

        getBinding().tvGreeting.setText(greeting);
        getBinding().tvUsername.setText(username);

        String avatarUrl = ProfileSession.INSTANCE.getAvatar(requireContext());
        AvatarLoader.INSTANCE.loadAvatar(getBinding().ivAvatar, avatarUrl);
    }

    private void updateRoutineLabels(String skinType, double temp, int humidity, double uv, int pm25) {
        String skinLower = skinType.toLowerCase();
        boolean isOily = skinLower.contains("dầu");
        boolean isSensitive = skinLower.contains("nhạy cảm") || skinLower.contains("kích ứng");
        boolean isDry = skinLower.contains("khô");
        boolean isCombination = skinLower.contains("hỗn hợp");
        boolean isAging = skinLower.contains("lão hóa");
        boolean isDehydrated = skinLower.contains("mất nước");

        Map<String, SkinWeatherProductMatcher.RecommendedProduct> matchedProducts = SkinWeatherProductMatcher.matchProductsForWeatherAndSkin(requireContext(), temp, humidity, skinType);

        // 1. Cleanser
        SkinWeatherProductMatcher.RecommendedProduct matchedCleanser = matchedProducts.get("Cleanser");
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

        String finalCleanserSub = pm25 >= 50 ? cleanserSub + " & sạch sâu bụi mịn PM2.5" : cleanserSub;

        // 2. Serum
        SkinWeatherProductMatcher.RecommendedProduct matchedSerum = matchedProducts.get("Serum");
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
        SkinWeatherProductMatcher.RecommendedProduct matchedMoisturizer = matchedProducts.get("Moisturizer");
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
        SkinWeatherProductMatcher.RecommendedProduct matchedSunscreen = matchedProducts.get("Sunscreen");
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
    }

    private void setupRoutineCard(View card, String name, String sub, SkinWeatherProductMatcher.RecommendedProduct product) {
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
        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getMain(), null, (coroutineScope, continuation) -> {
            RootieDatabase db = RootieDatabase.Companion.getDatabase(requireContext());
            ProductEntity[] productRef = new ProductEntity[1];
            
            BuildersKt.withContext(Dispatchers.getIO(), (coroutineScopeIO, contIO) -> {
                try {
                    if (productId != null && !productId.isEmpty()) {
                        productRef[0] = db.productDao().getProductById(productId, contIO);
                    }
                    if (productRef[0] == null && productName != null && !productName.isEmpty()) {
                        List<ProductEntity> all = (List<ProductEntity>) FlowKt.first(db.productDao().getAllProducts(), contIO);
                        for (ProductEntity p : all) {
                            if (p.getName().equalsIgnoreCase(productName)) {
                                productRef[0] = p;
                                break;
                            }
                        }
                        if (productRef[0] == null) {
                            for (ProductEntity p : all) {
                                if (p.getName().toLowerCase().contains(productName.toLowerCase())) {
                                    productRef[0] = p;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return kotlin.Unit.INSTANCE;
            }, continuation);

            if (productRef[0] != null) {
                ShopDetailFragment detailFragment = new ShopDetailFragment();
                detailFragment.setProduct(productRef[0]);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), "Sản phẩm không có sẵn trên cửa hàng!", Toast.LENGTH_SHORT).show();
            }
            return kotlin.Unit.INSTANCE;
        });
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

                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location loc) {
                        loadWeatherForCoordinates(loc.getLatitude(), loc.getLongitude());
                        locationManager.removeUpdates(this);
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
                    locationManager.requestLocationUpdates(provider, 0L, 0f, locationListener, Looper.getMainLooper());
                }
            }
        } catch (SecurityException e) {
            loadWeatherForCoordinates(defaultLat, defaultLng);
        }
    }

    private String getCityName(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
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
        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getIO(), null, (coroutineScope, continuation) -> {
            String cityName = getCityName(lat, lng);
            try {
                String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lng + "&current=temperature_2m,relative_humidity_2m&daily=uv_index_max&timezone=auto";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                double[] tempRef = {32.0};
                int[] humidityRef = {68};
                double[] uvRef = {9.2};
                boolean[] successRef = {false};

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = connection.getInputStream();
                    Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    is.close();

                    JSONObject json = new JSONObject(response);
                    JSONObject current = json.getJSONObject("current");
                    tempRef[0] = current.getDouble("temperature_2m");
                    humidityRef[0] = current.getInt("relative_humidity_2m");

                    JSONObject daily = json.getJSONObject("daily");
                    JSONArray uvArray = daily.getJSONArray("uv_index_max");
                    uvRef[0] = uvArray.length() > 0 ? uvArray.getDouble(0) : 5.0;
                    successRef[0] = true;
                }

                double[] realPm25Ref = {-1.0};
                if (successRef[0]) {
                    try {
                        String aqUrlString = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + lat + "&longitude=" + lng + "&current=pm2_5&timezone=auto";
                        URL aqUrl = new URL(aqUrlString);
                        HttpURLConnection aqConnection = (HttpURLConnection) aqUrl.openConnection();
                        aqConnection.setRequestMethod("GET");
                        aqConnection.setConnectTimeout(5000);
                        aqConnection.setReadTimeout(5000);
                        if (aqConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            InputStream aqIs = aqConnection.getInputStream();
                            Scanner aqScanner = new Scanner(aqIs, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
                            String aqResponse = aqScanner.hasNext() ? aqScanner.next() : "";
                            aqIs.close();

                            JSONObject aqJson = new JSONObject(aqResponse);
                            JSONObject aqCurrent = aqJson.getJSONObject("current");
                            realPm25Ref[0] = aqCurrent.getDouble("pm2_5");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                BuildersKt.withContext(Dispatchers.getMain(), (csMain, contMain) -> {
                    if (successRef[0]) {
                        updateWeatherUI(tempRef[0], humidityRef[0], uvRef[0], realPm25Ref[0], cityName, lat, lng);
                    } else {
                        useFallbackWeatherData(cityName, lat, lng);
                    }
                    return kotlin.Unit.INSTANCE;
                }, continuation);

            } catch (Exception e) {
                e.printStackTrace();
                BuildersKt.withContext(Dispatchers.getMain(), (csMain, contMain) -> {
                    useFallbackWeatherData(cityName, lat, lng);
                    return kotlin.Unit.INSTANCE;
                }, continuation);
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    private void useFallbackWeatherData(String cityName, double lat, double lng) {
        updateWeatherUI(32.0, 68, 9.2, -1.0, cityName, lat, lng);
    }

    private void updateWeatherUI(double temp, int humidity, double uv, double pm25, String cityName, double lat, double lng) {
        if (_binding == null) return;

        String weatherCondition = temp >= 33 ? "NẮNG NÓNG GAY GẮT" :
                                  temp >= 28 ? "NẮNG NHIỀU, OI NHẸ" : "MÁT MẺ, DỄ CHỊU";
        
        int pm25Val = pm25 >= 0 ? (int) pm25 : (int) (15 + (temp * 0.4) + (humidity * 0.1));

        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
            prefs.edit()
                 .putFloat("SAVED_WEATHER_TEMP", (float) temp)
                 .putInt("SAVED_WEATHER_HUMIDITY", humidity)
                 .putFloat("SAVED_WEATHER_UV", (float) uv)
                 .putInt("SAVED_WEATHER_PM25", pm25Val)
                 .putString("SAVED_WEATHER_CITY", cityName)
                 .putString("SAVED_WEATHER_CONDITION", weatherCondition)
                 .putFloat("SAVED_WEATHER_LAT", (float) lat)
                 .putFloat("SAVED_WEATHER_LNG", (float) lng)
                 .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }

        getBinding().tvLocation.setText(cityName);
        getBinding().tvTemperature.setText(String.valueOf((int) temp));
        getBinding().tvWeatherCondition.setText(weatherCondition);
        getBinding().tvWeatherCondition.setTextColor(ContextCompat.getColor(requireContext(), 
            temp >= 33 ? R.color.status_level_red :
            temp >= 28 ? R.color.status_level_yellow : R.color.secondary));

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isNight = hour < 6 || hour >= 18;
        if (isNight) {
            getBinding().ivWeatherIcon.setImageResource(R.drawable.ic_skin_moon);
            getBinding().ivWeatherIcon.setColorFilter(Color.parseColor("#90A4AE"));
        } else {
            getBinding().ivWeatherIcon.setImageResource(R.drawable.ic_weather_sun);
            getBinding().ivWeatherIcon.setColorFilter(Color.parseColor("#F0C43D"));
        }

        getBinding().tvHumidity.setText(humidity + "%");
        String humidityLevel = humidity < 40 ? "Thấp" : humidity <= 65 ? "Trung bình" : "Cao";
        getBinding().tvHumidityLevel.setText(humidityLevel);

        int humidityColorRes = humidity < 40 ? R.color.status_level_orange : R.color.status_level_blue;
        int humidityBgRes = humidity < 40 ? R.drawable.bg_card_status_orange : R.drawable.bg_card_status_blue;
        int humidityColorVal = ContextCompat.getColor(requireContext(), humidityColorRes);
        getBinding().tvHumidity.setTextColor(humidityColorVal);
        getBinding().tvHumidityLevel.setTextColor(humidityColorVal);
        getBinding().layoutHumidityBox.setBackgroundResource(humidityBgRes);

        getBinding().tvUvIndex.setText(String.format(Locale.US, "%.1f", uv));
        String uvLevel = uv < 3 ? "Thấp" : uv < 6 ? "Trung bình" : uv < 8 ? "Cao" : "Nguy hiểm";
        getBinding().tvUvLevel.setText(uvLevel);

        int uvColorRes = uv < 3 ? R.color.status_level_green : uv < 6 ? R.color.status_level_yellow : uv < 8 ? R.color.status_level_orange : R.color.status_level_red;
        int uvBgRes = uv < 3 ? R.drawable.bg_card_status_green : uv < 6 ? R.drawable.bg_card_status_yellow : uv < 8 ? R.drawable.bg_card_status_orange : R.drawable.bg_card_status_red;
        int uvColorVal = ContextCompat.getColor(requireContext(), uvColorRes);
        getBinding().tvUvIndex.setTextColor(uvColorVal);
        getBinding().tvUvLevel.setTextColor(uvColorVal);
        getBinding().layoutUvBox.setBackgroundResource(uvBgRes);

        getBinding().tvDustIndex.setText(String.valueOf(pm25Val));
        String dustLevel = pm25Val < 25 ? "Tốt" : pm25Val < 50 ? "Trung bình" : "Kém";
        getBinding().tvDustLevel.setText(dustLevel);

        int dustColorRes = pm25Val < 25 ? R.color.status_level_green : pm25Val < 50 ? R.color.status_level_yellow : R.color.status_level_red;
        int dustBgRes = pm25Val < 25 ? R.drawable.bg_card_status_green : pm25Val < 50 ? R.drawable.bg_card_status_yellow : R.drawable.bg_card_status_red;
        int dustColorVal = ContextCompat.getColor(requireContext(), dustColorRes);
        getBinding().tvDustIndex.setTextColor(dustColorVal);
        getBinding().tvDustLevel.setTextColor(dustColorVal);
        getBinding().layoutDustBox.setBackgroundResource(dustBgRes);

        String warningMsg = uv >= 8 ? "Hôm nay chỉ số UV ở mức nguy hiểm (" + String.format(Locale.US, "%.1f", uv) + "). Bạn hãy bôi kem chống nắng kỹ và che chắn khi ra ngoài!" :
                            humidity < 40 ? "Độ ẩm không khí hôm nay khá thấp (" + humidity + "%). Da bạn sẽ mất nước nhanh, hãy chú ý cấp khóa ẩm!" :
                            "Hôm nay thời tiết khá nóng và độ ẩm trung bình. Hãy bảo vệ da khỏi tia UV hại và uống đủ nước!";
        getBinding().tvWarningText.setText(warningMsg);

        int warningBgRes, warningTextColRes;
        if (uv >= 8 || pm25Val >= 50) {
            warningBgRes = R.drawable.bg_warning_red;
            warningTextColRes = R.color.warning_text_red;
        } else if (humidity < 40 || uv >= 5.0 || pm25Val >= 25) {
            warningBgRes = R.drawable.bg_warning_orange;
            warningTextColRes = R.color.warning_text_orange;
        } else {
            warningBgRes = R.drawable.bg_warning_yellow;
            warningTextColRes = R.color.warning_text_yellow;
        }
        
        getBinding().layoutWarningBox.setBackgroundResource(warningBgRes);
        int warningTextColorVal = ContextCompat.getColor(requireContext(), warningTextColRes);
        getBinding().tvWarningText.setTextColor(warningTextColorVal);
        getBinding().ivWarningIcon.setColorFilter(warningTextColorVal);

        String alertText = uv >= 8 ? "Chỉ số UV hôm nay cực cao! Nhớ thoa lại kem chống nắng sau mỗi 2 giờ ra ngoài." :
                           temp >= 33 ? "Nhiệt độ nóng gay gắt, hãy dùng gel dưỡng ẩm mỏng nhẹ để tránh bít tắc lỗ chân lông." :
                           "Hôm nay da bạn cần chống nắng và cấp ẩm nhiều hơn. Đừng quên nhé!";
        getBinding().tvAlertText.setText(alertText);

        SharedPreferences prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        String skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da dầu nhạy cảm");
        if (skinType == null) skinType = "Da dầu nhạy cảm";

        String skinLower = skinType.toLowerCase();
        boolean isOily = skinLower.contains("dầu");
        boolean isSensitive = skinLower.contains("nhạy cảm") || skinLower.contains("kích ứng");
        boolean isDry = skinLower.contains("khô");
        boolean isCombination = skinLower.contains("hỗn hợp");
        boolean isNormal = skinLower.contains("thường");
        boolean isDehydrated = skinLower.contains("mất nước");

        int baseOily = isOily ? 70 : isCombination ? 55 : isDehydrated ? 50 : isNormal ? 40 : isDry ? 20 : 45;
        baseOily += temp >= 33 ? 15 : temp >= 28 ? 8 : temp < 22 ? -5 : 0;
        int oilyPercent = Math.max(10, Math.min(95, baseOily));

        int baseHydration = isNormal ? 70 : isOily ? 60 : isCombination ? 55 : isSensitive ? 50 : isDry ? 35 : isDehydrated ? 25 : 50;
        baseHydration += humidity < 40 ? -15 : humidity < 55 ? -5 : humidity > 75 ? 10 : 0;
        if (uv >= 8.0) baseHydration -= 5;
        int hydrationPercent = Math.max(10, Math.min(95, baseHydration));

        int baseSensitivity = isSensitive ? 55 : isDehydrated ? 35 : isDry ? 30 : isCombination ? 20 : isOily ? 20 : 15;
        baseSensitivity += uv >= 8.0 ? 20 : uv >= 5.0 ? 10 : 0;
        if (temp >= 33) baseSensitivity += 10;
        if (pm25Val >= 50) baseSensitivity += 15; else if (pm25Val >= 25) baseSensitivity += 5;
        int sensitivityPercent = Math.max(5, Math.min(95, baseSensitivity));

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
        if (isOily) {
            uvImpact = uv >= 6.0 ? "Tăng bã nhờn & sạm da" : "Tăng tiết bã nhờn";
            hydrationImpact = humidity < 55 ? "Dầu nước mất cân bằng" : "Độ ẩm cân bằng";
            dustImpact = pm25Val >= 50 ? "Nguy cơ tắc nghẽn mụn" : "Bám dính dầu nhờn";
        } else if (isDry) {
            uvImpact = uv >= 6.0 ? "Dễ rát & sạm da khô" : "Dễ mất ẩm bong tróc";
            hydrationImpact = humidity < 55 ? "Thiếu nước nghiêm trọng" : "Giảm khô căng nhẹ";
            dustImpact = pm25Val >= 50 ? "Hàng rào bảo vệ yếu" : "Khô ngứa do khói bụi";
        } else if (isSensitive) {
            uvImpact = uv >= 5.0 ? "Nguy cơ đỏ rát cao" : "Dễ kích ứng nhẹ";
            hydrationImpact = humidity < 55 ? "Màng ẩm tổn thương" : "Đủ ẩm dễ chịu";
            dustImpact = pm25Val >= 50 ? "Mẩn ngứa bít tắc" : "Bám dính bụi bẩn";
        } else if (isCombination) {
            uvImpact = uv >= 6.0 ? "Vùng chữ T nhờn rát" : "Vùng chữ T tiết dầu";
            hydrationImpact = humidity < 55 ? "Hai bên má khô rát" : "Cân bằng vùng má";
            dustImpact = pm25Val >= 50 ? "Bít tắc vùng chữ T" : "Tích tụ bụi nhẹ";
        } else if (isAging) {
            uvImpact = uv >= 6.0 ? "Tia UV gây sạm nám" : "Đẩy nhanh lão hóa";
            hydrationImpact = humidity < 55 ? "Mất nước nếp nhăn sâu" : "Giữ ẩm tế bào";
            dustImpact = pm25Val >= 50 ? "Tổn thương gốc tự do" : "Tác nhân ô nhiễm";
        } else if (isDehydrated) {
            uvImpact = uv >= 6.0 ? "Rát sạm khô căng" : "Nếp nhăn giả do UV";
            hydrationImpact = humidity < 45 ? "Mất nước tế bào sâu" : "Cải thiện tình trạng khô";
            dustImpact = pm25Val >= 50 ? "Tắc tuyến bã nhờn" : "Bụi gây khô bề mặt";
        } else {
            uvImpact = uv >= 6.0 ? "Chống nắng tối đa" : "Bảo vệ dịu nhẹ";
            hydrationImpact = humidity < 55 ? "Cần cấp ẩm nhẹ" : "Duy trì ẩm tốt";
            dustImpact = pm25Val >= 50 ? "Làm sạch sâu bụi mịn" : "Làm sạch bình thường";
        }

        getBinding().tvUvImpact.setText(uvImpact);
        getBinding().tvHydrationImpact.setText(hydrationImpact);
        getBinding().tvDustImpact.setText(dustImpact);

        updateRoutineLabels(skinType, temp, humidity, uv, pm25Val);
        fetchGeminiInsight(temp, humidity, uv, pm25Val, cityName, skinType, oilyPercent, hydrationPercent, sensitivityPercent);
    }

    private void fetchGeminiInsight(double temp, int humidity, double uv, int pm25, String cityName, String skinType, int oily, int hydration, int sensitivity) {
        SharedPreferences prefs = requireContext().getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.trim().isEmpty() || GEMINI_API_KEY.equals("YOUR_GEMINI_API_KEY_HERE")) {
            updateRuleBasedAiInsight(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity);
            return;
        }

        BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getIO(), null, (coroutineScope, continuation) -> {
            try {
                String urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setDoOutput(true);

                JSONObject requestJson = new JSONObject();
                JSONArray partsArray = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", "Đưa ra lời khuyên da liễu cho người dùng có làn da: " + skinType + ". Thời tiết hiện tại: Nhiệt độ " + temp + "°C, Độ ẩm " + humidity + "%, Chỉ số UV " + String.format(Locale.US, "%.1f", uv) + ", Bụi mịn PM2.5 " + pm25 + ".");
                partsArray.put(textPart);

                JSONArray contentsArray = new JSONArray();
                JSONObject contentsObj = new JSONObject();
                contentsObj.put("parts", partsArray);
                contentsArray.put(contentsObj);
                requestJson.put("contents", contentsArray);

                JSONObject systemInstruction = new JSONObject();
                JSONArray systemParts = new JSONArray();
                JSONObject sysTextPart = new JSONObject();
                sysTextPart.put("text", "Bạn là trợ lý bác sĩ da liễu thông minh của ứng dụng ROOTIE - ứng dụng tư vấn chăm sóc da hữu cơ và thuần chay (Vegan Skincare). Nhiệm vụ của bạn là đưa ra 1 lời khuyên ngắn gọn, thiết thực và khoa học (tối đa 2-3 câu ngắn) bằng tiếng Việt cho người dùng dựa trên loại da và thời tiết thực tế được gửi. Khuyên dùng các giải pháp lành tính, tự nhiên, thuần chay, bảo vệ màng ẩm của da và khuyên thoa kem chống nắng/che chắn nếu UV cao. Hãy bắt đầu câu trả lời bằng ký tự mở ngoặc kép kép và kết thúc bằng đóng ngoặc kép kép (ví dụ: “Lời khuyên...”).");
                systemParts.put(sysTextPart);
                systemInstruction.put("parts", systemParts);
                requestJson.put("systemInstruction", systemInstruction);

                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", 0.7);
                generationConfig.put("maxOutputTokens", 250);
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
                            String textResult = parts.getJSONObject(0).getString("text").trim();
                            prefs.edit().putString("SAVED_CACHED_AI_INSIGHT", textResult).apply();

                            BuildersKt.withContext(Dispatchers.getMain(), (csMain, contMain) -> {
                                if (_binding != null) {
                                    getBinding().tvAiInsightDesc.setText(textResult);
                                    saveDiagnosticRecord(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity, textResult);
                                }
                                return kotlin.Unit.INSTANCE;
                            }, continuation);
                            return kotlin.Unit.INSTANCE;
                        }
                    }
                }

                BuildersKt.withContext(Dispatchers.getMain(), (csMain, contMain) -> {
                    String cachedInsight = prefs.getString("SAVED_CACHED_AI_INSIGHT", null);
                    if (cachedInsight != null && !cachedInsight.trim().isEmpty()) {
                        getBinding().tvAiInsightDesc.setText(cachedInsight);
                        saveDiagnosticRecord(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity, cachedInsight);
                    } else {
                        updateRuleBasedAiInsight(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity);
                    }
                    return kotlin.Unit.INSTANCE;
                }, continuation);

            } catch (Exception e) {
                e.printStackTrace();
                BuildersKt.withContext(Dispatchers.getMain(), (csMain, contMain) -> {
                    String cachedInsight = prefs.getString("SAVED_CACHED_AI_INSIGHT", null);
                    if (cachedInsight != null && !cachedInsight.trim().isEmpty()) {
                        getBinding().tvAiInsightDesc.setText(cachedInsight);
                        saveDiagnosticRecord(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity, cachedInsight);
                    } else {
                        updateRuleBasedAiInsight(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity);
                    }
                    return kotlin.Unit.INSTANCE;
                }, continuation);
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    private void updateRuleBasedAiInsight(double temp, int humidity, double uv, int pm25, String cityName, String skinType, int oily, int hydration, int sensitivity) {
        String baseInsight;
        String lowerSkin = skinType.toLowerCase();

        if (lowerSkin.contains("dầu nhạy cảm")) {
            if (uv >= 8) baseInsight = "Thời tiết nắng gắt với tia UV cực cao (" + String.format(Locale.US, "%.1f", uv) + "). Làn da dầu nhạy cảm rất dễ đỏ rát và kích ứng. Hãy ưu tiên bôi kem chống nắng vật lý mỏng nhẹ và che chắn kỹ.";
            else if (temp >= 33) baseInsight = "Nhiệt độ cao (" + temp + "°C) làm tăng tiết bã nhờn trong khi hàng rào bảo vệ da nhạy cảm mỏng yếu. Nên dùng gel dưỡng ẩm phục hồi dịu nhẹ và tránh chà xát da.";
            else if (pm25 >= 50) baseInsight = "Chỉ số bụi mịn cao (" + pm25 + ") có thể gây ngứa rát và bít tắc. Hãy chú ý làm sạch dịu nhẹ với sữa rửa mặt không bọt hoặc gel dịu nhẹ.";
            else baseInsight = "Lượng dầu tiết ra ổn định nhưng da vẫn nhạy cảm. Hãy củng cố hàng rào da bằng serum B5 và kem dưỡng phục hồi Ceramide.";
        } else if (lowerSkin.contains("dầu")) {
            if (temp >= 33) baseInsight = "Nhiệt độ cao (" + temp + "°C) kích thích tuyến bã nhờn hoạt động cực mạnh. Hãy dùng sữa rửa mặt kiềm dầu dịu nhẹ và gel dưỡng ẩm mỏng nhẹ dạng nước để tránh bít tắc mụn.";
            else if (humidity < 40) baseInsight = "Độ ẩm không khí thấp (" + humidity + "%) làm da mất nước bề mặt, dễ tiết dầu bù nhiều hơn. Hãy uống nhiều nước và cấp ẩm nhẹ nhàng bằng toner hyaluronic acid.";
            else baseInsight = "Lượng bã nhờn có thể tăng nhẹ. Ưu tiên serum Niacinamide để điều tiết dầu thừa và se khít lỗ chân lông.";
        } else if (lowerSkin.contains("khô")) {
            if (humidity < 40) baseInsight = "Độ ẩm không khí rất thấp (" + humidity + "%), dễ gây khô căng, bong tróc rát da. Hãy dùng serum HA cấp nước đa tầng kết hợp kem dưỡng ẩm dạng đặc để khóa ẩm sâu.";
            else if (temp >= 33) baseInsight = "Nhiệt độ cao (" + temp + "°C) làm tăng sự mất nước qua da. Đừng quên xịt khoáng làm dịu da và thoa kem chống nắng cấp ẩm để bảo vệ da.";
            else baseInsight = "Da khô cần duy trì độ đàn hồi tốt. Hãy đắp mặt nạ dưỡng ẩm và dùng kem dưỡng khóa ẩm dày hơn vào buổi tối.";
        } else if (lowerSkin.contains("nhạy cảm") || lowerSkin.contains("kích ứng")) {
            if (uv >= 6) baseInsight = "Tia UV cao (" + String.format(Locale.US, "%.1f", uv) + ") rất dễ làm tổn hại hàng rào bảo vệ da mỏng yếu. Hãy luôn thoa kem chống nắng vật lý dành cho da nhạy cảm trước khi ra ngoài.";
            else if (pm25 >= 50) baseInsight = "Không khí nhiều khói bụi (" + pm25 + ") dễ gây dị ứng mẩn đỏ. Nên làm sạch da nhẹ nhàng bằng nước tẩy trang micellar không chứa cồn ngay khi về nhà.";
            else baseInsight = "Da nhạy cảm cần sự tối giản. Tránh dùng sản phẩm chứa cồn khô, hương liệu hay retinol nồng độ cao vào hôm nay.";
        } else if (lowerSkin.contains("mụn")) {
            if (temp >= 33 || pm25 >= 50) baseInsight = "Nắng nóng (" + temp + "°C) và bụi mịn (" + pm25 + ") là tác nhân hàng đầu gây tắc nghẽn và sưng mụn. Hãy làm sạch sâu bằng gel rửa mặt chứa BHA/Salicylic Acid dịu nhẹ.";
            else baseInsight = "Tập trung kiểm soát dầu thừa và kháng viêm cho nốt mụn. Tránh sử dụng kem dưỡng ẩm quá dày gây bí bách da.";
        } else if (lowerSkin.contains("hỗn hợp")) {
            if (temp >= 33) baseInsight = "Vùng chữ T sẽ bóng nhờn nhiều do nhiệt độ cao (" + temp + "°C), trong khi hai bên má vẫn cần giữ ẩm. Hãy thoa lotion mỏng nhẹ toàn mặt và dùng giấy thấm dầu khi cần.";
            else baseInsight = "Cân bằng ẩm cho da hỗn hợp. Dùng toner cấp nước nhẹ nhàng và kem dưỡng ẩm mỏng để vùng chữ T thoáng sạch còn vùng má đủ ẩm.";
        } else {
            baseInsight = "Làn da của bạn tương đối ổn định hôm nay. Hãy duy trì thói quen làm sạch dịu nhẹ, cấp ẩm vừa đủ và bôi kem chống nắng bảo vệ da khỏi tia UV.";
        }

        String finalInsight = "“" + baseInsight + "”";
        getBinding().tvAiInsightDesc.setText(finalInsight);
        saveDiagnosticRecord(temp, humidity, uv, pm25, cityName, skinType, oily, hydration, sensitivity, finalInsight);
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
                    temp,
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

            SkinWeatherHistoryManager.INSTANCE.saveDiagnostic(requireContext(), diagnostic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupFeedbackButtons() {
        android.graphics.drawable.Drawable activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_btn_solid_feedback);
        android.graphics.drawable.Drawable inactiveBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_btn_outlined_feedback);

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
        LinearLayout navMySkin = getBinding().layoutBottomNav.navMyskin;
        ImageView icon = navMySkin.getChildCount() > 0 ? (ImageView) navMySkin.getChildAt(0) : null;
        TextView label = navMySkin.getChildCount() > 1 ? (TextView) navMySkin.getChildAt(1) : null;

        if (icon != null) icon.setColorFilter(Color.parseColor("#677559"));
        if (label != null) {
            label.setTextColor(Color.parseColor("#677559"));
            label.setTypeface(null, Typeface.BOLD);
        }

        getBinding().layoutBottomNav.navAccount.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountProfileFragment())
                    .commit();
        });

        getBinding().layoutBottomNav.navShop.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new ShopHomeFragment())
                    .commit();
        });

        getBinding().layoutBottomNav.navHome.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new HomeFragment())
                    .commit();
        });
    }

    @Override
    public void onDestroyView() {
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

        List<SkinWeatherDiagnostic> localHistory = SkinWeatherHistoryManager.INSTANCE.getHistory(requireContext());
        WeatherHistoryAdapter adapter = new WeatherHistoryAdapter(localHistory, selectedDiagnostic -> {
            displayHistoricalDiagnostic(selectedDiagnostic);
            dialog.dismiss();
            return null;
        });
        rvHistory.setAdapter(adapter);

        SkinWeatherHistoryManager.INSTANCE.syncFromFirestore(requireContext(), syncedList -> {
            if (isAdded() && _binding != null) {
                rvHistory.setAdapter(new WeatherHistoryAdapter(syncedList, selectedDiagnostic -> {
                    displayHistoricalDiagnostic(selectedDiagnostic);
                    dialog.dismiss();
                    return null;
                }));
            }
            return kotlin.Unit.INSTANCE;
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
            getBinding().ivWeatherIcon.setImageResource(R.drawable.ic_skin_moon);
            getBinding().ivWeatherIcon.setColorFilter(Color.parseColor("#90A4AE"));
        } else {
            getBinding().ivWeatherIcon.setImageResource(R.drawable.ic_weather_sun);
            getBinding().ivWeatherIcon.setColorFilter(Color.parseColor("#F0C43D"));
        }

        getBinding().tvHumidity.setText(diagnostic.getHumidity() + "%");
        String humidityLevel = diagnostic.getHumidity() < 40 ? "Thấp" : diagnostic.getHumidity() <= 65 ? "Trung bình" : "Cao";
        getBinding().tvHumidityLevel.setText(humidityLevel);

        int humidityColorRes = diagnostic.getHumidity() < 40 ? R.color.status_level_orange : R.color.status_level_blue;
        int humidityBgRes = diagnostic.getHumidity() < 40 ? R.drawable.bg_card_status_orange : R.drawable.bg_card_status_blue;
        int humidityColorVal = ContextCompat.getColor(requireContext(), humidityColorRes);
        getBinding().tvHumidity.setTextColor(humidityColorVal);
        getBinding().tvHumidityLevel.setTextColor(humidityColorVal);
        getBinding().layoutHumidityBox.setBackgroundResource(humidityBgRes);

        getBinding().tvUvIndex.setText(String.format(Locale.US, "%.1f", diagnostic.getUv()));
        String uvLevel = diagnostic.getUv() < 3 ? "Thấp" : diagnostic.getUv() < 6 ? "Trung bình" : diagnostic.getUv() < 8 ? "Cao" : "Nguy hiểm";
        getBinding().tvUvLevel.setText(uvLevel);

        int uvColorRes = diagnostic.getUv() < 3 ? R.color.status_level_green : diagnostic.getUv() < 6 ? R.color.status_level_yellow : diagnostic.getUv() < 8 ? R.color.status_level_orange : R.color.status_level_red;
        int uvBgRes = diagnostic.getUv() < 3 ? R.drawable.bg_card_status_green : diagnostic.getUv() < 6 ? R.drawable.bg_card_status_yellow : diagnostic.getUv() < 8 ? R.drawable.bg_card_status_orange : R.drawable.bg_card_status_red;
        int uvColorVal = ContextCompat.getColor(requireContext(), uvColorRes);
        getBinding().tvUvIndex.setTextColor(uvColorVal);
        getBinding().tvUvLevel.setTextColor(uvColorVal);
        getBinding().layoutUvBox.setBackgroundResource(uvBgRes);

        getBinding().tvDustIndex.setText(String.valueOf(diagnostic.getPm25()));
        String dustLevel = diagnostic.getPm25() < 25 ? "Tốt" : diagnostic.getPm25() < 50 ? "Trung bình" : "Kém";
        getBinding().tvDustLevel.setText(dustLevel);

        int dustColorRes = diagnostic.getPm25() < 25 ? R.color.status_level_green : diagnostic.getPm25() < 50 ? R.color.status_level_yellow : R.color.status_level_red;
        int dustBgRes = diagnostic.getPm25() < 25 ? R.drawable.bg_card_status_green : diagnostic.getPm25() < 50 ? R.drawable.bg_card_status_yellow : R.drawable.bg_card_status_red;
        int dustColorVal = ContextCompat.getColor(requireContext(), dustColorRes);
        getBinding().tvDustIndex.setTextColor(dustColorVal);
        getBinding().tvDustLevel.setTextColor(dustColorVal);
        getBinding().layoutDustBox.setBackgroundResource(dustBgRes);

        String warningMsg = diagnostic.getUv() >= 8 ? "Hôm nay chỉ số UV ở mức nguy hiểm (" + String.format(Locale.US, "%.1f", diagnostic.getUv()) + "). Bạn hãy bôi kem chống nắng kỹ và che chắn khi ra ngoài!" :
                            diagnostic.getHumidity() < 40 ? "Độ ẩm không khí hôm nay khá thấp (" + diagnostic.getHumidity() + "%). Da bạn sẽ mất nước nhanh, hãy chú ý cấp khóa ẩm!" :
                            "Hôm nay thời tiết khá nóng và độ ẩm trung bình. Hãy bảo vệ da khỏi tia UV hại và uống đủ nước!";
        getBinding().tvWarningText.setText(warningMsg);

        int warningBgRes, warningTextColRes;
        if (diagnostic.getUv() >= 8 || diagnostic.getPm25() >= 50) {
            warningBgRes = R.drawable.bg_warning_red;
            warningTextColRes = R.color.warning_text_red;
        } else if (diagnostic.getHumidity() < 40 || diagnostic.getUv() >= 5.0 || diagnostic.getPm25() >= 25) {
            warningBgRes = R.drawable.bg_warning_orange;
            warningTextColRes = R.color.warning_text_orange;
        } else {
            warningBgRes = R.drawable.bg_warning_yellow;
            warningTextColRes = R.color.warning_text_yellow;
        }
        getBinding().layoutWarningBox.setBackgroundResource(warningBgRes);
        int warningTextColorVal = ContextCompat.getColor(requireContext(), warningTextColRes);
        getBinding().tvWarningText.setTextColor(warningTextColorVal);
        getBinding().ivWarningIcon.setColorFilter(warningTextColorVal);

        String alertText = diagnostic.getUv() >= 8 ? "Chỉ số UV hôm nay cực cao! Nhớ thoa lại kem chống nắng sau mỗi 2 giờ ra ngoài." :
                           diagnostic.getTemperature() >= 33 ? "Nhiệt độ nóng gay gắt, hãy dùng gel dưỡng ẩm mỏng nhẹ để tránh bít tắc lỗ chân lông." :
                           "Hôm nay da bạn cần chống nắng và cấp ẩm nhiều hơn. Đừng quên nhé!";
        getBinding().tvAlertText.setText(alertText);

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
