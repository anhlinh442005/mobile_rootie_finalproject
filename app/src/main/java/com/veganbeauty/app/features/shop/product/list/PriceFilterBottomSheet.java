package com.veganbeauty.app.features.shop.product.list;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.databinding.ShopBottomSheetPriceFilterBinding;
import com.veganbeauty.app.features.shop.ShopViewModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class PriceFilterBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "PriceFilterBottomSheet";

    private ShopBottomSheetPriceFilterBinding binding;
    private ShopViewModel viewModel;
    private String selectedPriceRange = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopBottomSheetPriceFilterBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireParentFragment()).get(ShopViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadCurrentFilters();
        setupPriceChips();
        setupButtons();
    }

    private void loadCurrentFilters() {
        selectedPriceRange = viewModel.getCurrentPriceRange();
    }

    private void setupPriceChips() {
        Map<View, String> chips = new LinkedHashMap<>();
        chips.put(binding.chipPrice1, "Dưới 100.000đ");
        chips.put(binding.chipPrice2, "100.000đ - 300.000đ");
        chips.put(binding.chipPrice3, "300.000đ - 500.000đ");
        chips.put(binding.chipPrice4, "Trên 500.000đ");

        updateChipsUi(chips);

        for (Map.Entry<View, String> entry : chips.entrySet()) {
            View chip = entry.getKey();
            String value = entry.getValue();

            chip.setOnClickListener(v -> {
                if (value.equals(selectedPriceRange)) {
                    selectedPriceRange = null;
                } else {
                    selectedPriceRange = value;
                }
                updateChipsUi(chips);
            });
        }
    }

    private void updateChipsUi(Map<View, String> chips) {
        Typeface regTypeface = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_regular);
        Typeface medTypeface = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_medium);

        for (Map.Entry<View, String> entry : chips.entrySet()) {
            android.widget.TextView chip = (android.widget.TextView) entry.getKey();
            String value = entry.getValue();

            if (value.equals(selectedPriceRange)) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTextColor(Color.parseColor("#3E4D44"));
                chip.setTypeface(medTypeface);
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_normal);
                chip.setTextColor(Color.parseColor("#555555"));
                chip.setTypeface(regTypeface);
            }
        }
    }

    private void setupButtons() {
        binding.btnReset.setOnClickListener(v -> {
            selectedPriceRange = null;
            setupPriceChips();
        });

        binding.btnConfirm.setOnClickListener(v -> {
            viewModel.setAdvancedFilters(
                    viewModel.getCurrentSkinTypes(),
                    selectedPriceRange,
                    viewModel.getCurrentBenefits(),
                    viewModel.getCurrentIngredients()
            );
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
