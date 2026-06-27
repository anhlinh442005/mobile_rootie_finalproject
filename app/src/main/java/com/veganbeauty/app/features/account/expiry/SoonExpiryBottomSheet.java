package com.veganbeauty.app.features.account.expiry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.databinding.AccountBottomSheetSoonExpiryBinding;

public class SoonExpiryBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SoonExpiryBottomSheet";

    private AccountBottomSheetSoonExpiryBinding binding;
    private AccountProductExpiryViewModel viewModel;

    private final AccountProductExpiryAdapter listAdapter = new AccountProductExpiryAdapter(
            AccountProductExpiryAdapter.ExpiryLayoutMode.LIST,
            uiModel -> {
                if (getParentFragment() instanceof AccountProductExpiryFragment) {
                    ((AccountProductExpiryFragment) getParentFragment()).navigateToDetail(uiModel);
                }
                dismiss();
            }
    );

    public static SoonExpiryBottomSheet newInstance() {
        return new SoonExpiryBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountBottomSheetSoonExpiryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvProducts.setAdapter(listAdapter);

        if (getParentFragment() != null) {
            viewModel = new ViewModelProvider(getParentFragment()).get(AccountProductExpiryViewModel.class);
            viewModel.getSoonExpiryProducts().observe(getViewLifecycleOwner(), products -> {
                listAdapter.submitList(products);
            });
        }
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
