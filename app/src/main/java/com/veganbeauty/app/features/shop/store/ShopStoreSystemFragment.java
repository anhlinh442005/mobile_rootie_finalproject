package com.veganbeauty.app.features.shop.store;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.repository.StoreRepository;
import com.veganbeauty.app.databinding.DialogLocationPermissionBinding;
import com.veganbeauty.app.databinding.ShopFragmentStoreSystemBinding;
import com.veganbeauty.app.databinding.ShopItemStoreCardHorizontalBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.BuildersKt;

public class ShopStoreSystemFragment extends RootieFragment {

    private ShopFragmentStoreSystemBinding _binding;
    private ShopFragmentStoreSystemBinding getBinding() {
        return _binding;
    }

    private RootieDatabase database;
    private StoreRepository repository;

    private List<StoreEntity> allStoresList = new ArrayList<>();
    private List<StoreEntity> displayedStoresList = new ArrayList<>();

    private StoreCardAdapter storeCardAdapter;
    private Location userLocation = null;
    private boolean notificationTriggered = false;
    private int selectedIndex = 0;
    private boolean hasCheckedPermissionOnStart = false;

    private LocationManager locationManager = null;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                Boolean fineGranted = permissions.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseGranted = permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                if ((fineGranted != null && fineGranted) || (coarseGranted != null && coarseGranted)) {
                    onPermissionGranted();
                } else {
                    onPermissionDenied();
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = RootieDatabase.getDatabase(requireContext());
        repository = new StoreRepository(database.storeDao(), new LocalJsonReader(requireContext()));
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ShopFragmentStoreSystemBinding.inflate(inflater, container, false);
        return getBinding().getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        getBinding().btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        getBinding().btnSearch.setOnClickListener(v -> {
            ShopStoreSelectionFragment selectionFragment = ShopStoreSelectionFragment.newInstance(
                    false, null, true, null, null
            );
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, selectionFragment)
                    .addToBackStack(null)
                    .commit();
        });

