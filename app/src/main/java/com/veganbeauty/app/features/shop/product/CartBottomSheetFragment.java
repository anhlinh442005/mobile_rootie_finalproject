package com.veganbeauty.app.features.shop.product;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.FlowLiveDataConversions;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.databinding.ShopBottomSheetCartBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class CartBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "CartBottomSheetFragment";
    public static final String ARG_SELECTED_VOUCHER_CODE = "selected_voucher_code";
    public static final String ARG_SELECTED_VOUCHER_DISCOUNT = "selected_voucher_discount";

    private ShopBottomSheetCartBinding _binding;
    private RootieDatabase database;
    private CartAdapter adapter;

    private String selectedVoucherCode = null;
    private long voucherDiscountAmount = 0L;
    private boolean isPointsChecked = false;

    public static CartBottomSheetFragment newInstance(String selectedCode, long discount) {
        CartBottomSheetFragment fragment = new CartBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_VOUCHER_CODE, selectedCode);
        args.putLong(ARG_SELECTED_VOUCHER_DISCOUNT, discount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedVoucherCode = getArguments().getString(ARG_SELECTED_VOUCHER_CODE);
            voucherDiscountAmount = getArguments().getLong(ARG_SELECTED_VOUCHER_DISCOUNT, 0L);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ShopBottomSheetCartBinding.inflate(inflater, container, false);
        database = RootieDatabase.getDatabase(requireContext());
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (_binding == null) return;

        setupRecyclerView();
        setupListeners();
        applyGuestCartMode();
        observeCartItems();
    }

    private void applyGuestCartMode() {
        if (!ProfileSession.isLoggedIn(requireContext())) {
            _binding.llPointsRow.setVisibility(View.GONE);
            isPointsChecked = false;
            return;
        }
        _binding.llPointsRow.setVisibility(View.VISIBLE);
        refreshPointsRow();
    }

    private void setupRecyclerView() {
        adapter = new CartAdapter(
                (item, newQuantity) -> {
                    new Thread(() -> {
                        if (newQuantity <= 0) {
                            database.cartDao().deleteCartItem(item);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), "Đã xóa " + item.getName() + " khỏi giỏ hàng", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            CartItemEntity updatedItem = new CartItemEntity(
                                    item.getId(), item.getName(), item.getImage(), item.getPrice(), newQuantity, item.isSelected()
                            );
                            database.cartDao().updateCartItem(updatedItem);
                        }
                    }).start();
                },
                (item, isSelected) -> {
                    new Thread(() -> {
                        CartItemEntity updatedItem = new CartItemEntity(
                                item.getId(), item.getName(), item.getImage(), item.getPrice(), item.getQuantity(), isSelected
                        );
                        database.cartDao().updateCartItem(updatedItem);
                    }).start();
                }
        );

        _binding.rvCartItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        _binding.rvCartItems.setAdapter(adapter);
    }

    private void setupListeners() {
        _binding.llSelectAll.setOnClickListener(v -> {
            List<CartItemEntity> currentList = adapter.getCurrentList();
            boolean allSelected = true;
            for (CartItemEntity item : currentList) {
                if (!item.isSelected()) {
                    allSelected = false;
                    break;
                }
            }
            boolean finalAllSelected = allSelected;
            new Thread(() -> {
                for (CartItemEntity item : currentList) {
                    CartItemEntity updatedItem = new CartItemEntity(
                            item.getId(), item.getName(), item.getImage(), item.getPrice(), item.getQuantity(), !finalAllSelected
                    );
                    database.cartDao().updateCartItem(updatedItem);
                }
            }).start();
        });

        _binding.switchPoints.setOnClickListener(v -> refreshPointsRow());

        _binding.llVoucherRow.setOnClickListener(v -> {
            ShopVoucherFragment voucherFragment = ShopVoucherFragment.newInstance(selectedVoucherCode);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, voucherFragment)
                    .addToBackStack(null)
                    .commit();
            dismiss();
        });

        _binding.llPrices.setOnClickListener(v -> {
            boolean isVisible = _binding.llPriceDetails.getVisibility() == View.VISIBLE;
            if (isVisible) {
                _binding.llPriceDetails.setVisibility(View.GONE);
                _binding.ivPriceToggleArrow.setRotation(270f);
            } else {
                _binding.llPriceDetails.setVisibility(View.VISIBLE);
                _binding.ivPriceToggleArrow.setRotation(90f);

                if (getDialog() instanceof BottomSheetDialog) {
                    BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) getDialog();
                    View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    if (bottomSheet != null) {
                        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }
            }
        });

        _binding.btnCheckout.setOnClickListener(v -> {
            List<CartItemEntity> selectedItems = new ArrayList<>();
            for (CartItemEntity item : adapter.getCurrentList()) {
                if (item.isSelected()) {
                    selectedItems.add(item);
                }
            }

            if (selectedItems.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
            } else {
                ArrayList<CartItemEntity> checkoutItems = new ArrayList<>(selectedItems);
                ShopCheckoutFragment checkoutFragment = ShopCheckoutFragment.newInstance(
                        checkoutItems,
                        selectedVoucherCode,
                        voucherDiscountAmount
                );
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, checkoutFragment)
                        .addToBackStack(null)
                        .commit();
                dismiss();
            }
        });
    }

    private void observeCartItems() {
        FlowLiveDataConversions.asLiveData(database.cartDao().getAllCartItems())
            .observe(getViewLifecycleOwner(), items -> {
                if (items != null) {
                    adapter.submitList(items);
                    updateUI(items);
                }
            });
    }

    private void updateUI(List<CartItemEntity> items) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        List<CartItemEntity> selectedItems = new ArrayList<>();
        int totalSelectedQty = 0;
        for (CartItemEntity item : items) {
            if (item.isSelected()) {
                selectedItems.add(item);
                totalSelectedQty += item.getQuantity();
            }
        }

        long originalPriceSum = 0;
        long finalPriceSum = 0;
        for (CartItemEntity item : selectedItems) {
            originalPriceSum += (long) (item.getPrice() * 1.2) * item.getQuantity();
            finalPriceSum += item.getPrice() * item.getQuantity();
        }

        boolean isVoucherApplied = selectedVoucherCode != null && !selectedVoucherCode.isEmpty();
        if (isVoucherApplied && finalPriceSum > voucherDiscountAmount) {
            finalPriceSum -= voucherDiscountAmount;
        }

        long totalSavings = originalPriceSum - finalPriceSum;

        _binding.tvDetailSubtotal.setText(formatter.format(originalPriceSum));
        long directDiscount = originalPriceSum - finalPriceSum;
        if (directDiscount - voucherDiscountAmount > 0) {
            _binding.tvDetailDirectDiscount.setText("-" + formatter.format(directDiscount - voucherDiscountAmount));
        } else {
            _binding.tvDetailDirectDiscount.setText("0đ");
        }

        if (voucherDiscountAmount > 0) {
            _binding.tvDetailVoucherDiscount.setText("-" + formatter.format(voucherDiscountAmount));
        } else {
            _binding.tvDetailVoucherDiscount.setText("0đ");
        }

        if (isVoucherApplied) {
            _binding.tvVoucherDesc.setText("Đã áp dụng mã: " + selectedVoucherCode + " (-" + formatter.format(voucherDiscountAmount) + ")");
            _binding.llVoucherRow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5F2FF")));
        } else {
            _binding.tvVoucherDesc.setText("Áp dụng ưu đãi để được giảm giá");
            _binding.llVoucherRow.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EDF3ED")));
        }

        _binding.tvOriginalTotalPrice.setText(formatter.format(originalPriceSum));
        _binding.tvOriginalTotalPrice.setPaintFlags(_binding.tvOriginalTotalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        _binding.tvTotalValue.setText(formatter.format(finalPriceSum));
        _binding.tvSavingsValue.setText(formatter.format(totalSavings));

        refreshPointsRow();

        _binding.btnCheckout.setText("Mua hàng");

        _binding.tvSelectAllLabel.setText("Chọn tất cả (" + items.size() + ")");
        boolean allSelected = !items.isEmpty();
        for (CartItemEntity item : items) {
            if (!item.isSelected()) {
                allSelected = false;
                break;
            }
        }
        if (allSelected) {
            _binding.ivSelectAll.setImageResource(R.drawable.ic_circle_checked);
            _binding.ivSelectAll.setImageTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
        } else {
            _binding.ivSelectAll.setImageResource(R.drawable.ic_circle);
            _binding.ivSelectAll.setImageTintList(null);
        }
    }

    private void refreshPointsRow() {
        if (_binding == null || !isAdded() || !ProfileSession.isLoggedIn(requireContext())) return;
        int balance = Math.max(0, com.veganbeauty.app.utils.RewardPointsHelper.getTotalPoints(requireContext()));
        String balanceText = String.format(Locale.getDefault(), "%,d", balance).replace(',', '.');
        _binding.tvPointsText.setText("Bạn có " + balanceText + " xu");
        isPointsChecked = false;
        updateSwitchUI(_binding.switchPoints, _binding.switchPointsThumb, false);
    }

    private void updateSwitchUI(FrameLayout container, ImageView thumb, boolean enabled) {
        if (enabled) {
            container.setBackgroundResource(R.drawable.ic_switch_track_on);
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
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
