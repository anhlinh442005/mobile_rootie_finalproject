package com.veganbeauty.app.features.shop.product;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.ShopBottomSheetBuyBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class ChooseQuantityBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ChooseQuantityBottomSheet";

    public interface OnQuantitySelectedListener {
        void onAddToCartClick(ProductEntity product, int quantity);
        void onBuyNowClick(ProductEntity product, int quantity);
    }

    private ShopBottomSheetBuyBinding binding;
    private final ProductEntity product;
    private final OnQuantitySelectedListener listener;
    private int currentQuantity = 1;

    public ChooseQuantityBottomSheet(ProductEntity product, OnQuantitySelectedListener listener) {
        this.product = product;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopBottomSheetBuyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupProductInfo();
        setupListeners();
        updatePriceAndSavings();
    }

    private void setupProductInfo() {
        binding.tvProductName.setText(product.getName());

        com.veganbeauty.app.utils.ProductImageHelper.loadProductImage(binding.ivProduct, product);

        binding.tvQuantityValue.setText(String.valueOf(currentQuantity));
    }

    private void setupListeners() {
        binding.tvQuantityValue.addTextChangedListener(new android.text.TextWatcher() {
            private String currentText = "";
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String input = s.toString().trim();
                if (input.equals(currentText)) return;
                if (input.isEmpty()) {
                    return;
                }
                try {
                    int val = Integer.parseInt(input);
                    if (val < 1) {
                        currentText = "1";
                        binding.tvQuantityValue.setText("1");
                        if (binding.tvQuantityValue instanceof android.widget.EditText) {
                            ((android.widget.EditText) binding.tvQuantityValue).setSelection(1);
                        }
                        currentQuantity = 1;
                        updatePriceAndSavings();
                    } else if (val > product.getStock()) {
                        android.widget.Toast.makeText(requireContext(), "Số lượng chọn vượt quá tồn kho (" + product.getStock() + ")", android.widget.Toast.LENGTH_SHORT).show();
                        currentText = String.valueOf(product.getStock());
                        binding.tvQuantityValue.setText(currentText);
                        if (binding.tvQuantityValue instanceof android.widget.EditText) {
                            ((android.widget.EditText) binding.tvQuantityValue).setSelection(currentText.length());
                        }
                        currentQuantity = product.getStock();
                        updatePriceAndSavings();
                    } else {
                        currentText = String.valueOf(val);
                        currentQuantity = val;
                        updatePriceAndSavings();
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        });

        binding.tvQuantityValue.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String input = binding.tvQuantityValue.getText().toString().trim();
                if (input.isEmpty()) {
                    binding.tvQuantityValue.setText(String.valueOf(currentQuantity));
                }
            }
        });

        binding.btnMinus.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                binding.tvQuantityValue.setText(String.valueOf(currentQuantity - 1));
            }
        });

        binding.btnPlus.setOnClickListener(v -> {
            if (currentQuantity >= product.getStock()) {
                android.widget.Toast.makeText(requireContext(), "Số lượng chọn vượt quá tồn kho (" + product.getStock() + ")", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            binding.tvQuantityValue.setText(String.valueOf(currentQuantity + 1));
        });

        binding.btnAddToCartOutline.setOnClickListener(v -> {
            String input = binding.tvQuantityValue.getText().toString().trim();
            if (input.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Vui lòng nhập số lượng", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            int finalQty = currentQuantity;
            try {
                finalQty = Integer.parseInt(input);
            } catch (NumberFormatException e) {}
            if (finalQty > product.getStock()) {
                android.widget.Toast.makeText(requireContext(), "Số lượng chọn vượt quá tồn kho (" + product.getStock() + ")", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) listener.onAddToCartClick(product, finalQty);
            dismiss();
        });

        binding.btnBuyNow.setOnClickListener(v -> {
            String input = binding.tvQuantityValue.getText().toString().trim();
            if (input.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Vui lòng nhập số lượng", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            int finalQty = currentQuantity;
            try {
                finalQty = Integer.parseInt(input);
            } catch (NumberFormatException e) {}
            if (finalQty > product.getStock()) {
                android.widget.Toast.makeText(requireContext(), "Số lượng chọn vượt quá tồn kho (" + product.getStock() + ")", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) listener.onBuyNowClick(product, finalQty);
            dismiss();
        });
    }

    private void updatePriceAndSavings() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        long currentPrice = product.getPrice();
        long originalPrice = (long) (product.getPrice() * 1.2);

        binding.tvPrice.setText(formatter.format(currentPrice));
        binding.tvOriginalPrice.setText(formatter.format(originalPrice));
        binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        long subtotal = currentPrice * currentQuantity;
        binding.tvSubtotalValue.setText(formatter.format(subtotal));

        long savings = (originalPrice - currentPrice) * currentQuantity;
        binding.tvSavingsValue.setText(formatter.format(savings));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
