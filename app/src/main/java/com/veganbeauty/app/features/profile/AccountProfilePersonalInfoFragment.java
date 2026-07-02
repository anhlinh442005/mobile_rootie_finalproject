package com.veganbeauty.app.features.profile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.AccountProfilePersonalInfoBinding;
import com.veganbeauty.app.utils.SyncDataHelper;

public class AccountProfilePersonalInfoFragment extends RootieFragment {

    private AccountProfilePersonalInfoBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountProfilePersonalInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        Context context = requireContext();
        String fullName = ProfileSession.getFullName(context);
        String cccd = ProfileSession.getCCCD(context);
        String address = ProfileSession.getAddress(context);

        binding.etFullname.setText(maskFullName(fullName));
        binding.etCccd.setText(maskCCCD(cccd));
        binding.etAddress.setText(maskAddress(address));
        binding.tvAddressCount.setText(address.length() + "/200");

        binding.etFullname.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String current = binding.etFullname.getText().toString();
                if (current.contains("*")) {
                    binding.etFullname.setText(ProfileSession.getFullName(context));
                }
            } else {
                String entered = binding.etFullname.getText().toString();
                if (!entered.contains("*") && !entered.trim().isEmpty()) {
                    ProfileSession.setFullName(context, entered);
                }
                binding.etFullname.setText(maskFullName(ProfileSession.getFullName(context)));
            }
        });

        binding.etCccd.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String current = binding.etCccd.getText().toString();
                if (current.contains("*")) {
                    binding.etCccd.setText(ProfileSession.getCCCD(context));
                }
            } else {
                String entered = binding.etCccd.getText().toString();
                if (!entered.contains("*") && !entered.trim().isEmpty()) {
                    ProfileSession.setCCCD(context, entered);
                }
                binding.etCccd.setText(maskCCCD(ProfileSession.getCCCD(context)));
            }
        });

        binding.etAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String currentText = s != null ? s.toString() : "";
                if (binding.etAddress.hasFocus() && !currentText.contains("*")) {
                    binding.tvAddressCount.setText(currentText.length() + "/200");
                } else {
                    String realAddr = ProfileSession.getAddress(context);
                    binding.tvAddressCount.setText(realAddr.length() + "/200");
                }
            }
        });

        binding.etAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String current = binding.etAddress.getText().toString();
                if (current.contains("*")) {
                    binding.etAddress.setText(ProfileSession.getAddress(context));
                }
            } else {
                String entered = binding.etAddress.getText().toString();
                if (!entered.contains("*") && !entered.trim().isEmpty()) {
                    ProfileSession.setAddress(context, entered);
                }
                binding.etAddress.setText(maskAddress(ProfileSession.getAddress(context)));
                binding.tvAddressCount.setText(ProfileSession.getAddress(context).length() + "/200");
            }
        });

        binding.btnBack.setOnClickListener(v -> {
            binding.etFullname.clearFocus();
            binding.etCccd.clearFocus();
            binding.etAddress.clearFocus();
            SyncDataHelper.syncUserProfileToFirebaseAndLocal(requireContext());
            getParentFragmentManager().popBackStack();
        });

        com.veganbeauty.app.features.home.BottomNavHelper.highlightTab(view, R.id.nav_account);

        binding.btnNotification.setOnClickListener(v -> 
            Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    private String maskFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "";
        char firstChar = Character.toUpperCase(fullName.charAt(0));
        char lastChar = Character.toUpperCase(fullName.charAt(fullName.length() - 1));
        return firstChar + "*** **** ***" + lastChar;
    }

    private String maskCCCD(String cccd) {
        if (cccd == null) return "";
        if (cccd.length() < 4) return cccd;
        return "*********" + cccd.substring(cccd.length() - 4);
    }

    private String maskAddress(String address) {
        if (address == null || address.trim().isEmpty()) return "";
        return "********";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
