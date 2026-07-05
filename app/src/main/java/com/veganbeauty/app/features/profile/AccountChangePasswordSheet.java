package com.veganbeauty.app.features.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.repository.AuthRepository;
import com.veganbeauty.app.databinding.ChangePasswordSheetBinding;

import java.security.MessageDigest;

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

        binding.cpBtnChangePassword.setOnClickListener(v -> handleChangePassword());
        binding.cpBtnDone.setOnClickListener(v -> dismiss());
    }

    private void handleChangePassword() {
        String oldPassword = getText(binding.cpInputOldPassword);
        String newPassword = getText(binding.cpInputNewPassword);
        String confirmPassword = getText(binding.cpInputConfirmPassword);

        if (oldPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập mật khẩu cũ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.length() < 6) {
            Toast.makeText(requireContext(), "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(requireContext(), "Mật khẩu nhập lại không khớp", Toast.LENGTH_SHORT).show();
            return;
        }
        if (oldPassword.equals(newPassword)) {
            Toast.makeText(requireContext(), "Mật khẩu mới phải khác mật khẩu cũ", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.cpBtnChangePassword.setEnabled(false);

        new Thread(() -> {
            String email = ProfileSession.getEmail(requireContext());
            String phone = ProfileSession.getPhone(requireContext());
            String loginId = email != null && !email.isEmpty() ? email : phone;

            try {
                RootieDatabase db = RootieDatabase.getDatabase(requireContext());
                AuthRepository authRepo = new AuthRepository(db.userDao(), requireContext());
                UserEntity user = authRepo.login(loginId, oldPassword);

                if (user == null) {
                    showToast("Mật khẩu cũ không đúng");
                    return;
                }

                String hashedPassword = hashPassword(newPassword);
                int updated;
                if (email != null && !email.isEmpty()) {
                    updated = db.userDao().updatePasswordByEmailSync(email, hashedPassword);
                } else {
                    updated = db.userDao().updatePasswordByPhoneSync(phone, hashedPassword);
                }

                if (updated <= 0) {
                    showToast("Không thể cập nhật mật khẩu. Vui lòng thử lại");
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    binding.cpFormContainer.setVisibility(View.GONE);
                    binding.cpSuccessContainer.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                showToast("Đã xảy ra lỗi. Vui lòng thử lại");
            } finally {
                requireActivity().runOnUiThread(() -> binding.cpBtnChangePassword.setEnabled(true));
            }
        }).start();
    }

    private String getText(com.google.android.material.textfield.TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showToast(String message) {
        requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return password;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
