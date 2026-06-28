package com.veganbeauty.app.features.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwnerKt;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountVoucherDetailFragmentBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.profile.VoucherListAdapter.VoucherItem;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import kotlin.Unit;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class AccountVoucherDetailFragment extends RootieFragment {

    private AccountVoucherDetailFragmentBinding binding;

    private OrderRepository repository;

    private String voucherId = "";
    private String voucherTitle = "";
    private String voucherDescription = "";
    private String voucherCode = "";
    private String voucherStatus = "";
    private String voucherHsd = "";
    private String voucherType = "";
    private boolean fromGift = false;
    private int minOrderValue = 0;
    private String applicableProducts = "Tất cả sản phẩm";
    private String offerType = "fixed_amount";
    private int discountValue = 0;

    private static final String ARG_ID = "arg_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESCRIPTION = "arg_description";
    private static final String ARG_CODE = "arg_code";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_HSD = "arg_hsd";
    private static final String ARG_TYPE = "arg_type";
    private static final String ARG_FROM_GIFT = "arg_from_gift";
    private static final String ARG_MIN_ORDER_VALUE = "arg_min_order_value";
    private static final String ARG_APPLICABLE_PRODUCTS = "arg_applicable_products";
    private static final String ARG_OFFER_TYPE = "arg_offer_type";
    private static final String ARG_DISCOUNT_VALUE = "arg_discount_value";

    public static AccountVoucherDetailFragment newInstance(VoucherItem item) {
        AccountVoucherDetailFragment fragment = new AccountVoucherDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, item.getId());
        args.putString(ARG_TITLE, item.getTitle());
        args.putString(ARG_DESCRIPTION, item.getDescription());
        args.putString(ARG_CODE, item.getCode());
        args.putString(ARG_STATUS, item.getStatus());
        args.putString(ARG_HSD, item.getHsd());
        args.putString(ARG_TYPE, item.getType());
        args.putBoolean(ARG_FROM_GIFT, item.isFromGift());
        args.putInt(ARG_MIN_ORDER_VALUE, item.getMinOrderValue());
        args.putString(ARG_APPLICABLE_PRODUCTS, item.getApplicableProducts());
        args.putString(ARG_OFFER_TYPE, item.getOfferType());
        args.putInt(ARG_DISCOUNT_VALUE, item.getDiscountValue());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountVoucherDetailFragmentBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            voucherId = getArguments().getString(ARG_ID, "");
            voucherTitle = getArguments().getString(ARG_TITLE, "");
            voucherDescription = getArguments().getString(ARG_DESCRIPTION, "");
            voucherCode = getArguments().getString(ARG_CODE, "");
            voucherStatus = getArguments().getString(ARG_STATUS, "");
            voucherHsd = getArguments().getString(ARG_HSD, "");
            voucherType = getArguments().getString(ARG_TYPE, "");
            fromGift = getArguments().getBoolean(ARG_FROM_GIFT, false);
            minOrderValue = getArguments().getInt(ARG_MIN_ORDER_VALUE, 0);
            applicableProducts = getArguments().getString(ARG_APPLICABLE_PRODUCTS, "Tất cả sản phẩm");
            offerType = getArguments().getString(ARG_OFFER_TYPE, "fixed_amount");
            discountValue = getArguments().getInt(ARG_DISCOUNT_VALUE, 0);
        }

        setupRepository();
        return binding.getRoot();
    }

    private void setupRepository() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        repository = new OrderRepository(
                db.orderDao(),
                db.rewardPointDao(),
                db.userGiftDao(),
                new LocalJsonReader(requireContext())
        );
    }

    @Override
    public void setupUI(@NonNull View view) {
        Context context = requireContext();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        View.OnClickListener shareAction = v -> {
            copyToClipboard(context, voucherCode);
            Toast.makeText(context, "Đã sao chép và chia sẻ mã voucher " + voucherCode + "!", Toast.LENGTH_SHORT).show();
        };
        binding.btnShare.setOnClickListener(shareAction);
        binding.btnShareVoucher.setOnClickListener(shareAction);

        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());

        String normalizedType = voucherType.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
        if (normalizedType.equals("freeship") || normalizedType.equals("voucherfreeship")) {
            binding.ivVoucherIcon.setImageResource(R.drawable.ic_truck);
        } else if (normalizedType.equals("gift") || normalizedType.equals("productgift") || normalizedType.equals("product") || normalizedType.equals("product_gift")) {
            binding.ivVoucherIcon.setImageResource(R.drawable.ic_gift);
        } else {
            binding.ivVoucherIcon.setImageResource(R.drawable.ic_voucher);
        }

        String formattedTitle;
        String voucherTypeLower = voucherType.toLowerCase(Locale.ROOT);
        if (voucherTypeLower.contains("freeship") || voucherTypeLower.contains("free ship")) {
            formattedTitle = "Free Ship";
        } else if ("percentage".equals(offerType)) {
            formattedTitle = "Giảm " + discountValue + "%";
        } else if ("fixed_amount".equals(offerType)) {
            formattedTitle = "Giảm " + (discountValue / 1000) + "K";
        } else if ("product_gift".equals(offerType)) {
            formattedTitle = "Quà tặng";
        } else {
            formattedTitle = voucherTitle;
        }
        binding.tvVoucherTitle.setText(formattedTitle);

        String formattedSubtitle = "Tối đa " + (minOrderValue / 1000) + "k";
        binding.tvVoucherSubtitle.setText(formattedSubtitle);

        binding.tvDetailTitle.setText(voucherTitle);
        binding.tvDetailDesc.setText(voucherDescription);

        binding.tvVoucherCode.setText(voucherCode);
        binding.btnCopyCode.setOnClickListener(v -> {
            copyToClipboard(context, voucherCode);
            Toast.makeText(context, "Đã sao chép mã voucher " + voucherCode + " thành công!", Toast.LENGTH_SHORT).show();
        });

        binding.tvCondMinOrder.setText("Áp dụng cho đơn hàng từ " + formatCurrency(minOrderValue) + " VNĐ.");

        String displayHsd = formatHsd(voucherHsd);
        binding.tvInfoExpiry.setText(displayHsd);

        String remainingText = computeRemainingDays(voucherHsd);
        binding.tvInfoRemaining.setText(remainingText);

        if ("Hết hạn".equals(remainingText) || "expired".equals(voucherStatus)) {
            binding.tvInfoRemaining.setTextColor(Color.parseColor("#C62828"));
            binding.tvInfoStatus.setText("Hết hiệu lực");
            binding.tvInfoStatus.setTextColor(Color.parseColor("#C62828"));
            
            binding.btnUseNow.setEnabled(false);
            binding.btnUseNow.setText("Đã hết hạn");
            binding.btnUseNow.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray_light));
            binding.btnUseNow.setTextColor(ContextCompat.getColor(context, R.color.gray_dark));
        } else if ("Hôm nay".equals(remainingText) || "expiring".equals(voucherStatus)) {
            binding.tvInfoRemaining.setTextColor(Color.parseColor("#C62828"));
            binding.tvInfoStatus.setText("Có hiệu lực");
        } else {
            binding.tvInfoRemaining.setTextColor(Color.parseColor("#7A9161"));
            binding.tvInfoStatus.setText("Có hiệu lực");
        }

        binding.tvInfoUsage.setText("0/1");

        binding.btnUseNow.setOnClickListener(v -> deleteVoucherFromList(success -> {
            if (success) {
                Toast.makeText(context, "Mã voucher " + voucherCode + " đã được áp dụng cho đơn hàng của bạn!", Toast.LENGTH_LONG).show();
            }
            BottomNavHelper.navigate(this, R.id.nav_shop);
        }));
    }

    private interface OnCompleteCallback {
        void onComplete(boolean success);
    }

    private void deleteVoucherFromList(OnCompleteCallback onComplete) {
        if (voucherId.startsWith("db_")) {
            String dbIdStr = voucherId.substring(3);
            try {
                int dbId = Integer.parseInt(dbIdStr);
                new Thread(() -> {
                    repository.deleteUserGiftById(dbId);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> onComplete.onComplete(true));
                    }
                }).start();
            } catch (NumberFormatException e) {
                onComplete.onComplete(false);
            }
        } else {
            AccountVoucherFragment.getDeletedSystemVoucherIdsStatic().add(voucherId);
            onComplete.onComplete(true);
        }
    }

    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xoá voucher")
                .setMessage("Bạn có chắc chắn muốn xoá " + voucherTitle + " khỏi ví voucher của bạn không?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    Context context = requireContext();
                    if (voucherId.startsWith("db_")) {
                        String dbIdStr = voucherId.substring(3);
                        try {
                            int dbId = Integer.parseInt(dbIdStr);
                            new Thread(() -> {
                                repository.deleteUserGiftById(dbId);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        Toast.makeText(context, "Đã xoá voucher thành công", Toast.LENGTH_SHORT).show();
                                        getParentFragmentManager().popBackStack();
                                    });
                                }
                            }).start();
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, "Xoá voucher thất bại", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        AccountVoucherFragment.getDeletedSystemVoucherIdsStatic().add(voucherId);
                        Toast.makeText(context, "Đã xoá voucher thành công", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private String formatCurrency(int amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amount);
    }

    private String formatHsd(String hsdStr) {
        try {
            SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputSdf.parse(hsdStr);
            if (date == null) return hsdStr;
            SimpleDateFormat outputSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outputSdf.format(date);
        } catch (Exception e) {
            return hsdStr.contains(" ") ? hsdStr.split(" ")[0] : hsdStr;
        }
    }

    private String computeRemainingDays(String hsdStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date expiryDate = sdf.parse(hsdStr);
            if (expiryDate == null) return "Còn hiệu lực";

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar expiry = Calendar.getInstance();
            expiry.setTime(expiryDate);
            expiry.set(Calendar.HOUR_OF_DAY, 0);
            expiry.set(Calendar.MINUTE, 0);
            expiry.set(Calendar.SECOND, 0);
            expiry.set(Calendar.MILLISECOND, 0);

            long diffMs = expiry.getTimeInMillis() - today.getTimeInMillis();
            long diffDays = diffMs / (1000 * 60 * 60 * 24);

            if (diffDays < 0) {
                return "Hết hạn";
            } else if (diffDays == 0L) {
                return "Hôm nay";
            } else {
                return diffDays + " ngày";
            }
        } catch (Exception e) {
            return "Còn hiệu lực";
        }
    }

    private void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Voucher Code", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    @Override
    public void observeViewModel() {
        // Not used
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
