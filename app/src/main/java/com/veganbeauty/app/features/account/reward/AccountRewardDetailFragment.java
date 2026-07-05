package com.veganbeauty.app.features.account.reward;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.dao.RewardPointDao;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.databinding.AccountRewardDetailFragmentBinding;

import java.util.List;

import kotlinx.coroutines.flow.FlowCollector;

public class AccountRewardDetailFragment extends RootieFragment {

    private static final String ARG_GIFT_ID = "arg_gift_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESCRIPTION = "arg_description";
    private static final String ARG_COST = "arg_cost";
    private static final String ARG_EXPIRY = "arg_expiry";
    private static final String ARG_CODE = "arg_code";
    private static final String ARG_TYPE = "arg_type";
    private static final String ARG_IS_OWNED = "arg_is_owned";
    private static final String ARG_DB_ID = "arg_db_id";
    private static final String ARG_RANK_REQUIRED = "arg_rank_required";
    private static final String ARG_MIN_ORDER_VALUE = "arg_min_order_value";
    private static final String ARG_APPLICABLE_PRODUCTS = "arg_applicable_products";
    private static final String ARG_OFFER_TYPE = "arg_offer_type";
    private static final String ARG_PRODUCT_ID = "arg_product_id";
    private static final String ARG_DISCOUNT_VALUE = "arg_discount_value";

    public static int selectRewardTabOnResume = 0;

    private AccountRewardDetailFragmentBinding _binding;
    private OrderRepository repository;

    private String giftId = "";
    private String giftTitle = "";
    private String giftDescription = "";
    private int giftCost = 0;
    private String giftExpiryDate = "";
    private String giftCode = "";
    private String giftType = "";
    private boolean isOwned = false;
    private int dbId = -1;
    private String rankRequired = "Đồng";
    private int minOrderValue = 0;
    private String applicableProducts = "Tất cả sản phẩm";
    private String offerType = "fixed_amount";
    private String productId = null;
    private int discountValue = 0;

    private int currentPoints = 8500;

