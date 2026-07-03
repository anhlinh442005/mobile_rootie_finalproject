package com.veganbeauty.app.features.shop.product;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


import com.veganbeauty.app.R;
import com.veganbeauty.app.utils.VietQRHelper;

import java.text.NumberFormat;
import java.util.Locale;

public class ShopPaymentDialogs {

    private static final String MOCK_BANK_CODE = "VCB";
    private static final String MOCK_ACCOUNT_NUMBER = "9960123456";

    public static void showAtmVnpayQrDialog(
            Fragment fragment,
            String orderCode,
            long totalAmount,
            boolean isVnpay
    ) {
        Context ctx = fragment.requireContext();
        View view = LayoutInflater.from(ctx).inflate(R.layout.shop_dialog_atm_vnpay_qr, null, false);
        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(view)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        ImageView ivProviderLogo = view.findViewById(R.id.ivProviderLogo);
        TextView tvProviderName = view.findViewById(R.id.tvProviderName);
        ImageView ivQrCode = view.findViewById(R.id.ivQrCode);
        TextView tvAmount = view.findViewById(R.id.tvAmount);
        TextView tvOrderCode = view.findViewById(R.id.tvOrderCode);
        View btnCopy = view.findViewById(R.id.btnCopy);
        View btnClose = view.findViewById(R.id.btnClose);
        com.google.android.material.button.MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmPayment);

        if (isVnpay) {
            ivProviderLogo.setImageResource(R.drawable.ic_vnpay);
            ivProviderLogo.setImageTintList(null);
            tvProviderName.setText("Cổng thanh toán VNPay");
        } else {
            ivProviderLogo.setImageResource(R.drawable.ic_credit_card);
            ivProviderLogo.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3E4D44")));
            tvProviderName.setText("Ngân hàng TMCP Ngoại thương Việt Nam (Vietcombank)");
        }

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvAmount.setText(formatter.format(totalAmount));
        tvOrderCode.setText(orderCode);

        String qrUrl = VietQRHelper.buildImageUrl(
                MOCK_BANK_CODE,
                MOCK_ACCOUNT_NUMBER,
                totalAmount,
                orderCode,
                "qr_only"
        );
        com.bumptech.glide.Glide.with(ivQrCode.getContext()).load(qrUrl).placeholder(R.drawable.ic_qrscan).error(R.drawable.ic_qrscan).into(ivQrCode);

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Order code", orderCode));
                Toast.makeText(ctx, "Đã sao chép mã đơn hàng", Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToOrderSuccess(
                    fragment,
                    orderCode,
                    totalAmount,
                    isVnpay ? "Thanh toán trực tuyến VNPay" : "Thẻ ATM nội địa/Internet Banking"
            );
        });

        dialog.show();
    }

    public static void showMomoRedirectDialog(Fragment fragment, String orderCode, long totalAmount) {
        Context ctx = fragment.requireContext();
        View view = LayoutInflater.from(ctx).inflate(R.layout.shop_dialog_momo_redirect, null, false);
        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(view)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvOrderCode = view.findViewById(R.id.tvOrderCode);
        View btnClose = view.findViewById(R.id.btnClose);
        com.google.android.material.button.MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmPayment);

        tvOrderCode.setText("Mã đơn: " + orderCode);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setEnabled(false);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialog.isShowing()) {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Xác nhận đã thanh toán");
            }
        }, 2000L);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            navigateToOrderSuccess(
                    fragment,
                    orderCode,
                    totalAmount,
                    "Thanh toán trực tuyến MoMo"
            );
        });

        dialog.show();
    }

    private static void navigateToOrderSuccess(
            Fragment fragment,
            String orderCode,
            long totalAmount,
            String paymentMethod
    ) {
        ShopCheckoutFragment checkoutFragment = null;
        if (fragment instanceof ShopCheckoutFragment) {
            checkoutFragment = (ShopCheckoutFragment) fragment;
        }
        boolean isGuest = !com.veganbeauty.app.data.local.ProfileSession.isLoggedIn(fragment.requireContext());

        boolean isEmailNotification = isGuest && checkoutFragment != null && checkoutFragment.hasGuestEmail();
        String notificationType = isEmailNotification ? "email" : "sms";

        if (checkoutFragment != null) {
            checkoutFragment.persistOrderFromDialog(
                    orderCode,
                    totalAmount,
                    paymentMethod,
                    isGuest,
                    isEmailNotification
            );
            return;
        }

        boolean isStorePickup = checkoutFragment != null && checkoutFragment.isStorePickup();
        String storeName = checkoutFragment != null ? checkoutFragment.getSelectedStoreName() : null;
        String storeAddress = checkoutFragment != null ? checkoutFragment.getSelectedStoreAddress() : null;

        FragmentManager fm = fragment.getParentFragmentManager();
        ShopOrderSuccessFragment successFragment = ShopOrderSuccessFragment.newInstance(
                orderCode,
                totalAmount,
                paymentMethod,
                isGuest,
                notificationType,
                isStorePickup,
                storeName,
                storeAddress,
                null
        );
        try {
            fm.popBackStack();
        } catch (Exception ignored) {
        }
        fm.beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .replace(R.id.main_container, successFragment)
                .commit();
    }
}
