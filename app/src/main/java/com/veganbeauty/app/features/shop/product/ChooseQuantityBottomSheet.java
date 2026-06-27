package com.veganbeauty.app.features.shop.product;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import coil.Coil;
import coil.request.ImageRequest;
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

        ImageRequest request = new ImageRequest.Builder(requireContext())
                .data(product.getMainImage())
                .target(binding.ivProduct)
                .crossfade(true)
                .placeholder(android.R.color.darker_gray)
                .build();
        Coil.imageLoader(requireContext()).enqueue(request);

        binding.tvQuantityValue.setText(String.valueOf(currentQuantity));
    }

    private void setupListeners() {
        binding.btnMinus.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                binding.tvQuantityValue.setText(String.valueOf(currentQuantity));
                updatePriceAndSavings();
            }
        });

        binding.btnPlus.setOnClickListener(v -> {
            currentQuantity++;
            binding.tvQuantityValue.setText(String.valueOf(currentQuantity));
            updatePriceAndSavings();
        });

        binding.btnAddToCartOutline.setOnClickListener(v -> {
            if (listener != null) listener.onAddToCartClick(product, currentQuantity);
            dismiss();
        });

        binding.btnBuyNow.setOnClickListener(v -> {
            if (listener != null) listener.onBuyNowClick(product, currentQuantity);
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