        getBinding().ivFilterIcon.setOnClickListener(v -> {
            ShopAddressSelectionFragment addressSelectionFragment = ShopAddressSelectionFragment.newInstance();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, addressSelectionFragment)
                    .addToBackStack(null)
                    .commit();
        });

        getParentFragmentManager().setFragmentResultListener(
                ShopAddressSelectionFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    String province = bundle.getString(ShopAddressSelectionFragment.RESULT_PROVINCE);
                    String district = bundle.getString(ShopAddressSelectionFragment.RESULT_DISTRICT);
                    if (province != null && !province.isEmpty() && district != null && !district.isEmpty()) {
                        ShopStoreSelectionFragment selectionFragment = ShopStoreSelectionFragment.newInstance(
                                false, null, false, province, district
                        );
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.main_container, selectionFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                }
        );

        getBinding().btnViewList.setOnClickListener(v -> {
            if (!displayedStoresList.isEmpty()) {
                ShopStoresBottomSheetFragment bottomSheet = ShopStoresBottomSheetFragment.newInstance(new ArrayList<>(displayedStoresList));
                bottomSheet.show(getParentFragmentManager(), ShopStoresBottomSheetFragment.TAG);
            }
        });

        ZoomPanTouchListener zoomPanTouchListener = new ZoomPanTouchListener(getBinding().flZoomableMapWrapper);
        getBinding().ivMapBackground.setOnTouchListener(zoomPanTouchListener);

        getBinding().btnUseMyLocation.setOnClickListener(v -> showCustomPermissionDialog());

        getBinding().btnGpsTarget.setOnClickListener(v -> fetchLocation());

        setupRecyclerView();
        loadStoresAndCheckPermission();
    }

    private void setupRecyclerView() {
        storeCardAdapter = new StoreCardAdapter();
        getBinding().rvStoreCards.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        getBinding().rvStoreCards.setAdapter(storeCardAdapter);

        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(getBinding().rvStoreCards);

        getBinding().rvStoreCards.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        View centerView = snapHelper.findSnapView(layoutManager);
                        if (centerView != null) {
                            int position = layoutManager.getPosition(centerView);
                            if (position != RecyclerView.NO_POSITION && position != selectedIndex) {
                                selectedIndex = position;
                                highlightPin(position);
                            }
                        }
                    }
                }
            }
        });
    }

    private void loadStoresAndCheckPermission() {
        BuildersKt.launch(
                LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()),
                kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
                kotlinx.coroutines.CoroutineStart.DEFAULT,
                (coroutineScope, continuation) -> {
                    repository.getAllStores().collect(new kotlinx.coroutines.flow.FlowCollector<List<StoreEntity>>() {
                        @Override
                        public Object emit(List<StoreEntity> stores, Continuation<? super Unit> continuation) {
                            allStoresList = stores;
                            if (allStoresList.isEmpty()) {
                                try {
                                    repository.refreshStores();
                                } catch (Exception ignored) {}
                            } else {
                                if (!hasCheckedPermissionOnStart) {
                                    hasCheckedPermissionOnStart = true;
                                    checkAndPromptPermission(true);
                                } else {
                                    checkAndPromptPermission(false);
                                }
                            }
                            return Unit.INSTANCE;
                        }
                    }, continuation);
                    return Unit.INSTANCE;
                }
        );
    }

    private void checkAndPromptPermission(boolean autoPrompt) {
        int finePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION);

        if (finePermission == PackageManager.PERMISSION_GRANTED || coarsePermission == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted();
        } else {
            if (autoPrompt) {
                showCustomPermissionDialog();
            } else {
                onPermissionDenied();
            }
        }
    }

    private void showCustomPermissionDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_location_permission, null);
        DialogLocationPermissionBinding dialogBinding = DialogLocationPermissionBinding.bind(dialogView);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogBinding.btnAllow.setOnClickListener(v -> {
            dialog.dismiss();
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        });

        dialogBinding.btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            onPermissionDenied();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void onPermissionGranted() {
        getBinding().btnUseMyLocation.setVisibility(View.GONE);
        getBinding().btnGpsTarget.setVisibility(View.VISIBLE);
        fetchLocation();
    }

    private void onPermissionDenied() {
        getBinding().btnUseMyLocation.setVisibility(View.VISIBLE);
        getBinding().btnGpsTarget.setVisibility(View.GONE);
        getBinding().pinUser.setVisibility(View.GONE);
        userLocation = null;

        List<StoreEntity> defaultStores = new ArrayList<>();
        for (StoreEntity s : allStoresList) {
            if (s.getTinhThanh().toLowerCase().contains("hà nội") || s.getDiaChiDayDu().toLowerCase().contains("hà nội")) {
                defaultStores.add(s);
                if (defaultStores.size() == 4) break;
            }
        }
        List<StoreEntity> finalStores = defaultStores.size() == 4 ? defaultStores : new ArrayList<>();
        if (finalStores.isEmpty()) {
            for (int i = 0; i < Math.min(4, allStoresList.size()); i++) {
                finalStores.add(allStoresList.get(i));
            }
        }

        updateMapWithPins(null, finalStores);
    }

    private void fetchLocation() {
        try {
            boolean hasFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            if (!hasFine && !hasCoarse) {
                onPermissionDenied();
                return;
            }

            Location location = null;

            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (location == null && locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location == null && locationManager != null && locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }

            if (location == null) {
                location = new Location("Mock");
                location.setLatitude(10.775);
                location.setLongitude(106.701);
            }

            Location finalLoc = location;
            userLocation = finalLoc;
            processStoresWithLocation(finalLoc);

            try {
                LocationListener listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location loc) {
                        if (_binding == null) return;
                        userLocation = loc;
                        processStoresWithLocation(loc);
                        if (locationManager != null) {
                            locationManager.removeUpdates(this);
                        }
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                };

                if (locationManager != null) {
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, listener);
                    } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, listener);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            onPermissionDenied();
        } catch (Exception e) {
            e.printStackTrace();
            onPermissionDenied();
        }
    }

    private void processStoresWithLocation(Location location) {
        if (allStoresList.isEmpty()) return;

        class StoreDistance {
            StoreEntity store;
            float distance;
            StoreDistance(StoreEntity store, float distance) {
                this.store = store;
                this.distance = distance;
            }
        }

        List<StoreDistance> sortedList = new ArrayList<>();
        for (StoreEntity store : allStoresList) {
            float[] results = new float[1];
            Location.distanceBetween(location.getLatitude(), location.getLongitude(), store.getLat(), store.getLng(), results);
            sortedList.add(new StoreDistance(store, results[0]));
        }

        Collections.sort(sortedList, new Comparator<StoreDistance>() {
            @Override
            public int compare(StoreDistance o1, StoreDistance o2) {
                return Float.compare(o1.distance, o2.distance);
            }
        });

        List<StoreEntity> nearbyStores = new ArrayList<>();
        for (StoreDistance sd : sortedList) {
            if (sd.distance < 12000f) {
                nearbyStores.add(sd.store);
                if (nearbyStores.size() == 6) break;
            }
        }

        List<StoreEntity> finalStores = nearbyStores;
        if (finalStores.isEmpty()) {
            finalStores = new ArrayList<>();
            for (int i = 0; i < Math.min(4, sortedList.size()); i++) {
                finalStores.add(sortedList.get(i).store);
            }
        }

        updateMapWithPins(location, finalStores);
    }

    private void updateMapWithPins(Location location, List<StoreEntity> stores) {
        displayedStoresList = stores;
        selectedIndex = 0;

        List<Double> lats = new ArrayList<>();
        List<Double> lngs = new ArrayList<>();

        for (StoreEntity store : stores) {
            lats.add(store.getLat());
            lngs.add(store.getLng());
        }
        if (location != null) {
            lats.add(location.getLatitude());
            lngs.add(location.getLongitude());
        }

        if (lats.isEmpty()) return;

        double minLat = Collections.min(lats);
        double maxLat = Collections.max(lats);
        double minLng = Collections.min(lngs);
        double maxLng = Collections.max(lngs);

        if (minLat == maxLat) { minLat -= 0.05; maxLat += 0.05; }
        if (minLng == maxLng) { minLng -= 0.05; maxLng += 0.05; }

        double latRange = maxLat - minLat;
        double lngRange = maxLng - minLng;

        FrameLayout[] storePins = new FrameLayout[]{
                getBinding().pinStore1, getBinding().pinStore2, getBinding().pinStore3,
                getBinding().pinStore4, getBinding().pinStore5, getBinding().pinStore6
        };

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(getBinding().clPinsOverlay);

        for (FrameLayout pin : storePins) {
            constraintSet.setVisibility(pin.getId(), View.GONE);
        }
        constraintSet.setVisibility(getBinding().pinUser.getId(), View.GONE);

        for (int idx = 0; idx < stores.size(); idx++) {
            if (idx < storePins.length) {
                StoreEntity store = stores.get(idx);
                FrameLayout pin = storePins[idx];
                constraintSet.setVisibility(pin.getId(), View.VISIBLE);

                float xBias = (float) (0.15 + 0.70 * ((store.getLng() - minLng) / lngRange));
                float yBias = (float) (0.15 + 0.70 * ((maxLat - store.getLat()) / latRange));

                constraintSet.setHorizontalBias(pin.getId(), Math.max(0.05f, Math.min(0.95f, xBias)));
                constraintSet.setVerticalBias(pin.getId(), Math.max(0.05f, Math.min(0.95f, yBias)));

                int finalIdx = idx;
                pin.setOnClickListener(v -> {
                    getBinding().rvStoreCards.smoothScrollToPosition(finalIdx);
                    selectedIndex = finalIdx;
                    highlightPin(finalIdx);
                });
            }
        }

        if (location != null) {
            FrameLayout userPin = getBinding().pinUser;
            constraintSet.setVisibility(userPin.getId(), View.VISIBLE);

            float xBias = (float) (0.15 + 0.70 * ((location.getLongitude() - minLng) / lngRange));
            float yBias = (float) (0.15 + 0.70 * ((maxLat - location.getLatitude()) / latRange));

            constraintSet.setHorizontalBias(userPin.getId(), Math.max(0.05f, Math.min(0.95f, xBias)));
            constraintSet.setVerticalBias(userPin.getId(), Math.max(0.05f, Math.min(0.95f, yBias)));
        }

        constraintSet.applyTo(getBinding().clPinsOverlay);

        getBinding().tvCountLabel.setText("Tìm kiếm " + stores.size() + " cửa hàng");
        storeCardAdapter.updateItems(stores);

        highlightPin(0);

        if (location != null) {
            checkProximityAndNotify(location, stores);
        }
    }

    private void highlightPin(int position) {
        FrameLayout[] storePins = new FrameLayout[]{
                getBinding().pinStore1, getBinding().pinStore2, getBinding().pinStore3,
                getBinding().pinStore4, getBinding().pinStore5, getBinding().pinStore6
        };
        for (int index = 0; index < storePins.length; index++) {
            FrameLayout frameLayout = storePins[index];
            if (index == position) {
                frameLayout.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
                frameLayout.setElevation(8f);
            } else {
                frameLayout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7E8A83")));
                frameLayout.setElevation(2f);
            }
        }
    }

    private void checkProximityAndNotify(Location location, List<StoreEntity> stores) {
        if (notificationTriggered || stores.isEmpty()) return;

        StoreEntity closestStore = stores.get(0);
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), closestStore.getLat(), closestStore.getLng(), results);
        float distance = results[0];

        boolean isMockLocation = (location.getLatitude() == 10.775 && location.getLongitude() == 106.701);
        if (distance < 1500f || isMockLocation) {
            notificationTriggered = true;
            showDealNotifications(closestStore);
        }
    }

    private void showDealNotifications(StoreEntity store) {
        Context ctx = getContext();
        if (ctx == null) return;
        String titleText = "Ưu đãi gần bạn!";
        String messageText = "Bạn đang ở gần cửa hàng " + store.getTenCuaHang() + ", vào săn deal ngay!";

        if (_binding != null) {
            getBinding().tvNotiMessage.setText(messageText);
            getBinding().cvNotificationBanner.setVisibility(View.VISIBLE);
            getBinding().cvNotificationBanner.setTranslationY(-300f);

            getBinding().cvNotificationBanner.animate()
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (_binding != null) {
                    getBinding().cvNotificationBanner.animate()
                            .translationY(-300f)
                            .setDuration(500)
                            .withEndAction(() -> {
                                if (_binding != null) {
                                    getBinding().cvNotificationBanner.setVisibility(View.GONE);
                                }
                            })
                            .start();
                }
            }, 5000);
        }

        try {
            String channelId = "proximity_deal_channel";
            String channelName = "Ưu đãi gần bạn";
            NotificationManager notiManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Thông báo khi bạn ở gần cửa hàng Rootie");
                if (notiManager != null) {
                    notiManager.createNotificationChannel(channel);
                }
            }

            Uri mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + store.getLat() + "," + store.getLng());
            Intent intent = new Intent(Intent.ACTION_VIEW, mapUri);
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                    ctx, 0, intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(titleText)
                    .setContentText(messageText)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            if (notiManager != null) {
                notiManager.notify(101, builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openGoogleMaps(double lat, double lng) {
        try {
            Uri mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
            startActivity(mapIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Không thể mở Google Maps", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }

    public class StoreCardAdapter extends RecyclerView.Adapter<StoreCardAdapter.ViewHolder> {

        private List<StoreEntity> items = new ArrayList<>();

        public class ViewHolder extends RecyclerView.ViewHolder {
            ShopItemStoreCardHorizontalBinding itemBinding;

            public ViewHolder(ShopItemStoreCardHorizontalBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ShopItemStoreCardHorizontalBinding binding = ShopItemStoreCardHorizontalBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StoreEntity store = items.get(position);
            ShopItemStoreCardHorizontalBinding b = holder.itemBinding;

            b.tvStoreName.setText(store.getTenCuaHang());
            b.tvStoreHours.setText("Mở cửa từ " + (store.getMoCua() != null ? store.getMoCua() : "") + " đến " + (store.getDongCua() != null ? store.getDongCua() : ""));
            b.tvStoreAddress.setText(store.getDiaChiDayDu());

            b.btnDirections.setOnClickListener(v -> openGoogleMaps(store.getLat(), store.getLng()));

            b.getRoot().setOnClickListener(v -> {
                selectedIndex = position;
                highlightPin(position);
                ShopStoreDetailFragment detailFragment = ShopStoreDetailFragment.newInstance(store);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void updateItems(List<StoreEntity> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }
    }
}

class ZoomPanTouchListener implements View.OnTouchListener {
    private final View container;
    private float scaleFactor = 1.0f;
    private final float minScale = 1.0f;
    private final float maxScale = 5.0f;

    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    private final ScaleGestureDetector scaleGestureDetector;

    public ZoomPanTouchListener(View container) {
        this.container = container;
        scaleGestureDetector = new ScaleGestureDetector(
                container.getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        scaleFactor *= detector.getScaleFactor();
                        scaleFactor = Math.max(minScale, Math.min(maxScale, scaleFactor));
                        container.setScaleX(scaleFactor);
                        container.setScaleY(scaleFactor);
                        adjustTranslationBoundaries();
                        return true;
                    }
                }
        );
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                int index = event.getActionIndex();
                lastTouchX = event.getX(index);
                lastTouchY = event.getY(index);
                activePointerId = event.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex != -1) {
                    float x = event.getX(pointerIndex);
                    float y = event.getY(pointerIndex);

                    if (!scaleGestureDetector.isInProgress()) {
                        float dx = x - lastTouchX;
                        float dy = y - lastTouchY;

                        container.setTranslationX(container.getTranslationX() + dx);
                        container.setTranslationY(container.getTranslationY() + dy);
                        adjustTranslationBoundaries();
                    }

                    lastTouchX = x;
                    lastTouchY = y;
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    lastTouchX = event.getX(newPointerIndex);
                    lastTouchY = event.getY(newPointerIndex);
                    activePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }
        }
        return true;
    }

    private void adjustTranslationBoundaries() {
        float width = container.getWidth();
        float height = container.getHeight();

        float maxTx = (width * (scaleFactor - 1.0f)) / 2f;
        float maxTy = (height * (scaleFactor - 1.0f)) / 2f;

        container.setTranslationX(Math.max(-maxTx, Math.min(maxTx, container.getTranslationX())));
        container.setTranslationY(Math.max(-maxTy, Math.min(maxTy, container.getTranslationY())));
    }
}
