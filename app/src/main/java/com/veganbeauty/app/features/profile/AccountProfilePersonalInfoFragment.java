package com.veganbeauty.app.features.profile;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    private boolean isSaving;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountProfilePersonalInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        Context context = requireContext();
        loadFieldsFromSession(context);

        binding.etAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                int length = s != null ? s.length() : 0;
                binding.tvAddressCount.setText(length + "/200");
            }
        });

        binding.btnBack.setOnClickListener(v -> {
            if (!isSaving) {
                getParentFragmentManager().popBackStack();
            }
        });

        binding.btnSave.setOnClickListener(v -> savePersonalInfo(false));

        com.veganbeauty.app.features.home.BottomNavHelper.highlightTab(view, R.id.nav_account);

        binding.layoutNotification.getRoot().setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                        .addToBackStack(null)
                        .commit());

        SyncDataHelper.syncUserProfileFromFirestore(context, () -> {
            if (binding != null && isAdded()) {
                loadFieldsFromSession(requireContext());
            }
        });
    }

    private void loadFieldsFromSession(Context context) {
        String fullName = ProfileSession.getFullName(context);
        String cccd = ProfileSession.getCCCD(context);
        String address = ProfileSession.getAddress(context);

        binding.etFullname.setText(fullName != null ? fullName : "");
        binding.etCccd.setText(cccd != null ? cccd : "");
        binding.etAddress.setText(address != null ? address : "");
        binding.tvAddressCount.setText((address != null ? address.length() : 0) + "/200");
    }

    private void savePersonalInfo(boolean exitAfterSave) {
        if (isSaving || binding == null) {
            return;
        }

        Context context = requireContext();
        String fullName = binding.etFullname.getText().toString().trim();
        String cccd = binding.etCccd.getText().toString().trim();
        String address = binding.etAddress.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(context, "Họ và tên không được để trống", Toast.LENGTH_SHORT).show();
            binding.etFullname.requestFocus();
            return;
        }
        if (cccd.isEmpty()) {
            Toast.makeText(context, "Vui lòng nhập số CCCD", Toast.LENGTH_SHORT).show();
            binding.etCccd.requestFocus();
            return;
        }
        if (address.isEmpty()) {
            Toast.makeText(context, "Vui lòng nhập địa chỉ", Toast.LENGTH_SHORT).show();
            binding.etAddress.requestFocus();
            return;
        }

        ProfileSession.setFullName(context, fullName);
        ProfileSession.setCCCD(context, cccd);
        ProfileSession.setAddress(context, address);
        ProfileSession.markLocalProfileEdited(context);

        isSaving = true;
        binding.btnSave.setEnabled(false);
        Toast.makeText(context, "Đang lưu thông tin...", Toast.LENGTH_SHORT).show();

        SyncDataHelper.syncUserProfileToFirebaseAndLocal(context, (localSuccess, cloudSynced) -> {
            isSaving = false;
            if (binding == null || !isAdded()) {
                return;
            }
            binding.btnSave.setEnabled(true);
            if (!localSuccess) {
                Toast.makeText(context, "Không thể lưu thông tin. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                return;
            }
            if (!cloudSynced) {
                Toast.makeText(context, "Đã lưu trên máy. Đồng bộ cloud sẽ thử lại khi có mạng.", Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(context, "Đã lưu thông tin cá nhân", Toast.LENGTH_SHORT).show();
            if (exitAfterSave) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
