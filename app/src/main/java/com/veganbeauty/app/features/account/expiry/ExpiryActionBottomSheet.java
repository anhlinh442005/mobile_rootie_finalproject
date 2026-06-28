package com.veganbeauty.app.features.account.expiry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.databinding.AccountBottomSheetExpiryActionBinding;

public class ExpiryActionBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ExpiryActionBottomSheet";
    private static final String ARG_NAME = "ARG_NAME";
    private static final String ARG_EXPIRY = "ARG_EXPIRY";
    private static final String ARG_IMAGE = "ARG_IMAGE";

    public interface OnActionClickListener {
        void onBuyAgain();
        void onDelete();
    }

    private AccountBottomSheetExpiryActionBinding binding;
    private String productName = "";
    private String productExpiry = "";
    private String productImage = "";
    private OnActionClickListener actionListener;

    public static ExpiryActionBottomSheet newInstance(String productName, String productExpiry, String productImage) {
        ExpiryActionBottomSheet fragment = new ExpiryActionBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, productName);
        args.putString(ARG_EXPIRY, productExpiry);
        args.putString(ARG_IMAGE, productImage);
        fragment.setArguments(args);
        return fragment;
    }

    public void setActionListener(OnActionClickListener listener) {
        this.actionListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productName = getArguments().getString(ARG_NAME, "");
            productExpiry = getArguments().getString(ARG_EXPIRY, "");
            productImage = getArguments().getString(ARG_IMAGE, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountBottomSheetExpiryActionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvProductName.setText(productName);
        binding.tvProductExpiry.setText("Hạn sử dụng: " + productExpiry);

        com.bumptech.glide.Glide.with(binding.ivProductImage.getContext()).load(productImage).placeholder(android.R.color.darker_gray).into(binding.ivProductImage);

        binding.btnBuyAgain.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onBuyAgain();
            }
            dismiss();
        });

        binding.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete();
            }
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
