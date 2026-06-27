package com.veganbeauty.app.features.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.MainActivity;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountProfileBinding;
import com.veganbeauty.app.features.account.checkin.AccountCheckinFragment;
import com.veganbeauty.app.features.account.expiry.AccountProductExpiryFragment;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;
import com.veganbeauty.app.features.account.order.AccountOrderListFragment;
import com.veganbeauty.app.features.account.reward.AccountRewardFragment;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity;
import com.veganbeauty.app.features.myskin.BookingHistoryFragment;
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment;
import com.veganbeauty.app.features.routine.SkinReminderFragment;
import com.veganbeauty.app.features.weather.SkinWeatherForecastFragment;
import com.veganbeauty.app.utils.AvatarLoader;
import com.veganbeauty.app.utils.NavAppUtils;
import com.veganbeauty.app.utils.SyncDataHelper;

import java.util.Set;

import kotlin.Unit;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class AccountProfileFragment extends RootieFragment {

    private AccountProfileBinding binding;

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

        String avatarUrl = ProfileSession.INSTANCE.getAvatar(ctx);
        AvatarLoader.INSTANCE.loadAvatar(binding.ivAvatar, avatarUrl);

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

        binding.llSearch.setOnClickListener(v -> Toast.makeText(getContext(), "Tính năng tìm kiếm đang phát triển", Toast.LENGTH_SHORT).show());
        binding.btnQrScan.setOnClickListener(v -> Toast.makeText(getContext(), "Mở trình quét mã QR", Toast.LENGTH_SHORT).show());

        binding.btnNotification.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountNotificationFragment())
                .addToBackStack(null)
                .commit());

        binding.btnExpiryShelf.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountProductExpiryFragment())
                .addToBackStack(null)
                .commit());

        setSecuredOnClickListener(binding.btnAllOrders, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Tất cả"))
                    .addToBackStack(null)
                    .commit();
        });

        setSecuredOnClickListener(binding.btnStatusPending, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Chờ xử lý"))
                    .addToBackStack(null)
                    .commit();
        });

        setSecuredOnClickListener(binding.btnStatusProcessing, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Đang xử lý"))
                    .addToBackStack(null)
                    .commit();
        });

        setSecuredOnClickListener(binding.btnStatusDelivering, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Đang giao"))
                    .addToBackStack(null)
                    .commit();
        });

        setSecuredOnClickListener(binding.btnStatusSuccess, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Hoàn tất"))
                    .addToBackStack(null)
                    .commit();
        });

        setSecuredOnClickListener(binding.btnStatusCancelled, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Đã hủy"))
                    .addToBackStack(null)
                    .commit();
        });

        setSecuredOnClickListener(binding.btnEditProfile, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountProfileEditFragment())
                    .addToBackStack(null)
                    .commit();
        });

        setSecuredOnClickListener(binding.btnRewardExchange, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountRewardFragment())
                    .addToBackStack(null)
                    .commit();
        });

        setSecuredOnClickListener(binding.btnReviewProducts, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, AccountOrderListFragment.newInstance("Hoàn tất"))
                    .addToBackStack(null)
                    .commit();
        });

        setSecuredOnClickListener(binding.layoutCoinsBadge, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountCheckinFragment())
                    .addToBackStack(null)
                    .commit();
        });

        setSecuredOnClickListener(binding.btnAccountSetup, isLoggedIn, guestRedirectListener, () -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountProfileSetupFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnRootieDeal.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountVoucherFragment())
                .addToBackStack(null)
                .commit());

        View pinParent = (View) view.findViewById(R.id.iv_pin).getParent();
        if (pinParent != null) {
            pinParent.setOnClickListener(v -> Toast.makeText(getContext(), "Chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show());
        }

        NavAppUtils.INSTANCE.setupNavApp(this, view, R.id.nav_account);

        binding.btnSkinWeather.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new SkinWeatherForecastFragment())
                .addToBackStack(null)
                .commit());

        binding.btnSpaHistory.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new BookingHistoryFragment())
                .addToBackStack(null)
                .commit());

        binding.btnSkinReminder.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new SkinReminderFragment())
                .addToBackStack(null)
                .commit());

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
            RootieDatabase db = RootieDatabase.Companion.getDatabase(requireContext());
            OrderRepository repository = new OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), new LocalJsonReader(requireContext()));
            
            BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
                repository.refreshOrders(continuation);
                return Unit.INSTANCE;
            });
            
            BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getMain(), kotlinx.coroutines.CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
                kotlinx.coroutines.flow.FlowKt.collect(db.rewardPointDao().getTotalPointsFlow(), new kotlinx.coroutines.flow.FlowCollector<Integer>() {
                    @Nullable
                    @Override
                    public Object emit(Integer points, @NonNull kotlin.coroutines.Continuation<? super Unit> continuation) {
                        binding.tvCoins.setText(String.valueOf(points != null ? points : 0));
                        return Unit.INSTANCE;
                    }
                }, continuation);
                return Unit.INSTANCE;
            });
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

        BottomNavHelper.INSTANCE.setup(
                this,
                binding.getRoot(),
                R.id.nav_account,
                tabId -> {
                    BottomNavHelper.INSTANCE.navigate(this, tabId);
                    return Unit.INSTANCE;
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
        super.observeViewModel();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            Context ctx = requireContext();
            boolean isLoggedIn = ProfileSession.INSTANCE.isLoggedIn(ctx);
            String avatarUrl = ProfileSession.INSTANCE.getAvatar(ctx);
            AvatarLoader.INSTANCE.loadAvatar(binding.ivAvatar, avatarUrl);
            if (isLoggedIn) {
                String fullName = ProfileSession.INSTANCE.getFullName(ctx);
                String email = ProfileSession.INSTANCE.getEmail(ctx);
                binding.tvUsername.setText(fullName);
                binding.tvEmail.setText(email);

                SyncDataHelper.INSTANCE.syncUserProfileFromFirestore(ctx, () -> {
                    if (binding != null && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String updatedAvatarUrl = ProfileSession.INSTANCE.getAvatar(ctx);
                            AvatarLoader.INSTANCE.loadAvatar(binding.ivAvatar, updatedAvatarUrl);
                            binding.tvUsername.setText(ProfileSession.INSTANCE.getFullName(ctx));
                            binding.tvEmail.setText(ProfileSession.INSTANCE.getEmail(ctx));
                        });
                    }
                    return Unit.INSTANCE;
                });
            }
        }
    }
}
