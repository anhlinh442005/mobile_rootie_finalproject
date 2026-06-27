package com.veganbeauty.app.features.shop.product.list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.databinding.ShopBottomSheetSkinTypeFilterBinding;
import com.veganbeauty.app.features.shop.ShopViewModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SkinTypeFilterBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SkinTypeFilterBottomSheet";

    private ShopBottomSheetSkinTypeFilterBinding binding;
    private ShopViewModel viewModel;
    private final Set<String> selectedSkinTypes = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopBottomSheetSkinTypeFilterBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireParentFragment()).get(ShopViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadCurrentFilters();
        setupCheckboxes();
        setupButtons();
    }

    private void loadCurrentFilters() {
        if (viewModel.getCurrentSkinTypes() != null) {
            selectedSkinTypes.addAll(viewModel.getCurrentSkinTypes());
        }
    }

    private void setupCheckboxes() {
        Map<CheckBox, String> checkboxes = new HashMap<>();
        checkboxes.put(binding.cbSkinNormal, "Da thường");
        checkboxes.put(binding.cbSkinDry, "Da khô");
        checkboxes.put(binding.cbSkinOily, "Da dầu");
        checkboxes.put(binding.cbSkinSensitive, "Da nhạy cảm");
        checkboxes.put(binding.cbSkinCombination, "Da hỗn hợp");

        for (Map.Entry<CheckBox, String> entry : checkboxes.entrySet()) {
            CheckBox cb = entry.getKey();
            String value = entry.getValue();

            cb.setChecked(selectedSkinTypes.contains(value));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSkinTypes.add(value);
                } else {
                    selectedSkinTypes.remove(value);
                }
            });
        }
    }

    private void setupButtons() {
        binding.btnReset.setOnClickListener(v -> {
            selectedSkinTypes.clear();
            binding.cbSkinNormal.setChecked(false);
            binding.cbSkinDry.setChecked(false);
            binding.cbSkinOily.setChecked(false);
            binding.cbSkinSensitive.setChecked(false);
            binding.cbSkinCombination.setChecked(false);
        });

        binding.btnConfirm.setOnClickListener(v -> {
            // Lấy các filter khác từ viewModel để giữ nguyên
            viewModel.setAdvancedFilters(
                    selectedSkinTypes,
                    viewModel.getCurrentPriceRange(),
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