    public static AccountRewardDetailFragment newInstance(
            String giftId, String title, String description, int cost,
            String expiryDate, String code, String giftType, boolean isOwned,
            int dbId, String rankRequired, int minOrderValue, String applicableProducts,
            String offerType, String productId, int discountValue) {

        AccountRewardDetailFragment fragment = new AccountRewardDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GIFT_ID, giftId);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putInt(ARG_COST, cost);
        args.putString(ARG_EXPIRY, expiryDate);
        args.putString(ARG_CODE, code);
        args.putString(ARG_TYPE, giftType);
        args.putBoolean(ARG_IS_OWNED, isOwned);
        args.putInt(ARG_DB_ID, dbId);
        args.putString(ARG_RANK_REQUIRED, rankRequired);
        args.putInt(ARG_MIN_ORDER_VALUE, minOrderValue);
        args.putString(ARG_APPLICABLE_PRODUCTS, applicableProducts);
        args.putString(ARG_OFFER_TYPE, offerType);
        args.putString(ARG_PRODUCT_ID, productId);
        args.putInt(ARG_DISCOUNT_VALUE, discountValue);
        fragment.setArguments(args);
        return fragment;
    }

    private AccountRewardDetailFragmentBinding getBinding() {
        return _binding;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = AccountRewardDetailFragmentBinding.inflate(inflater, container, false);
        
        if (getArguments() != null) {
            giftId = getArguments().getString(ARG_GIFT_ID, "");
            giftTitle = getArguments().getString(ARG_TITLE, "");
            giftDescription = getArguments().getString(ARG_DESCRIPTION, "");
            giftCost = getArguments().getInt(ARG_COST, 0);
            giftExpiryDate = getArguments().getString(ARG_EXPIRY, "");
            giftCode = getArguments().getString(ARG_CODE, "");
            giftType = getArguments().getString(ARG_TYPE, "");
            isOwned = getArguments().getBoolean(ARG_IS_OWNED, false);
            dbId = getArguments().getInt(ARG_DB_ID, -1);
            rankRequired = getArguments().getString(ARG_RANK_REQUIRED, "Đồng");
            minOrderValue = getArguments().getInt(ARG_MIN_ORDER_VALUE, 0);
            applicableProducts = getArguments().getString(ARG_APPLICABLE_PRODUCTS, "Tất cả sản phẩm");
            offerType = getArguments().getString(ARG_OFFER_TYPE, "fixed_amount");
            productId = getArguments().getString(ARG_PRODUCT_ID);
            discountValue = getArguments().getInt(ARG_DISCOUNT_VALUE, 0);
        }

        setupRepository();
        return getBinding().getRoot();
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
        getBinding().btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        getBinding().btnShare.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Đã copy link chia sẻ quà tặng!", Toast.LENGTH_SHORT).show()
        );

        getBinding().tvGiftTitle.setText(giftTitle);
        getBinding().tvGiftSubtitle.setText(giftDescription);
        getBinding().tvGiftCost.setText(String.format("%,d xu", giftCost).replace(',', '.'));
        
        String displayDate = giftExpiryDate.contains(" ") ? giftExpiryDate.split(" ")[0] : giftExpiryDate;
        getBinding().tvExpiryDate.setText(displayDate);
        
        if (isOwned) {
            getBinding().tvUsageCodeChip.setText(giftCode);
        } else {
            getBinding().tvUsageCodeChip.setText("******");
        }
        
        String days;
        if (displayDate.equals("Hôm nay") || displayDate.equals("2026-06-11")) {
            days = "Hôm nay";
        } else if (displayDate.equals("2026-12-15") || displayDate.equals("2026-12-31")) {
            days = "Còn 6 tháng";
        } else if (displayDate.equals("2027-01-15")) {
            days = "Còn 7 tháng";
        } else {
            days = "Còn hiệu lực";
        }
        getBinding().tvRemainingDays.setText(days);

        if (days.equals("Hôm nay")) {
            getBinding().tvRemainingDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_cancelled_text));
            getBinding().tvStatus.setText("Gấp");
        } else if (days.equals("Hết hạn")) {
            getBinding().tvRemainingDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark));
            getBinding().tvRemainingDays.setText("Hết hạn");
            getBinding().tvStatus.setText("Hết hiệu lực");
        } else {
            getBinding().tvRemainingDays.setTextColor(Color.parseColor("#7A9161"));
            getBinding().tvStatus.setText("Có hiệu lực");
        }

        getBinding().tvUsageLimit.setText(giftType.startsWith("voucher") ? "0/1" : "Chỉ nhận 1 lần");

        getBinding().tvRuleApplicableProducts.setText("Áp dụng cho: " + applicableProducts);
        if (minOrderValue > 0) {
            getBinding().tvRuleMinOrderValue.setText("Hóa đơn tối thiểu " + String.format("%,d đ", minOrderValue).replace(',', '.'));
        } else {
            getBinding().tvRuleMinOrderValue.setText("Áp dụng cho mọi hóa đơn");
        }
        
        switch (offerType) {
            case "percentage":
                getBinding().tvRuleOfferType.setText("Ưu đãi: Giảm " + discountValue + "%");
                break;
            case "fixed_amount":
                getBinding().tvRuleOfferType.setText("Ưu đãi: Giảm " + String.format("%,d đ", discountValue).replace(',', '.'));
                break;
            case "product_gift":
                getBinding().tvRuleOfferType.setText("Ưu đãi: Tặng kèm sản phẩm");
                break;
            default:
                getBinding().tvRuleOfferType.setText("Ưu đãi đặc quyền từ Rootie");
                break;
        }

        if (isOwned) {
            getBinding().btnRedeemNow.setText("Đã sở hữu");
            getBinding().btnRedeemNow.setEnabled(false);
            getBinding().btnRedeemNow.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray_light));
            getBinding().btnRedeemNow.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark));
            
            getBinding().btnRemove.setOnClickListener(v -> showRemoveConfirmationDialog());
            getBinding().btnRemove.setVisibility(View.VISIBLE);
        } else {
            getBinding().btnRemove.setVisibility(View.GONE);

            getBinding().btnRedeemNow.setOnClickListener(v -> handleRedeemAction());
        }
    }

    @Override
    public void observeViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        androidx.lifecycle.FlowLiveDataConversions.asLiveData(db.rewardPointDao().getTotalPointsFlow())
            .observe(getViewLifecycleOwner(), ptsList -> {
                int pointsVal = (ptsList != null && !ptsList.isEmpty()) ? ptsList.get(0).total : 0;
                currentPoints = pointsVal;
                if (_binding != null) {
                    _binding.tvUserBalance.setText("Bạn hiện có: " + String.format("%,d xu", pointsVal).replace(',', '.'));

                    boolean isLockedByRank = (rankRequired.equals("Vàng") && pointsVal < 10000) || 
                                             (rankRequired.equals("VIP") && pointsVal < 20000) ||
                                             (rankRequired.equals("Kim Cương") && pointsVal < 20000);
                    boolean isNotEnoughPoints = pointsVal < giftCost;

                    if (pointsVal >= giftCost) {
                        _binding.pbRedeemProgress.setProgress(100);
                        _binding.tvQualifyStatus.setText("✓ Đủ điều kiện");
                        _binding.tvQualifyStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                    } else {
                        _binding.pbRedeemProgress.setProgress(pointsVal * 100 / giftCost);
                        _binding.tvQualifyStatus.setText("✗ Chưa đủ xu");
                        _binding.tvQualifyStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_cancelled_text));
                    }

                    if (!isOwned) {
                        if (isLockedByRank) {
                            _binding.btnRedeemNow.setEnabled(false);
                            _binding.btnRedeemNow.setText("Chưa đủ hạng");
                            _binding.btnRedeemNow.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray_light));
                            _binding.btnRedeemNow.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark));
                        } else if (isNotEnoughPoints) {
                            _binding.btnRedeemNow.setEnabled(false);
                            _binding.btnRedeemNow.setText("Không đủ xu");
                            _binding.btnRedeemNow.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.gray_light));
                            _binding.btnRedeemNow.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark));
                        } else {
                            _binding.btnRedeemNow.setEnabled(true);
                            _binding.btnRedeemNow.setText("Đổi quà");
                            _binding.btnRedeemNow.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
                            _binding.btnRedeemNow.setTextColor(Color.WHITE);
                        }
                    }
                }
            });
    }

    private void handleRedeemAction() {
        if (currentPoints < giftCost) {
            Toast.makeText(requireContext(), "Bạn không đủ xu để đổi quà tặng này!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                boolean success = repository.redeemGift(
                        giftId, giftTitle, giftDescription, giftCost, giftExpiryDate, giftCode,
                        giftType, minOrderValue, applicableProducts, offerType, productId, discountValue
                );

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            selectRewardTabOnResume = 1;

                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Đổi quà thành công!")
                                    .setMessage("Chúc mừng! Bạn đã đổi thành công " + giftTitle + ". Quà tặng đã được lưu vào mục 'Quà của tôi'.")
                                    .setPositiveButton("Xem danh sách quà", (dialog, which) -> getParentFragmentManager().popBackStack())
                                    .setCancelable(false)
                                    .show();
                        } else {
                            Toast.makeText(requireContext(), "Đổi quà thất bại. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showRemoveConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xoá quà tặng")
                .setMessage("Bạn có chắc chắn muốn xoá " + giftTitle + " khỏi danh sách quà của tôi không?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            boolean deleted = repository.deleteUserGiftById(dbId);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (deleted) {
                                        Toast.makeText(requireContext(), "Đã xoá quà tặng khỏi danh sách!", Toast.LENGTH_SHORT).show();
                                        getParentFragmentManager().popBackStack();
                                    } else {
                                        Toast.makeText(requireContext(), "Xoá quà tặng thất bại!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
