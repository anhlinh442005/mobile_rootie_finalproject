package com.veganbeauty.app.features.profile;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.lifecycle.LifecycleOwnerKt;

import com.veganbeauty.app.MainActivity;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.databinding.AccountProfileBinding;
import com.veganbeauty.app.features.account.checkin.AccountCheckinFragment;
import com.veganbeauty.app.features.account.expiry.AccountProductExpiryFragment;
import com.veganbeauty.app.features.account.order.AccountOrderListFragment;
import com.veganbeauty.app.features.account.reward.AccountRewardFragment;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.home.HomeHeaderHelper;
import com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity;
import com.veganbeauty.app.features.myskin.BookingHistoryFragment;
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment;
import com.veganbeauty.app.features.routine.SkinReminderFragment;
import com.veganbeauty.app.features.shop.store.ShopStoreDetailFragment;
import com.veganbeauty.app.features.shop.store.StoreProximityHelper;
import com.veganbeauty.app.features.weather.SkinWeatherForecastFragment;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.features.auth.FreshDemoAccountSeeder;
import com.veganbeauty.app.utils.AvatarLoader;
import com.veganbeauty.app.utils.NavAppUtils;
import com.veganbeauty.app.utils.ProfileSessionHelper;
import com.veganbeauty.app.utils.ProfileUpdateNotifier;
import com.veganbeauty.app.utils.RootieLocationHelper;
import com.veganbeauty.app.utils.SyncDataHelper;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import kotlin.Unit;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class AccountProfileFragment extends RootieFragment {

    private AccountProfileBinding binding;
    private OrderRepository orderRepository;
    @Nullable
    private StoreEntity nearestStore;
    @Nullable
    private Location latestUserLocation;

    private final ActivityResultLauncher<String[]> requestLocationLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                Boolean fineGranted = permissions.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseGranted = permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                    startNearestStoreLocationUpdates();
                } else if (binding != null && isAdded()) {
                    binding.tvNearestStoreHint.setText("Vui lòng cho phép định vị để biết chi nhánh gần bạn nhất");
                }
            }
    );

    private final ProfileUpdateNotifier.Listener profileUpdateListener = () -> refreshProfileUiFromSession();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        Context ctx = requireContext();
        boolean isLoggedIn = ProfileSession.INSTANCE.isLoggedIn(ctx);

        HomeHeaderHelper.setup(this, binding.getRoot());

        if (isLoggedIn) {
            ProfileSessionHelper.restoreLocalAvatarIfPresent(ctx);
            bindProfileAvatar(ctx);
            refreshProfileUiFromSession();
            loadUserProfileData(ctx);
        } else {
            AvatarLoader.loadAvatar(binding.ivAvatar, "");
        }

        View.OnClickListener guestRedirectListener = v -> {
            Intent intent = new Intent(ctx, HomeWelcomeActivity.class);
            intent.putExtra("DIRECT_LOGIN", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        };

        if (isLoggedIn) {
            String fullName = ProfileSession.INSTANCE.getFullName(ctx);
            String email = ProfileSession.INSTANCE.getEmail(ctx);
            binding.tvUsername.setText(fullName);
            binding.tvEmail.setText(email);
        } else {
            binding.tvUsername.setText("Khách hàng");
            binding.tvEmail.setText("Chạm để đăng nhập");
            binding.tvUsername.setOnClickListener(guestRedirectListener);
            binding.tvEmail.setOnClickListener(guestRedirectListener);
            binding.ivAvatar.setOnClickListener(guestRedirectListener);
        }

        SharedPreferences prefs = ctx.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        String savedSkinType = prefs.getString("SAVED_USER_SKIN_TYPE", null);
        if (savedSkinType != null) {
            binding.tvProfileSkinType.setText(savedSkinType);
            binding.llProfileSkinBadge.setVisibility(View.VISIBLE);
        } else {
            binding.tvProfileSkinType.setText("Chưa làm quiz");
        }

        binding.btnExpiryShelf.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountProductExpiryFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss());

        setSecuredOnClickListener(binding.btnAllOrders, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Tất cả"))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        setSecuredOnClickListener(binding.btnStatusPending, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Chờ xác nhận"))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        setSecuredOnClickListener(binding.btnStatusProcessing, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Đang xử lý"))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        setSecuredOnClickListener(binding.btnStatusDelivering, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Đang giao"))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        setSecuredOnClickListener(binding.btnStatusSuccess, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Hoàn tất"))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        setSecuredOnClickListener(binding.btnStatusCancelled, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Đã hủy"))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        setSecuredOnClickListener(binding.btnEditProfile, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountProfileEditFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        setSecuredOnClickListener(binding.btnRewardExchange, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountRewardFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        setSecuredOnClickListener(binding.btnReviewProducts, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Hoàn tất"))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        setSecuredOnClickListener(binding.layoutCoinsBadge, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountCheckinFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        setSecuredOnClickListener(binding.btnAccountSetup, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountProfileSetupFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        binding.btnRootieDeal.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountVoucherFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss());

        View pinParent = (View) view.findViewById(R.id.iv_pin).getParent();
        if (pinParent != null) {
            pinParent.setOnClickListener(v -> openNearestStoreDetail());
        }

        updateNearestStoreAreaHint();

        NavAppUtils.setupNavApp(this, view, R.id.nav_account);
        ProfileUpdateNotifier.addListener(profileUpdateListener);

        binding.btnSkinWeather.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.slide_out_right
                )
                .replace(R.id.main_container, new SkinWeatherForecastFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss());

        binding.btnSpaHistory.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new BookingHistoryFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss());

        binding.btnSkinReminder.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new SkinReminderFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss());

        binding.btnTreatmentHistory.setOnClickListener(v -> Toast.makeText(getContext(), "Tính năng Lịch sử liệu trình đang được phát triển", Toast.LENGTH_SHORT).show());

        binding.btnSkinProfile.setOnClickListener(v -> {
            String savedSkin = prefs.getString("SAVED_USER_SKIN_TYPE", null);
            if (savedSkin != null) {
                String recommendation = prefs.getString("SAVED_RECOMMENDATION", null);
                Set<String> flaggedGroups = prefs.getStringSet("SAVED_FLAGGED_GROUPS", null);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("SKIN_TYPE_RESULT", savedSkin);
                if (recommendation != null) editor.putString("RECOMMENDATION", recommendation);
                if (flaggedGroups != null) editor.putStringSet("FLAGGED_GROUPS", flaggedGroups);
                editor.apply();

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new SkinAllergyProfileFragment())
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), "Bạn chưa thực hiện khảo sát da. Đang chuyển hướng đến bài test...", Toast.LENGTH_LONG).show();
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new QuizTestIntroFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        if (isLoggedIn) {
            RootieDatabase db = RootieDatabase.getDatabase(requireContext());
            orderRepository = new OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), new LocalJsonReader(requireContext()));

            if (!FreshDemoAccountSeeder.isDemoAccount(
                    ProfileSessionHelper.getEffectiveUserId(ctx),
                    ProfileSession.getEmail(ctx))) {
                BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
                    orderRepository.refreshOrders(
                            ProfileSessionHelper.getEffectiveUserId(ctx),
                            ProfileSession.getPhone(ctx)
                    );
                    return Unit.INSTANCE;
                });
            } else {
                binding.tvCoins.setText("0");
                updateOrderBadges(Collections.emptyList());
            }
        } else {
            binding.tvCoins.setText("0");
        }

        binding.btnLogout.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        binding.btnLogout.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.com_dialog_logout_confirm, null);
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();

            TextView btnConfirm = dialogView.findViewById(R.id.btnConfirmLogout);
            TextView btnCancel = dialogView.findViewById(R.id.btnCancelLogout);

            btnConfirm.setOnClickListener(v1 -> {
                dialog.dismiss();
                ProfileSession.INSTANCE.setLoggedIn(requireContext(), false);
                Intent intent = new Intent(requireContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) getActivity().finish();
            });

            btnCancel.setOnClickListener(v1 -> dialog.dismiss());

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.show();
        });

        BottomNavHelper.setup(
                this,
                binding.getRoot(),
                R.id.nav_account,
                tabId -> {
                    BottomNavHelper.navigate(this, tabId);
                }
        );
    }

    private void setSecuredOnClickListener(View view, boolean isLoggedIn, View.OnClickListener guestRedirectListener, Runnable action) {
        view.setOnClickListener(v -> {
            if (isLoggedIn) {
                action.run();
            } else {
                guestRedirectListener.onClick(v);
            }
        });
    }

    @Override
    public void observeViewModel() {
        if (!ProfileSession.isLoggedIn(requireContext())) return;
        Context ctx = requireContext();
        RootieDatabase db = RootieDatabase.getDatabase(ctx);

        String userId = ProfileSessionHelper.getEffectiveUserId(ctx);
        String phone = ProfileSession.getPhone(ctx);
        boolean isFreshDemo = FreshDemoAccountSeeder.isDemoAccount(userId, ProfileSession.getEmail(ctx));

        FlowLiveDataConversions.asLiveData(db.rewardPointDao().getTotalPointsFlow(userId != null ? userId : ""))
                .observe(getViewLifecycleOwner(), points -> {
                    if (binding == null || !isAdded()) return;
                    int total = (points != null && !points.isEmpty()) ? points.get(0).total : 0;
                    binding.tvCoins.setText(String.valueOf(total));
                });

        if (orderRepository == null) {
            orderRepository = new OrderRepository(
                    db.orderDao(),
                    db.rewardPointDao(),
                    db.userGiftDao(),
                    new LocalJsonReader(ctx)
            );
        }

        // Tài khoản mới / thường: chỉ hiện đơn của chính họ trong Room (Firebase sync riêng).
        // Không seed/remap đơn mẫu từ assets.
        updateOrderBadges(Collections.emptyList());
        FlowLiveDataConversions.asLiveData(orderRepository.getOrdersForBuyer(userId, phone))
                .observe(getViewLifecycleOwner(), roomOrders -> {
                    if (binding == null || !isAdded()) return;
                    updateOrderBadges(roomOrders != null ? roomOrders : Collections.emptyList());
                });

        if (isFreshDemo) {
            return;
        }

        if (ProfileSession.isDemoTeamUser(userId)) {
            new Thread(() -> {
                orderRepository.syncOrdersFromAssetsBlocking();
            }).start();
        }
    }

    private void refreshProfileUiFromSession() {
        if (binding == null || !isAdded()) {
            return;
        }
        Context ctx = requireContext();
        if (!ProfileSession.isLoggedIn(ctx)) {
            return;
        }
        bindProfileAvatar(ctx);
        binding.tvUsername.setText(ProfileSession.getFullName(ctx));
        binding.tvEmail.setText(ProfileSession.getEmail(ctx));
    }

    private void bindProfileAvatar(Context ctx) {
        if (binding == null) {
            return;
        }
        String avatarUrl = ProfileSessionHelper.getAccountProfileAvatarUrl(ctx);
        AvatarLoader.loadAvatar(binding.ivAvatar, avatarUrl);
    }

    private void loadUserProfileData(Context ctx) {
        new Thread(() -> {
            try {
                ProfileSessionHelper.ensureCurrentUserInDatabase(ctx);

                UserEntity user = ProfileSessionHelper.findCurrentUser(ctx);
                if (user != null) {
                    user.setFull_name(ProfileSession.getFullName(ctx));
                    String username = ProfileSession.getUsername(ctx);
                    if (username != null) {
                        user.setUsername(username.replace("@", "").trim());
                    }
                    user.setEmail(ProfileSession.getEmail(ctx));
                    user.setPhone(ProfileSession.getPhone(ctx));
                    RootieDatabase.getDatabase(ctx).userDao().insertUserSync(user);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> refreshProfileUiFromSession());
                }

                if (orderRepository == null) {
                    RootieDatabase db = RootieDatabase.getDatabase(ctx);
                    orderRepository = new OrderRepository(
                            db.orderDao(),
                            db.rewardPointDao(),
                            db.userGiftDao(),
                            new LocalJsonReader(ctx)
                    );
                }
                if (ProfileSession.isDemoTeamUser(ProfileSessionHelper.getEffectiveUserId(ctx))
                        && !FreshDemoAccountSeeder.isDemoAccount(
                        ProfileSessionHelper.getEffectiveUserId(ctx),
                        ProfileSession.getEmail(ctx))) {
                    orderRepository.syncOrdersFromAssetsBlocking();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateOrderBadges(@Nullable List<OrderEntity> orders) {
        if (binding == null || !isAdded()) return;

        int pending = 0;
        int processing = 0;
        int delivering = 0;
        int success = 0;
        int cancelled = 0;

        if (orders != null) {
            for (OrderEntity order : orders) {
                String status = order.getStatus();
                if (status == null) continue;
                switch (status) {
                    case "Chờ xác nhận":
                        pending++;
                        break;
                    case "Đang xử lý":
                        processing++;
                        break;
                    case "Đang giao":
                        delivering++;
                        break;
                    case "Hoàn tất":
                        success++;
                        break;
                    case "Đã hủy":
                        cancelled++;
                        break;
                    default:
                        break;
                }
            }
        }

        setOrderBadge(binding.tvOrderBadgePending, pending);
        setOrderBadge(binding.tvOrderBadgeProcessing, processing);
        setOrderBadge(binding.tvOrderBadgeDelivering, delivering);
        setOrderBadge(binding.tvOrderBadgeSuccess, success);
        setOrderBadge(binding.tvOrderBadgeCancelled, cancelled);
    }

    private void setOrderBadge(TextView badgeView, int count) {
        if (count > 0) {
            badgeView.setVisibility(View.VISIBLE);
            badgeView.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            badgeView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding == null) {
            return;
        }
        startNearestStoreLocationUpdates();
        if (ProfileSession.isLoggedIn(requireContext())) {
            Context ctx = requireContext();
            refreshProfileUiFromSession();
            bindProfileAvatar(ctx);
            if (!ProfileSession.hasLocalProfileEdits(ctx)) {
                SyncDataHelper.syncUserProfileFromFirestore(ctx, () -> {
                    if (binding == null || !isAdded()) {
                        return;
                    }
                    bindProfileAvatar(ctx);
                    refreshProfileUiFromSession();
                });
            }
        } else {
            refreshProfileUiFromSession();
        }
    }

    @Override
    public void onPause() {
        stopNearestStoreLocationUpdates();
        super.onPause();
    }

    private void updateNearestStoreAreaHint() {
        if (binding == null || !isAdded()) {
            return;
        }
        Context ctx = requireContext();
        if (!StoreProximityHelper.hasLocationPermission(ctx)) {
            binding.tvNearestStoreHint.setText("Vui lòng cho phép định vị để biết chi nhánh gần bạn nhất");
            requestLocationPermissionIfNeeded();
            return;
        }
        if (!RootieLocationHelper.isLocationEnabled(ctx)) {
            binding.tvNearestStoreHint.setText("Vui lòng bật GPS để định vị chính xác hơn");
            return;
        }
        binding.tvNearestStoreHint.setText("Đang định vị chính xác (GPS)...");
        Location cached = RootieLocationHelper.getBestAvailableLocation(ctx);
        if (cached != null) {
            resolveNearestStoreFromLocation(cached);
        }
        RootieLocationHelper.requestAccurateLocation(ctx, this::resolveNearestStoreFromLocation);
    }

    private void requestLocationPermissionIfNeeded() {
        if (!isAdded()) {
            return;
        }
        Context ctx = requireContext();
        if (StoreProximityHelper.hasLocationPermission(ctx)) {
            return;
        }
        int finePermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarsePermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (finePermission != PackageManager.PERMISSION_GRANTED
                && coarsePermission != PackageManager.PERMISSION_GRANTED) {
            requestLocationLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startNearestStoreLocationUpdates() {
        if (binding == null || !isAdded()) {
            return;
        }
        Context ctx = requireContext();
        if (!StoreProximityHelper.hasLocationPermission(ctx)) {
            requestLocationPermissionIfNeeded();
            updateNearestStoreAreaHint();
            return;
        }
        updateNearestStoreAreaHint();
        RootieLocationHelper.startHighAccuracyUpdates(ctx, this::resolveNearestStoreFromLocation);
    }

    private void stopNearestStoreLocationUpdates() {
        if (!isAdded()) {
            return;
        }
        RootieLocationHelper.stopHighAccuracyUpdates(requireContext().getApplicationContext());
    }

    private void resolveNearestStoreFromLocation(@Nullable Location location) {
        if (binding == null || !isAdded()) {
            return;
        }
        if (location == null) {
            latestUserLocation = null;
            nearestStore = null;
            binding.tvNearestStoreHint.setText(
                    "Không xác định được vị trí. Hãy bật GPS và thử lại"
            );
            return;
        }
        latestUserLocation = location;
        Context appContext = requireContext().getApplicationContext();
        new Thread(() -> {
            StoreProximityHelper.NearestStoreResult result =
                    StoreProximityHelper.findNearestStore(appContext, location);
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(() -> {
                if (binding == null || !isAdded()) {
                    return;
                }
                if (result != null && result.store != null) {
                    nearestStore = result.store;
                    binding.tvNearestStoreHint.setText(
                            StoreProximityHelper.formatNearestAreaLabel(result, location)
                    );
                } else {
                    nearestStore = null;
                    binding.tvNearestStoreHint.setText(
                            "Không xác định được chi nhánh gần bạn. Vui lòng thử lại"
                    );
                }
            });
        }).start();
    }

    private void openNearestStoreDetail() {
        if (nearestStore == null) {
            Toast.makeText(getContext(), "Đang xác định chi nhánh gần bạn...", Toast.LENGTH_SHORT).show();
            updateNearestStoreAreaHint();
            return;
        }
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, ShopStoreDetailFragment.newInstance(nearestStore))
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    @Override
    public void onDestroyView() {
        nearestStore = null;
        latestUserLocation = null;
        stopNearestStoreLocationUpdates();
        ProfileUpdateNotifier.removeListener(profileUpdateListener);
        super.onDestroyView();
        binding = null;
    }
}
