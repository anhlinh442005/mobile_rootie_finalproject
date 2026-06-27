package com.veganbeauty.app.features.account.expiry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.databinding.AccountBottomSheetExpiryFilterBinding;

public class ExpiryFilterBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ExpiryFilterBottomSheet";

    private AccountBottomSheetExpiryFilterBinding binding;
    private AccountProductExpiryViewModel viewModel;

    public static ExpiryFilterBottomSheet newInstance() {
        return new ExpiryFilterBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountBottomSheetExpiryFilterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Fragment parent = getParentFragment();
        if (parent == null) return;
        
        viewModel = new ViewModelProvider(parent).get(AccountProductExpiryViewModel.class);

        viewModel.getSelectedFilter().observe(getViewLifecycleOwner(), selectedState -> {
            binding.ivCheckAll.setVisibility(selectedState == ExpiryFilterState.ALL ? View.VISIBLE : View.GONE);
            binding.ivCheckExpired.setVisibility(selectedState == ExpiryFilterState.EXPIRED ? View.VISIBLE : View.GONE);
            binding.ivCheckSoon.setVisibility(selectedState == ExpiryFilterState.SOON ? View.VISIBLE : View.GONE);
            binding.ivCheckValid.setVisibility(selectedState == ExpiryFilterState.VALID ? View.VISIBLE : View.GONE);
        });

        binding.btnFilterAll.setOnClickListener(v -> {
            viewModel.setSelectedFilter(ExpiryFilterState.ALL);
            dismiss();
        });

        binding.btnFilterExpired.setOnClickListener(v -> {
            viewModel.setSelectedFilter(ExpiryFilterState.EXPIRED);
            dismiss();
        });

        binding.btnFilterSoon.setOnClickListener(v -> {
            viewModel.setSelectedFilter(ExpiryFilterState.SOON);
            dismiss();
        });

        binding.btnFilterValid.setOnClickListener(v -> {
            viewModel.setSelectedFilter(ExpiryFilterState.VALID);
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            View bottomSheet = ((BottomSheetDialog) getDialog()).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
