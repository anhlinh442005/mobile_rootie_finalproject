package com.veganbeauty.app.features.account.notification;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.data.repository.NotificationRepository;
import com.veganbeauty.app.databinding.AccountNotificationFragmentBinding;
import com.veganbeauty.app.features.account.checkin.AccountCheckinFragment;
import com.veganbeauty.app.features.account.expiry.AccountProductExpiryFragment;
import com.veganbeauty.app.features.account.order.AccountOrderDetailFragment;
import com.veganbeauty.app.features.myskin.AccountSyncHelper;
import com.veganbeauty.app.features.myskin.BookingDetailCancelledFragment;
import com.veganbeauty.app.features.myskin.BookingDetailCompletedFragment;
import com.veganbeauty.app.features.myskin.BookingDetailUpcomingFragment;
import com.veganbeauty.app.features.profile.AccountVoucherDetailFragment;
import com.veganbeauty.app.features.profile.VoucherListAdapter.VoucherItem;
import com.veganbeauty.app.features.routine.SkinReminderFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlinx.coroutines.flow.FlowCollector;

public class AccountNotificationFragment extends RootieFragment {

    private AccountNotificationFragmentBinding _binding;
    private NotificationViewModel viewModel;
    private NotificationListAdapter listAdapter;

    @Override
    protected boolean shouldSetupNotificationBell() {
        return false;
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Toast.makeText(requireContext(), "Đã cấp quyền nhận thông báo", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Bạn chưa cấp quyền nhận thông báo", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private AccountNotificationFragmentBinding getBinding() {
        return _binding;
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = AccountNotificationFragmentBinding.inflate(inflater, container, false);
        
        listAdapter = new NotificationListAdapter(
                item -> {
                    viewModel.markAsRead(item.getId());
                    handleNotificationNavigation(item);
                },
                item -> {
                    viewModel.markAsRead(item.getId());
                    if ("COPY MÃ".equals(item.getActionText()) && item.getVoucherCode() != null && !item.getVoucherCode().isEmpty()) {
                        if (getContext() != null) {
                            copyToClipboard(getContext(), item.getVoucherCode());
                            Toast.makeText(getContext(), "Đã sao chép mã voucher " + item.getVoucherCode() + " thành công!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    handleNotificationNavigation(item);
                },
                item -> {
                    viewModel.markAsRead(item.getId());
                },
                item -> {
                    viewModel.deleteNotification(item.getId());
                }
        );

        setupViewModel();
        return getBinding().getRoot();
    }

    private void setupViewModel() {
        NotificationRepository repository = NotificationRepository.getInstance(requireContext());
        viewModel = new ViewModelProvider(this, new NotificationViewModelFactory(repository)).get(NotificationViewModel.class);
    }

    @Override
    public void setupUI(@NonNull View view) {
        checkNotificationPermission();
        
        getBinding().btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        getBinding().rvNotifications.setAdapter(listAdapter);

        getBinding().tabAll.setOnClickListener(v -> viewModel.selectTab("Tất cả"));
        getBinding().tabPromo.setOnClickListener(v -> viewModel.selectTab("Khuyến mãi"));
        getBinding().tabOrder.setOnClickListener(v -> viewModel.selectTab("Đơn hàng"));
        getBinding().tabOther.setOnClickListener(v -> viewModel.selectTab("Khác"));

        getBinding().etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s != null ? s.toString() : "");
            }
        });

        getBinding().btnMarkAllRead.setOnClickListener(v -> {
            viewModel.markAllAsRead();
            Toast.makeText(getContext(), "Đã đánh dấu đọc tất cả", Toast.LENGTH_SHORT).show();
        });

        getBinding().btnDeleteAll.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Xoá tất cả thông báo")
                    .setMessage("Bạn có chắc chắn muốn xoá toàn bộ thông báo không?")
                    .setPositiveButton("Xoá", (dialog, which) -> {
                        viewModel.deleteAllNotifications();
                        Toast.makeText(getContext(), "Đã xoá toàn bộ thông báo", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    @Override
    public void observeViewModel() {
        viewModel.notificationItems.observe(getViewLifecycleOwner(), items -> {
            listAdapter.submitList(items);
            boolean isEmpty = items.isEmpty();
            getBinding().layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            getBinding().rvNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            getBinding().layoutHeaderActions.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        viewModel.selectedTab.observe(getViewLifecycleOwner(), this::updateTabStyles);
    }

    private void updateTabStyles(String activeTab) {
        Map<String, TextView> tabs = new HashMap<>();
        tabs.put("Tất cả", getBinding().tabAll);
        tabs.put("Khuyến mãi", getBinding().tabPromo);
        tabs.put("Đơn hàng", getBinding().tabOrder);
        tabs.put("Khác", getBinding().tabOther);

        for (Map.Entry<String, TextView> entry : tabs.entrySet()) {
            TextView textView = entry.getValue();
            if (entry.getKey().equalsIgnoreCase(activeTab)) {
                textView.setBackgroundResource(R.drawable.bg_btn_buy);
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            } else {
                textView.setBackgroundResource(R.drawable.tab_inactive_bg);
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
            }
        }
    }

    private void handleNotificationNavigation(NotificationItem item) {
        String type = (item.getNotificationType() != null ? item.getNotificationType() : "").toLowerCase();
        String category = (item.getCategory() != null ? item.getCategory() : "").toLowerCase();
        String title = (item.getTitle() != null ? item.getTitle() : "").toLowerCase();
        String content = (item.getContent() != null ? item.getContent() : "").toLowerCase();

        if (type.equals("voucher") || category.contains("khuyến mãi") || title.contains("voucher") || content.contains("voucher")) {
            String code = item.getVoucherCode() != null ? item.getVoucherCode() : "RT50KDEC";
            VoucherItem voucher = NotificationIntentHandler.findVoucherByCode(requireContext(), code);
            if (voucher == null) {
                voucher = new VoucherItem(
                        item.getId(),
                        item.getTitle(),
                        item.getContent(),
                        code,
                        "valid",
                        "2026-12-31 23:59:59",
                        "discount",
                        false,
                        1,
                        300000,
                        "Tất cả sản phẩm",
                        "fixed_amount",
                        50000
                );
            }
            com.veganbeauty.app.features.profile.VoucherListAdapter.VoucherItem mappedVoucher = new com.veganbeauty.app.features.profile.VoucherListAdapter.VoucherItem(
                    voucher.getId(), voucher.getTitle(), voucher.getDescription(), voucher.getCode(),
                    voucher.getStatus(), voucher.getHsd(), voucher.getType(), voucher.isFromGift(),
                    voucher.getQuantity(), voucher.getMinOrderValue(), voucher.getApplicableProducts(),
                    voucher.getOfferType(), voucher.getDiscountValue()
            );
            AccountVoucherDetailFragment fragment = AccountVoucherDetailFragment.newInstance(mappedVoucher);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();

        } else if (type.equals("order") || category.contains("đơn hàng") || title.contains("đơn hàng") || content.contains("đơn hàng")) {
            String orderId = item.getOrderId() != null ? item.getOrderId() : "RT8829";
            AccountOrderDetailFragment fragment = AccountOrderDetailFragment.newInstance(orderId);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();

        } else if (type.equals("checkin") || title.contains("điểm danh") || content.contains("điểm danh")) {
            AccountCheckinFragment fragment = new AccountCheckinFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();

        } else if (type.equals("product expired") || title.contains("hết hạn") || content.contains("hết hạn")) {
            AccountProductExpiryFragment fragment = new AccountProductExpiryFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();

        } else if (type.equals("schedule date") || title.contains("lịch hẹn") || content.contains("lịch hẹn")) {
            String scheduleId = item.getScheduleId() != null ? item.getScheduleId() : "BK_NOTI_101";
            String userEmail = ProfileSession.getEmail(requireContext());
            List<BookingHistoryEntity> bookings = new LocalJsonReader(requireContext()).getUserBookingHistory(userEmail);
            
            BookingHistoryEntity realBooking = null;
            for (BookingHistoryEntity b : bookings) {
                if (b.getId().equals(scheduleId)) {
                    realBooking = b;
                    break;
                }
            }

            RootieFragment fragment;
            if (realBooking != null) {
                switch (realBooking.getStatus()) {
                    case "Đã hoàn thành":
                        fragment = BookingDetailCompletedFragment.newInstance(realBooking);
                        break;
                    case "Đã huỷ":
                        fragment = BookingDetailCancelledFragment.newInstance(realBooking);
                        break;
                    default:
                        fragment = BookingDetailUpcomingFragment.newInstance(realBooking);
                        break;
                }
            } else {
                BookingHistoryEntity mockBooking = new BookingHistoryEntity(
                        scheduleId, "user_1", "Nguyễn Khánh Xuân", "0901234567", userEmail,
                        "Chăm sóc da chuyên sâu Acne Free", "15 Tháng 6, 2026", "Thứ Hai",
                        "14:00 - 15:30", "90 phút", "Rootie Quận 1", "123 Nguyễn Thị Minh Khai, Quận 1, TP. HCM",
                        "Sắp diễn ra"
                );
                fragment = BookingDetailUpcomingFragment.newInstance(mockBooking);
            }
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();

        } else if (type.equals("skin care") || title.contains("dưỡng da") || title.contains("chăm sóc da") || content.contains("dưỡng da") || content.contains("chăm sóc da")) {
            SkinReminderFragment fragment = new SkinReminderFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Rootie Voucher Code", text);
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        AccountSyncHelper.sync(requireContext(), () -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        NotificationRepository.getInstance(requireContext()).refreshNotifications()
                );
            }
        });
    }
}
