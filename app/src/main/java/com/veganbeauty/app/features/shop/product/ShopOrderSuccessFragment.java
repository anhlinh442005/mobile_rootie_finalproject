package com.veganbeauty.app.features.shop.product;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.databinding.ShopOrderSuccessBinding;
import com.veganbeauty.app.features.account.order.AccountOrderDetailFragment;
import com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity;
import com.veganbeauty.app.features.shop.home.ShopHomeFragment;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ShopOrderSuccessFragment extends RootieFragment {

    private ShopOrderSuccessBinding binding;

    private static final String ARG_ORDER_CODE = "arg_order_code";
    private static final String ARG_TOTAL_AMOUNT = "arg_total_amount";
    private static final String ARG_PAYMENT_METHOD = "arg_payment_method";
    private static final String ARG_IS_GUEST = "arg_is_guest";
    private static final String ARG_NOTIFICATION_TYPE = "arg_notification_type"; // "email" or "sms"
    private static final String ARG_IS_STORE_PICKUP = "arg_is_store_pickup";
    private static final String ARG_STORE_NAME = "arg_store_name";
    private static final String ARG_STORE_ADDRESS = "arg_store_address";
    private static final String ARG_RECIPIENT_NAME = "arg_recipient_name";

    public static ShopOrderSuccessFragment newInstance(
            String orderCode, long totalAmount, String paymentMethod, boolean isGuest,
            String notificationType, boolean isStorePickup, String storeName,
            String storeAddress, String recipientName) {

        ShopOrderSuccessFragment fragment = new ShopOrderSuccessFragment();
        Bundle args = new Bundle();
        if (orderCode != null && !orderCode.isEmpty()) args.putString(ARG_ORDER_CODE, orderCode);
        args.putLong(ARG_TOTAL_AMOUNT, totalAmount);
        if (paymentMethod != null && !paymentMethod.isEmpty()) args.putString(ARG_PAYMENT_METHOD, paymentMethod);
        args.putBoolean(ARG_IS_GUEST, isGuest);
        args.putString(ARG_NOTIFICATION_TYPE, notificationType);
        args.putBoolean(ARG_IS_STORE_PICKUP, isStorePickup);
        if (storeName != null && !storeName.isEmpty()) args.putString(ARG_STORE_NAME, storeName);
        if (storeAddress != null && !storeAddress.isEmpty()) args.putString(ARG_STORE_ADDRESS, storeAddress);
        if (recipientName != null && !recipientName.isEmpty()) args.putString(ARG_RECIPIENT_NAME, recipientName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopOrderSuccessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        String orderCode = (getArguments() != null && getArguments().getString(ARG_ORDER_CODE) != null) ? getArguments().getString(ARG_ORDER_CODE) : generateMockOrderCode();
        long totalAmount = (getArguments() != null) ? getArguments().getLong(ARG_TOTAL_AMOUNT, 0L) : 0L;
        String paymentMethod = (getArguments() != null && getArguments().getString(ARG_PAYMENT_METHOD) != null) ? getArguments().getString(ARG_PAYMENT_METHOD) : "Thanh toán tiền mặt khi nhận hàng";
        boolean isGuest = (getArguments() != null) ? getArguments().getBoolean(ARG_IS_GUEST, false) : false;
        String notificationType = (getArguments() != null && getArguments().getString(ARG_NOTIFICATION_TYPE) != null) ? getArguments().getString(ARG_NOTIFICATION_TYPE) : "sms";
        boolean isStorePickup = (getArguments() != null) ? getArguments().getBoolean(ARG_IS_STORE_PICKUP, false) : false;
        String storeName = (getArguments() != null) ? getArguments().getString(ARG_STORE_NAME) : null;

        binding.tvOrderCode.setText("#" + orderCode);
        binding.tvTotalAmount.setText(formatCurrency(totalAmount));
        binding.tvPaymentMethod.setText(paymentMethod);

        if (isStorePickup) {
            binding.tvHeaderTitle.setText("Đăng ký nhận tại cửa hàng");
            binding.tvSuccessTitle.setText("Đăng ký nhận tại cửa hàng thành công!");

            String baseMsg = "Cảm ơn bạn đã mua sắm tại Rootie. Đơn hàng của bạn đang được chuẩn bị. Vui lòng đến nhận hàng tại cửa hàng đã chọn khi nhận được thông báo.";
            if (isGuest) {
                String notificationMsg = "email".equals(notificationType) ? "Xác nhận đăng ký nhận tại cửa hàng đã được gửi qua email của bạn." : "Xác nhận đăng ký nhận tại cửa hàng đã được gửi qua SMS đến số điện thoại của bạn.";
                binding.tvSuccessSubtitle.setText(baseMsg + " " + notificationMsg);
            } else {
                binding.tvSuccessSubtitle.setText(baseMsg);
            }

            binding.tvEstimatedDeliveryLabel.setText("Cửa hàng nhận");
            binding.tvEstimatedDelivery.setText(storeName != null ? storeName : "Cửa hàng Rootie");
            binding.tvTimelineStep3.setText("Sẵn sàng nhận hàng");
        } else {
            binding.tvHeaderTitle.setText("Đặt hàng thành công");
            binding.tvSuccessTitle.setText("Đặt hàng thành công!");

            String baseMsg = "Cảm ơn bạn đã mua sắm tại Rootie. Đơn hàng của bạn đang được xử lý.";
            if (isGuest) {
                String notificationMsg = "email".equals(notificationType) ? "Xác nhận đặt hàng đã được gửi qua email của bạn." : "Xác nhận đặt hàng đã được gửi qua SMS đến số điện thoại của bạn.";
                binding.tvSuccessSubtitle.setText(baseMsg + " " + notificationMsg);
            } else {
                binding.tvSuccessSubtitle.setText(baseMsg);
            }

            binding.tvEstimatedDeliveryLabel.setText("Dự kiến giao hàng");
            binding.tvEstimatedDelivery.setText(calculateEstimatedDelivery());
            binding.tvTimelineStep3.setText("Đang giao hàng");
        }

        if (isGuest) {
            binding.llGuestOnboarding.setVisibility(View.VISIBLE);
            binding.btnGuestCreateAccount.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), HomeWelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
            binding.tvGuestSkip.setOnClickListener(v -> binding.llGuestOnboarding.setVisibility(View.GONE));
        } else {
            binding.llGuestOnboarding.setVisibility(View.GONE);
        }

        if (isGuest) {
            binding.btnViewOrderStatus.setVisibility(View.GONE);
            binding.btnBackToShop.setText("Quay về trang chủ");
            if (binding.btnBackToShop.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) binding.btnBackToShop.getLayoutParams();
                params.topMargin = 0;
                binding.btnBackToShop.setLayoutParams(params);
            }
        } else {
            binding.btnViewOrderStatus.setVisibility(View.VISIBLE);
            binding.btnBackToShop.setText("Quay về cửa hàng");
        }

        binding.btnViewOrderStatus.setOnClickListener(v -> navigateToOrderStatus());
        binding.btnBackToShop.setOnClickListener(v -> navigateToShopHome());

        if (isGuest) {
            String recipientName = getArguments() != null ? getArguments().getString(ARG_RECIPIENT_NAME) : null;
            String greeting = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName + " ơi, " : "";
            String titleText, messageText;

            if ("email".equals(notificationType)) {
                if (isStorePickup) {
                    titleText = "Xác nhận nhận tại cửa hàng qua Email";
                    messageText = greeting + "chúng tôi đã nhận được đơn hàng #" + orderCode + ". Xác nhận nhận tại cửa hàng đã được gửi qua email của bạn.";
                } else {
                    titleText = "Xác nhận đặt hàng qua Email";
                    messageText = greeting + "chúng tôi đã nhận được đơn hàng #" + orderCode + ". Xác nhận đã được gửi qua email của bạn.";
                }
            } else {
                if (isStorePickup) {
                    titleText = "Xác nhận nhận tại cửa hàng qua SMS";
                    messageText = greeting + "cảm ơn bạn đã đặt hàng tại Rootie. Mã đơn hàng: #" + orderCode + ". Vui lòng đến cửa hàng đã chọn để nhận hàng.";
                } else {
                    titleText = "Xác nhận đặt hàng qua SMS";
                    messageText = greeting + "cảm ơn bạn đã đặt hàng tại Rootie. Mã đơn hàng: #" + orderCode + ". Đơn hàng đang được chuẩn bị.";
                }
            }

            binding.tvNotiTitle.setText(titleText);
            binding.tvNotiMessage.setText(messageText);

            binding.cvNotificationBanner.setVisibility(View.VISIBLE);
            binding.cvNotificationBanner.setTranslationY(-300f);

            binding.cvNotificationBanner.animate()
                    .translationY(0f)
                    .setDuration(1200)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (binding != null) {
                            binding.cvNotificationBanner.animate()
                                    .translationY(-300f)
                                    .setDuration(800)
                                    .withEndAction(() -> binding.cvNotificationBanner.setVisibility(View.GONE))
                                    .start();
                        }
                    }, 2500)).start();
        }
    }

    @Override
    public void observeViewModel() {}

    private void navigateToOrderStatus() {
        String orderCode = (getArguments() != null && getArguments().getString(ARG_ORDER_CODE) != null) ? getArguments().getString(ARG_ORDER_CODE) : "";
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, AccountOrderDetailFragment.Companion.newInstance(orderCode, true))
                .addToBackStack(null)
                .commit();
    }

    private void navigateToShopHome() {
        getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new ShopHomeFragment())
                .commit();
    }

    private String generateMockOrderCode() {
        SimpleDateFormat format = new SimpleDateFormat("HHmmss", new Locale("vi", "VN"));
        return "ORD-1" + format.format(new Date());
    }

    private String calculateEstimatedDelivery() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 2);
        Date startDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 2);
        Date endDate = calendar.getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
        return dateFormat.format(startDate) + " - " + dateFormat.format(endDate);
    }

    private String formatCurrency(long amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
