package com.veganbeauty.app.features.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.databinding.ChangePasswordSheetBinding;

public class AccountChangePasswordSheet extends BottomSheetDialogFragment {

    private ChangePasswordSheetBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ChangePasswordSheetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.fpBtnChooseEmail.setOnClickListener(v -> {
            binding.fpStep0Container.setVisibility(View.GONE);
            binding.fpStep1Container.setVisibility(View.VISIBLE);
        });

        binding.fpBtnChoosePhone.setOnClickListener(v -> {
            binding.fpStep0Container.setVisibility(View.GONE);
            binding.fpStep1Container.setVisibility(View.VISIBLE);
        });

        binding.fpBtnSendOtp.setOnClickListener(v -> {
            binding.fpStep1Container.setVisibility(View.GONE);
            binding.fpStep2Container.setVisibility(View.VISIBLE);
        });

        binding.fpBtnVerifyOtp.setOnClickListener(v -> {
            binding.fpStep2Container.setVisibility(View.GONE);
            binding.fpStep3Container.setVisibility(View.VISIBLE);
        });

        binding.fpBtnResetPassword.setOnClickListener(v -> {
            binding.fpStep3Container.setVisibility(View.GONE);
            binding.fpStep4Container.setVisibility(View.VISIBLE);
        });

        binding.fpBtnGoLogin.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
