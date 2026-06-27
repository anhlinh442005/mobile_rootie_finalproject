package com.veganbeauty.app.features.community.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;

import java.util.Arrays;
import java.util.List;

public class CommunitySortBottomSheet extends BottomSheetDialogFragment {

    public interface OnSortSelectedListener {
        void onSortSelected(int sortOption);
    }

    private final int currentSort;
    private final OnSortSelectedListener onSortSelected;

    public CommunitySortBottomSheet(int currentSort, OnSortSelectedListener onSortSelected) {
        this.currentSort = currentSort;
        this.onSortSelected = onSortSelected;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_bottom_sheet_sort, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.ivClose).setOnClickListener(v -> dismiss());

        ImageView ivRadioCreator = view.findViewById(R.id.ivRadioCreator);
        ImageView ivRadioSuggest = view.findViewById(R.id.ivRadioSuggest);
        ImageView ivRadioBestSeller = view.findViewById(R.id.ivRadioBestSeller);
        ImageView ivRadioPriceLow = view.findViewById(R.id.ivRadioPriceLow);
        ImageView ivRadioPriceHigh = view.findViewById(R.id.ivRadioPriceHigh);

        List<ImageView> radios = Arrays.asList(ivRadioCreator, ivRadioSuggest, ivRadioBestSeller, ivRadioPriceLow, ivRadioPriceHigh);

        updateUI(radios, currentSort);

        List<Integer> layouts = Arrays.asList(
                R.id.layoutSortCreator,
                R.id.layoutSortSuggest,
                R.id.layoutSortBestSeller,
                R.id.layoutSortPriceLow,
                R.id.layoutSortPriceHigh
        );

        for (int i = 0; i < layouts.size(); i++) {
            final int index = i;
            view.findViewById(layouts.get(i)).setOnClickListener(v -> {
                updateUI(radios, index);
                onSortSelected.onSortSelected(index);
                dismiss();
            });
        }
    }

    private void updateUI(List<ImageView> radios, int selected) {
        for (int i = 0; i < radios.size(); i++) {
            if (i == selected) {
                radios.get(i).setImageResource(R.drawable.ic_radio_primary_checked);
            } else {
                radios.get(i).setImageResource(R.drawable.ic_radio_primary_unchecked);
            }
        }
    }
}
