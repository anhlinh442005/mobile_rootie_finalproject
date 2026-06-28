package com.veganbeauty.app.features.account.order;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.repository.NotificationRepository;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountOrderTrackingFragmentBinding;
import com.veganbeauty.app.databinding.AccountOrderTrackingStepItemBinding;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.lifecycle.FlowLiveDataConversions;

public class AccountOrderTrackingFragment extends RootieFragment {

    private static final String ARG_ORDER_ID = "arg_order_id";

    private AccountOrderTrackingFragmentBinding _binding;
    private OrderDetailViewModel viewModel;

    public static AccountOrderTrackingFragment newInstance(String orderId) {
        AccountOrderTrackingFragment fragment = new AccountOrderTrackingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_ID, orderId);
        fragment.setArguments(args);
        return fragment;
    }

    private AccountOrderTrackingFragmentBinding getBinding() {
        return _binding;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = AccountOrderTrackingFragmentBinding.inflate(inflater, container, false);
        setupViewModel();
        return getBinding().getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        OrderRepository repository = new OrderRepository(
                db.orderDao(), db.rewardPointDao(), db.userGiftDao(), new LocalJsonReader(requireContext())
        );
        String orderId = getArguments() != null ? getArguments().getString(ARG_ORDER_ID, "") : "";

        viewModel = new ViewModelProvider(this, new OrderDetailViewModelFactory(repository, orderId))
                .get(OrderDetailViewModel.class);
    }

    @Override
    public void setupUI(@NonNull View view) {
        getBinding().btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        View.OnClickListener navigateToNotification = v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        };
        getBinding().layoutNotification.setOnClickListener(navigateToNotification);
        getBinding().btnNotification.setOnClickListener(navigateToNotification);

        getBinding().btnCallShipper.setOnClickListener(v ->
                Toast.makeText(getContext(), "Đang kết nối cuộc gọi đến shipper...", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void observeViewModel() {
        viewModel.order.observe(getViewLifecycleOwner(), order -> {
            if (order != null) {
                bindOrderTracking(order);
            }
        });

        FlowLiveDataConversions.asLiveData(
                NotificationRepository.getInstance(requireContext()).getUnreadCount()
        ).observe(getViewLifecycleOwner(), count -> {
            if (_binding == null) return;
            _binding.viewNotificationBadge.setVisibility(count != null && count > 0 ? View.VISIBLE : View.GONE);
        });
    }

    private void bindOrderTracking(OrderEntity order) {
        if (getContext() == null) return;

        getBinding().tvExpectedTime.setText(order.getExpectedDeliveryTime() != null ? order.getExpectedDeliveryTime() : "Hôm nay, 18:00 - 20:00");
        getBinding().tvOrderCode.setText("Mã đơn: " + order.getId());

        int badgeBgRes = R.color.status_pending_bg;
        int badgeTextRes = R.color.status_pending_text;

        switch (order.getStatus()) {
            case "Chờ xử lý":
                badgeBgRes = R.color.status_pending_bg;
                badgeTextRes = R.color.status_pending_text;
                break;
            case "Đang xử lý":
                badgeBgRes = R.color.status_processing_bg;
                badgeTextRes = R.color.status_processing_text;
                break;
            case "Đang giao":
                badgeBgRes = R.color.status_delivering_bg;
                badgeTextRes = R.color.status_delivering_text;
                break;
            case "Hoàn tất":
                badgeBgRes = R.color.status_success_bg;
                badgeTextRes = R.color.status_success_text;
                break;
            case "Đã hủy":
                badgeBgRes = R.color.status_cancelled_bg;
                badgeTextRes = R.color.status_cancelled_text;
                break;
        }

        getBinding().tvStatusBadge.getBackground().mutate().setTint(ContextCompat.getColor(getContext(), badgeBgRes));
        getBinding().tvStatusBadge.setTextColor(ContextCompat.getColor(getContext(), badgeTextRes));
        getBinding().tvStatusBadge.setText(order.getStatus().toUpperCase());

        getBinding().tvShipperName.setText("Nhân viên giao hàng");
        if (order.getPaymentMethod().contains("MoMo")) {
            getBinding().tvDeliveryService.setText("Giao hàng tiết kiệm (GHTK)");
        } else {
            getBinding().tvDeliveryService.setText("Giao hàng nhanh (GHN)");
        }

        buildJourneyTimeline(order);
    }

    private void buildJourneyTimeline(OrderEntity order) {
        if (getContext() == null) return;
        getBinding().layoutTimelineContainer.removeAllViews();

        Calendar orderCal = parseDateTime(order.getOrderDate(), order.getOrderTime());
        List<TrackingStep> steps = new ArrayList<>();

        if ("Đã hủy".equals(order.getStatus())) {
            Calendar calCancel = (Calendar) orderCal.clone();
            calCancel.add(Calendar.HOUR_OF_DAY, 1);
            steps.add(new TrackingStep("Đơn hàng đã hủy", "Đơn hàng của bạn đã bị hủy bỏ.", formatDateTime(calCancel), true, true));
            steps.add(new TrackingStep("Đặt hàng thành công", "Đơn hàng đã được ghi nhận trên hệ thống.", formatDateTime(orderCal), true, false));
        } else {
            Calendar cal1 = orderCal;
            Calendar cal2 = (Calendar) orderCal.clone(); cal2.add(Calendar.HOUR_OF_DAY, 2);
            Calendar cal3 = (Calendar) orderCal.clone(); cal3.add(Calendar.HOUR_OF_DAY, 6);
            Calendar cal4 = (Calendar) orderCal.clone(); cal4.add(Calendar.DAY_OF_YEAR, 1); cal4.add(Calendar.HOUR_OF_DAY, 2);
            Calendar cal5 = (Calendar) orderCal.clone(); cal5.add(Calendar.DAY_OF_YEAR, 1); cal5.add(Calendar.HOUR_OF_DAY, 8);

            String status = order.getStatus();

            steps.add(new TrackingStep("Giao hàng thành công", "Đơn hàng đã được giao thành công đến người nhận.", formatDateTime(cal5), "Hoàn tất".equals(status), "Hoàn tất".equals(status)));
            steps.add(new TrackingStep("Đang giao đến bạn", "Shipper đang trên đường giao hàng đến địa chỉ của bạn.", formatDateTime(cal4), "Hoàn tất".equals(status) || "Đang giao".equals(status), "Đang giao".equals(status)));
            steps.add(new TrackingStep("Đơn hàng đã giao cho đơn vị vận chuyển", "Đơn vị vận chuyển đã tiếp nhận đơn hàng và đang vận chuyển.", formatDateTime(cal3), "Hoàn tất".equals(status) || "Đang giao".equals(status), false));
            steps.add(new TrackingStep("Đơn hàng đã được đóng gói", "Nhân viên đã đóng gói sản phẩm hoàn tất.", formatDateTime(cal2), "Hoàn tất".equals(status) || "Đang giao".equals(status) || "Đang xử lý".equals(status), "Đang xử lý".equals(status)));
            steps.add(new TrackingStep("Đặt hàng thành công", "Đơn hàng của bạn đã được ghi nhận trên hệ thống.", formatDateTime(cal1), true, "Chờ xử lý".equals(status)));
        }

        for (int i = 0; i < steps.size(); i++) {
            TrackingStep step = steps.get(i);
            AccountOrderTrackingStepItemBinding stepBinding = AccountOrderTrackingStepItemBinding.inflate(
                    LayoutInflater.from(getContext()), getBinding().layoutTimelineContainer, false
            );

            stepBinding.tvStepTitle.setText(step.title);
            stepBinding.tvStepDesc.setText(step.description);

            if (step.isCompleted) {
                stepBinding.tvStepDateTime.setVisibility(View.VISIBLE);
                stepBinding.tvStepDateTime.setText(step.dateTimeStr);
                
                stepBinding.tvStepTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.primary));
                stepBinding.tvStepDesc.setTextColor(ContextCompat.getColor(getContext(), R.color.primary));
                stepBinding.tvStepDateTime.setTextColor(ContextCompat.getColor(getContext(), R.color.gray_dark));

                stepBinding.viewDotInner.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.primary)));
            } else {
                stepBinding.tvStepDateTime.setVisibility(View.GONE);
                
                stepBinding.tvStepTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.gray_dark));
                stepBinding.tvStepDesc.setTextColor(ContextCompat.getColor(getContext(), R.color.gray_dark));

                stepBinding.viewDotInner.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.gray_light)));
            }

            if (step.isActive) {
                stepBinding.viewDotOuter.setVisibility(View.VISIBLE);
                stepBinding.viewDotOuter.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.primary)));
                stepBinding.tvStepTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.primary));
                stepBinding.tvStepTitle.setTextSize(15f);
            } else {
                stepBinding.viewDotOuter.setVisibility(View.GONE);
            }

            if (i == 0) {
                stepBinding.viewLineTop.setVisibility(View.GONE);
            } else {
                stepBinding.viewLineTop.setVisibility(View.VISIBLE);
                TrackingStep prevStep = steps.get(i - 1);
                if (prevStep.isCompleted) {
                    stepBinding.viewLineTop.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.primary)));
                } else {
                    stepBinding.viewLineTop.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.gray_light)));
                }
            }

            if (i == steps.size() - 1) {
                stepBinding.viewLineBottom.setVisibility(View.GONE);
            } else {
                stepBinding.viewLineBottom.setVisibility(View.VISIBLE);
                if (step.isCompleted) {
                    stepBinding.viewLineBottom.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.primary)));
                } else {
                    stepBinding.viewLineBottom.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.gray_light)));
                }
            }

            getBinding().layoutTimelineContainer.addView(stepBinding.getRoot());
        }
    }

    private Calendar parseDateTime(String dateStr, String timeStr) {
        Calendar cal = Calendar.getInstance();
        try {
            if (dateStr.contains("/") || dateStr.contains("-")) {
                String separator = dateStr.contains("/") ? "/" : "-";
                String[] parts = dateStr.split(separator);
                if (parts.length == 3) {
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    String[] timeParts = timeStr.split(":");
                    int hour = timeParts.length > 0 ? Integer.parseInt(timeParts[0]) : 0;
                    int minute = timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0;
                    cal.set(year, month - 1, day, hour, minute, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal;
                }
            }
            if (dateStr.contains(" Thg ")) {
                String dateClean = dateStr.replace(",", "").trim();
                String[] parts = dateClean.split(" ");
                if (parts.length >= 4) {
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[2]);
                    int year = Integer.parseInt(parts[3]);
                    String[] timeParts = timeStr.split(":");
                    int hour = timeParts.length > 0 ? Integer.parseInt(timeParts[0]) : 0;
                    int minute = timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0;
                    cal.set(year, month - 1, day, hour, minute, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cal;
    }

    private String formatDateTime(Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        return String.format(java.util.Locale.getDefault(), "%02d:%02d, %02d/%02d/%d", hour, minute, day, month, year);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }

    public static class TrackingStep {
        public String title;
        public String description;
        public String dateTimeStr;
        public boolean isCompleted;
        public boolean isActive;

        public TrackingStep(String title, String description, String dateTimeStr, boolean isCompleted, boolean isActive) {
            this.title = title;
            this.description = description;
            this.dateTimeStr = dateTimeStr;
            this.isCompleted = isCompleted;
            this.isActive = isActive;
        }
    }
}
