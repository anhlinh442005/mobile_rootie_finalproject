package com.veganbeauty.app.features.profile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.AccountProfileSecurityBinding;

public class AccountProfileSecurityFragment extends RootieFragment {

    private AccountProfileSecurityBinding _binding;

    private boolean fastLoginState;
    private boolean floatingChatState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = AccountProfileSecurityBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        Context context = requireContext();

        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        _binding.btnNotification.setOnClickListener(v -> Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show());

        String username = ProfileSession.getUsername(context);
        String phone = ProfileSession.getPhone(context);
        String email = ProfileSession.getEmail(context);
        fastLoginState = ProfileSession.isFastLoginEnabled(context);

        _binding.tvUsernameVal.setText(username);
        _binding.tvPhoneVal.setText(maskPhone(phone));
        _binding.tvEmailVal.setText(maskEmail(email));

        updateSwitchUI(fastLoginState);

        SharedPreferences prefs = context.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
        floatingChatState = prefs.getBoolean("SKIN_AI_FLOATING_CHAT_ENABLED", true);
        updateFloatingSwitchUI(floatingChatState);

        _binding.btnMyProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountProfileEditFragment())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        _binding.btnUsername.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_username, null);
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            EditText etUsername = dialogView.findViewById(R.id.etUsername);
            TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
            TextView btnSave = dialogView.findViewById(R.id.btnSave);

            etUsername.setText(username);

            btnCancel.setOnClickListener(v1 -> dialog.dismiss());

            btnSave.setOnClickListener(v2 -> {
                String newName = etUsername.getText().toString().trim();
                if (!newName.isEmpty()) {
                    _binding.tvUsernameVal.setText(newName);
                    ProfileSession.setUsername(context, newName);
                    Toast.makeText(context, "Đã cập nhật tên người dùng", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            });

            dialog.show();
        });

        _binding.btnPhone.setOnClickListener(v -> Toast.makeText(context, "Số điện thoại: " + phone, Toast.LENGTH_SHORT).show());

        _binding.btnEmail.setOnClickListener(v -> Toast.makeText(context, "Email nhận hóa đơn: " + email, Toast.LENGTH_SHORT).show());

        _binding.btnSocial.setOnClickListener(v -> Toast.makeText(context, "Liên kết mạng xã hội (Đang phát triển)", Toast.LENGTH_SHORT).show());

        _binding.btnChangePassword.setOnClickListener(v -> {
            AccountChangePasswordSheet sheet = new AccountChangePasswordSheet();
            sheet.show(getParentFragmentManager(), "AccountChangePasswordSheet");
        });

        _binding.btnPasskey.setOnClickListener(v -> Toast.makeText(context, "Passkey (Đang phát triển)", Toast.LENGTH_SHORT).show());

        _binding.switchFastLoginContainer.setOnClickListener(v -> {
            fastLoginState = !fastLoginState;
            updateSwitchUI(fastLoginState);
            ProfileSession.setFastLoginEnabled(context, fastLoginState);
            String msg = fastLoginState ? "Đã bật đăng nhập nhanh" : "Đã tắt đăng nhập nhanh";
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        });

        _binding.switchFloatingChatContainer.setOnClickListener(v -> {
            floatingChatState = !floatingChatState;
            updateFloatingSwitchUI(floatingChatState);
            prefs.edit().putBoolean("SKIN_AI_FLOATING_CHAT_ENABLED", floatingChatState).apply();

            if (getActivity() != null) {
                View chatHead = getActivity().findViewById(R.id.skin_ai_floating_chat_head);
                if (chatHead != null) {
                    chatHead.setVisibility(floatingChatState ? View.VISIBLE : View.GONE);
                }
            }

            String msg = floatingChatState ? "Đã bật Trợ lý Rootie AI nổi" : "Đã tắt Trợ lý Rootie AI nổi";
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        });

        _binding.btnCheckActivity.setOnClickListener(v -> Toast.makeText(context, "Kiểm tra hoạt động (Đang phát triển)", Toast.LENGTH_SHORT).show());

        _binding.btnManageDevices.setOnClickListener(v -> Toast.makeText(context, "Quản lý thiết bị đăng nhập (Đang phát triển)", Toast.LENGTH_SHORT).show());

        com.veganbeauty.app.features.home.BottomNavHelper.highlightTab(view, R.id.nav_account);
    }

    private void updateSwitchUI(boolean enabled) {
        if (enabled) {
            _binding.switchFastLoginContainer.setBackgroundResource(R.drawable.ic_switch_track_on);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) _binding.switchFastLoginThumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            lp.setMarginStart(0);
            lp.setMarginEnd((int) (2 * getResources().getDisplayMetrics().density));
            _binding.switchFastLoginThumb.setLayoutParams(lp);
        } else {
            _binding.switchFastLoginContainer.setBackgroundResource(R.drawable.ic_switch_track_off);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) _binding.switchFastLoginThumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            lp.setMarginEnd(0);
            lp.setMarginStart((int) (2 * getResources().getDisplayMetrics().density));
            _binding.switchFastLoginThumb.setLayoutParams(lp);
        }
    }

    private void updateFloatingSwitchUI(boolean enabled) {
        if (enabled) {
            _binding.switchFloatingChatContainer.setBackgroundResource(R.drawable.ic_switch_track_on);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) _binding.switchFloatingChatThumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            lp.setMarginStart(0);
            lp.setMarginEnd((int) (2 * getResources().getDisplayMetrics().density));
            _binding.switchFloatingChatThumb.setLayoutParams(lp);
        } else {
            _binding.switchFloatingChatContainer.setBackgroundResource(R.drawable.ic_switch_track_off);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) _binding.switchFloatingChatThumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            lp.setMarginEnd(0);
            lp.setMarginStart((int) (2 * getResources().getDisplayMetrics().density));
            _binding.switchFloatingChatThumb.setLayoutParams(lp);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 2) return phone;
        return "******" + phone.substring(phone.length() - 2);
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        String[] parts = email.split("@");
        if (parts.length != 2) return email;
        String prefix = parts[0];
        String domain = parts[1];
        if (prefix.length() <= 2) return email;
        StringBuilder maskedPrefix = new StringBuilder();
        maskedPrefix.append(prefix.charAt(0));
        for (int i = 0; i < prefix.length() - 2; i++) {
            maskedPrefix.append("*");
        }
        maskedPrefix.append(prefix.charAt(prefix.length() - 1));
        return maskedPrefix.toString() + "@" + domain;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
