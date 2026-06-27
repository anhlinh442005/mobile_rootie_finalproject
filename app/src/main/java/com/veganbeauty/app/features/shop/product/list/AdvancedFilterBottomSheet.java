package com.veganbeauty.app.features.shop.product.list;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.databinding.ShopBottomSheetAdvancedFilterBinding;
import com.veganbeauty.app.features.shop.ShopViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdvancedFilterBottomSheet extends BottomSheetDialogFragment {

    private ShopBottomSheetAdvancedFilterBinding _binding;
    private ShopViewModel viewModel;

    private final Set<String> selectedSkinTypes = new HashSet<>();
    private String selectedPriceRange = null;
    private final Set<String> selectedBenefits = new HashSet<>();
    private final Set<String> selectedIngredients = new HashSet<>();

    public static final String TAG = "AdvancedFilterBottomSheet";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = ShopBottomSheetAdvancedFilterBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireParentFragment()).get(ShopViewModel.class);
        return _binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadCurrentFilters();
        setupExpandCollapse();
        setupPriceChips();
        setupCheckboxes();
        setupFooterButtons();
        renderFilterTags();
    }

    private void loadCurrentFilters() {
        if (viewModel.getCurrentSkinTypes() != null) {
            selectedSkinTypes.addAll(viewModel.getCurrentSkinTypes());
        }
        selectedPriceRange = viewModel.getCurrentPriceRange();
        if (viewModel.getCurrentBenefits() != null) {
            selectedBenefits.addAll(viewModel.getCurrentBenefits());
        }
        if (viewModel.getCurrentIngredients() != null) {
            selectedIngredients.addAll(viewModel.getCurrentIngredients());
        }
    }

    private void setupExpandCollapse() {
        _binding.headerSkinType.setOnClickListener(v -> {
            boolean isVisible = _binding.layoutSkinTypeOptions.getVisibility() == View.VISIBLE;
            _binding.layoutSkinTypeOptions.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            _binding.ivChevronSkinType.setRotation(isVisible ? 0f : 90f);
        });

        _binding.headerPrice.setOnClickListener(v -> {
            boolean isVisible = _binding.layoutPriceOptions.getVisibility() == View.VISIBLE;
            _binding.layoutPriceOptions.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            _binding.ivChevronPrice.setRotation(isVisible ? 0f : 90f);
        });

        _binding.headerUsage.setOnClickListener(v -> {
            boolean isVisible = _binding.layoutUsageOptions.getVisibility() == View.VISIBLE;
            _binding.layoutUsageOptions.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            _binding.ivChevronUsage.setRotation(isVisible ? 0f : 90f);
        });

        _binding.headerIngredients.setOnClickListener(v -> {
            boolean isVisible = _binding.layoutIngredientsOptions.getVisibility() == View.VISIBLE;
            _binding.layoutIngredientsOptions.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            _binding.ivChevronIngredients.setRotation(isVisible ? 0f : 90f);
        });
    }

    private void setupPriceChips() {
        class ChipData {
            TextView chip;
            String value;
            ChipData(TextView chip, String value) {
                this.chip = chip;
                this.value = value;
            }
        }

        List<ChipData> chips = new ArrayList<>();
        chips.add(new ChipData(_binding.chipPrice1, "Dưới 100.000đ"));
        chips.add(new ChipData(_binding.chipPrice2, "100.000đ - 300.000đ"));
        chips.add(new ChipData(_binding.chipPrice3, "300.000đ - 500.000đ"));
        chips.add(new ChipData(_binding.chipPrice4, "Trên 500.000đ"));

        updateChipsUi(chips);

        for (ChipData cd : chips) {
            cd.chip.setOnClickListener(v -> {
                if (selectedPriceRange != null && selectedPriceRange.equals(cd.value)) {
                    selectedPriceRange = null;
                } else {
                    selectedPriceRange = cd.value;
                }
                updateChipsUi(chips);
                renderFilterTags();
            });
        }
    }

    private void updateChipsUi(List<Object> chipsObj) {
        Typeface regTypeface = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_regular);
        Typeface medTypeface = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_medium);

        for (Object obj : chipsObj) {
            try {
                java.lang.reflect.Field chipField = obj.getClass().getDeclaredField("chip");
                java.lang.reflect.Field valueField = obj.getClass().getDeclaredField("value");
                TextView chip = (TextView) chipField.get(obj);
                String value = (String) valueField.get(obj);

                if (selectedPriceRange != null && selectedPriceRange.equals(value)) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected);
                    chip.setTextColor(Color.parseColor("#3E4D44"));
                    chip.setTypeface(medTypeface);
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_normal);
                    chip.setTextColor(Color.parseColor("#555555"));
                    chip.setTypeface(regTypeface);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setupCheckboxes() {
        Map<CheckBox, String> skinCheckboxes = new HashMap<>();
        skinCheckboxes.put(_binding.cbSkinNormal, "Da thường");
        skinCheckboxes.put(_binding.cbSkinDry, "Da khô");
        skinCheckboxes.put(_binding.cbSkinOily, "Da dầu");
        skinCheckboxes.put(_binding.cbSkinSensitive, "Da nhạy cảm");
        skinCheckboxes.put(_binding.cbSkinCombination, "Da hỗn hợp");

        for (Map.Entry<CheckBox, String> entry : skinCheckboxes.entrySet()) {
            entry.getKey().setChecked(selectedSkinTypes.contains(entry.getValue()));
            entry.getKey().setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSkinTypes.add(entry.getValue());
                } else {
                    selectedSkinTypes.remove(entry.getValue());
                }
                renderFilterTags();
            });
        }

        Map<CheckBox, String> usageCheckboxes = new HashMap<>();
        usageCheckboxes.put(_binding.cbUsageClean, "Làm sạch");
        usageCheckboxes.put(_binding.cbUsageMoisturize, "Dưỡng ẩm");

        for (Map.Entry<CheckBox, String> entry : usageCheckboxes.entrySet()) {
            entry.getKey().setChecked(selectedBenefits.contains(entry.getValue()));
            entry.getKey().setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedBenefits.add(entry.getValue());
                } else {
                    selectedBenefits.remove(entry.getValue());
                }
                renderFilterTags();
            });
        }

        Map<CheckBox, String> ingredientCheckboxes = new HashMap<>();
        ingredientCheckboxes.put(_binding.cbIngredientVegan, "Chay");
        ingredientCheckboxes.put(_binding.cbIngredientNatural, "Tự nhiên");

        for (Map.Entry<CheckBox, String> entry : ingredientCheckboxes.entrySet()) {
            entry.getKey().setChecked(selectedIngredients.contains(entry.getValue()));
            entry.getKey().setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedIngredients.add(entry.getValue());
                } else {
                    selectedIngredients.remove(entry.getValue());
                }
                renderFilterTags();
            });
        }
    }

    private void renderFilterTags() {
        _binding.layoutFilterTags.removeAllViews();

        class FilterAction {
            String name;
            Runnable action;
            FilterAction(String name, Runnable action) {
                this.name = name;
                this.action = action;
            }
        }

        List<FilterAction> allActiveFilters = new ArrayList<>();

        for (String skinType : new ArrayList<>(selectedSkinTypes)) {
            allActiveFilters.add(new FilterAction(skinType, () -> {
                selectedSkinTypes.remove(skinType);
                refreshCheckboxes();
                renderFilterTags();
            }));
        }

        if (selectedPriceRange != null) {
            allActiveFilters.add(new FilterAction(selectedPriceRange, () -> {
                selectedPriceRange = null;
                setupPriceChips();
                renderFilterTags();
            }));
        }

        for (String benefit : new ArrayList<>(selectedBenefits)) {
            allActiveFilters.add(new FilterAction(benefit, () -> {
                selectedBenefits.remove(benefit);
                refreshCheckboxes();
                renderFilterTags();
            }));
        }

        for (String ingredient : new ArrayList<>(selectedIngredients)) {
            allActiveFilters.add(new FilterAction(ingredient, () -> {
                selectedIngredients.remove(ingredient);
                refreshCheckboxes();
                renderFilterTags();
            }));
        }

        _binding.tvActiveFiltersCount.setText("Lọc theo (" + allActiveFilters.size() + ")");

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (FilterAction fa : allActiveFilters) {
            View tagView = inflater.inflate(R.layout.shop_item_filter_tag, _binding.layoutFilterTags, false);
            TextView tvName = tagView.findViewById(R.id.tvTagName);
            View ivRemove = tagView.findViewById(R.id.ivRemoveTag);

            tvName.setText(fa.name);
            ivRemove.setOnClickListener(v -> fa.action.run());
            _binding.layoutFilterTags.addView(tagView);
        }
    }

    private void refreshCheckboxes() {
        _binding.cbSkinNormal.setChecked(selectedSkinTypes.contains("Da thường"));
        _binding.cbSkinDry.setChecked(selectedSkinTypes.contains("Da khô"));
        _binding.cbSkinOily.setChecked(selectedSkinTypes.contains("Da dầu"));
        _binding.cbSkinSensitive.setChecked(selectedSkinTypes.contains("Da nhạy cảm"));
        _binding.cbSkinCombination.setChecked(selectedSkinTypes.contains("Da hỗn hợp"));

        _binding.cbUsageClean.setChecked(selectedBenefits.contains("Làm sạch"));
        _binding.cbUsageMoisturize.setChecked(selectedBenefits.contains("Dưỡng ẩm"));

        _binding.cbIngredientVegan.setChecked(selectedIngredients.contains("Chay"));
        _binding.cbIngredientNatural.setChecked(selectedIngredients.contains("Tự nhiên"));
    }

    private void setupFooterButtons() {
        _binding.btnReset.setOnClickListener(v -> {
            selectedSkinTypes.clear();
            selectedPriceRange = null;
            selectedBenefits.clear();
            selectedIngredients.clear();

            refreshCheckboxes();
            setupPriceChips();
            renderFilterTags();
        });

        _binding.btnConfirm.setOnClickListener(v -> {
            viewModel.setAdvancedFilters(
                    new HashSet<>(selectedSkinTypes),
                    selectedPriceRange,
                    new HashSet<>(selectedBenefits),
                    new HashSet<>(selectedIngredients)
            );
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
