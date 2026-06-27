package com.veganbeauty.app.features.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.AccountProfileAddressBinding;

public class AccountProfileAddressFragment extends RootieFragment {

    private AccountProfileAddressBinding binding;

    private static final String PREFS_NAME = "rootie_profile_prefs";
    
    private static final String KEY_HOME_NAME = "addr_home_name";
    private static final String KEY_HOME_PHONE = "addr_home_phone";
    private static final String KEY_HOME_ADDR = "addr_home_addr";
    
    private static final String KEY_OFFICE_NAME = "addr_office_name";
    private static final String KEY_OFFICE_PHONE = "addr_office_phone";
    private static final String KEY_OFFICE_ADDR = "addr_office_addr";
    
    private static final String KEY_DEFAULT_TYPE = "addr_default_type"; // "HOME" or "OFFICE"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountProfileAddressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        Context context = requireContext();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.btnNotification.setOnClickListener(v -> Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show());

        loadAddressData();

        binding.badgeHomeDefault.setOnClickListener(v -> setDefaultAddress("HOME"));
        binding.badgeOfficeDefault.setOnClickListener(v -> setDefaultAddress("OFFICE"));

        binding.btnHomeEdit.setOnClickListener(v -> {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String currentName = prefs.getString(KEY_HOME_NAME, "Ánh Linh");
            String currentPhone = prefs.getString(KEY_HOME_PHONE, "0999 999 999");
            String currentAddr = prefs.getString(KEY_HOME_ADDR, "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh");
            
            showEditDialog("Nhà riêng", currentName, currentPhone, currentAddr, (name, phone, address) -> {
                prefs.edit()
                        .putString(KEY_HOME_NAME, name)
                        .putString(KEY_HOME_PHONE, phone)
                        .putString(KEY_HOME_ADDR, address)
                        .apply();
                if ("HOME".equals(prefs.getString(KEY_DEFAULT_TYPE, "HOME"))) {
                    ProfileSession.INSTANCE.setAddress(context, address);
                }
                loadAddressData();
                Toast.makeText(context, "Đã cập nhật địa chỉ Nhà riêng", Toast.LENGTH_SHORT).show();
            });
        });

        binding.btnOfficeEdit.setOnClickListener(v -> {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String currentName = prefs.getString(KEY_OFFICE_NAME, "Khánh Xuân");
            String currentPhone = prefs.getString(KEY_OFFICE_PHONE, "0868 888 888");
            String currentAddr = prefs.getString(KEY_OFFICE_ADDR, "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh");
            
            showEditDialog("Văn phòng", currentName, currentPhone, currentAddr, (name, phone, address) -> {
                prefs.edit()
                        .putString(KEY_OFFICE_NAME, name)
                        .putString(KEY_OFFICE_PHONE, phone)
                        .putString(KEY_OFFICE_ADDR, address)
                        .apply();
                if ("OFFICE".equals(prefs.getString(KEY_DEFAULT_TYPE, "HOME"))) {
                    ProfileSession.INSTANCE.setAddress(context, address);
                }
                loadAddressData();
                Toast.makeText(context, "Đã cập nhật địa chỉ Văn phòng", Toast.LENGTH_SHORT).show();
            });
        });

        binding.btnHomeDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xóa địa chỉ")
                    .setMessage("Bạn có chắc chắn muốn xóa địa chỉ Nhà riêng?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove(KEY_HOME_NAME);
                        editor.remove(KEY_HOME_PHONE);
                        editor.remove(KEY_HOME_ADDR);
                        if ("HOME".equals(prefs.getString(KEY_DEFAULT_TYPE, "HOME"))) {
                            editor.putString(KEY_DEFAULT_TYPE, "OFFICE");
                            String officeAddr = prefs.getString(KEY_OFFICE_ADDR, "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh");
                            if (officeAddr != null) {
                                ProfileSession.INSTANCE.setAddress(context, officeAddr);
                            }
                        }
                        editor.apply();
                        loadAddressData();
                        Toast.makeText(context, "Đã xóa địa chỉ Nhà riêng", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        binding.btnOfficeDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xóa địa chỉ")
                    .setMessage("Bạn có chắc chắn muốn xóa địa chỉ Văn phòng?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove(KEY_OFFICE_NAME);
                        editor.remove(KEY_OFFICE_PHONE);
                        editor.remove(KEY_OFFICE_ADDR);
                        if ("OFFICE".equals(prefs.getString(KEY_DEFAULT_TYPE, "HOME"))) {
                            editor.putString(KEY_DEFAULT_TYPE, "HOME");
                            String homeAddr = prefs.getString(KEY_HOME_ADDR, "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh");
                            if (homeAddr != null) {
                                ProfileSession.INSTANCE.setAddress(context, homeAddr);
                            }
                        }
                        editor.apply();
                        loadAddressData();
                        Toast.makeText(context, "Đã xóa địa chỉ Văn phòng", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        binding.btnAddAddress.setOnClickListener(v -> {
            showEditDialog("Mới", "", "", "", (name, phone, address) -> {
                Toast.makeText(context, "Đã thêm địa chỉ mới thành công!", Toast.LENGTH_LONG).show();
            });
        });

        ViewGroup navAccount = view.findViewById(R.id.nav_account);
        if (navAccount != null) {
            if (navAccount.getChildAt(0) instanceof ImageView) {
                ((ImageView) navAccount.getChildAt(0)).setColorFilter(Color.parseColor("#677559"));
            }
            if (navAccount.getChildAt(1) instanceof TextView) {
                TextView label = (TextView) navAccount.getChildAt(1);
                label.setTextColor(Color.parseColor("#677559"));
                label.setTypeface(null, Typeface.BOLD);
            }
        }
    }

    private void loadAddressData() {
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String homeName = prefs.getString(KEY_HOME_NAME, "Ánh Linh");
        String homePhone = prefs.getString(KEY_HOME_PHONE, "0999 999 999");
        String homeAddr = prefs.getString(KEY_HOME_ADDR, "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh");

        String officeName = prefs.getString(KEY_OFFICE_NAME, "Khánh Xuân");
        String officePhone = prefs.getString(KEY_OFFICE_PHONE, "0868 888 888");
        String officeAddr = prefs.getString(KEY_OFFICE_ADDR, "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh");

        String defaultType = prefs.getString(KEY_DEFAULT_TYPE, "HOME");

        binding.tvHomeName.setText(homeName);
        binding.tvHomePhone.setText(homePhone);
        binding.tvHomeAddress.setText(homeAddr);

        binding.tvOfficeName.setText(officeName);
        binding.tvOfficePhone.setText(officePhone);
        binding.tvOfficeAddress.setText(officeAddr);

        if ("HOME".equals(defaultType)) {
            binding.badgeHomeDefault.setBackgroundResource(R.drawable.bg_badge_default_active);
            binding.badgeHomeDefault.setTextColor(Color.parseColor("#D9D9D9"));
            
            binding.badgeOfficeDefault.setBackgroundResource(R.drawable.bg_badge_default_inactive);
            binding.badgeOfficeDefault.setTextColor(Color.parseColor("#000000"));
        } else {
            binding.badgeOfficeDefault.setBackgroundResource(R.drawable.bg_badge_default_active);
            binding.badgeOfficeDefault.setTextColor(Color.parseColor("#D9D9D9"));
            
            binding.badgeHomeDefault.setBackgroundResource(R.drawable.bg_badge_default_inactive);
            binding.badgeHomeDefault.setTextColor(Color.parseColor("#000000"));
        }
    }

    private void setDefaultAddress(String type) {
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        prefs.edit().putString(KEY_DEFAULT_TYPE, type).apply();
        
        String defaultAddr = "HOME".equals(type) 
                ? prefs.getString(KEY_HOME_ADDR, "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh")
                : prefs.getString(KEY_OFFICE_ADDR, "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh");
        
        if (defaultAddr != null) {
            ProfileSession.INSTANCE.setAddress(context, defaultAddr);
        }

        loadAddressData();
        String targetName = "HOME".equals(type) ? "Nhà riêng" : "Văn phòng";
        Toast.makeText(context, "Đã đặt " + targetName + " làm địa chỉ mặc định", Toast.LENGTH_SHORT).show();
    }

    private interface OnSaveAddressListener {
        void onSave(String name, String phone, String address);
    }

    private void showEditDialog(String title, String currentName, String currentPhone, String currentAddress, OnSaveAddressListener onSave) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.account_dialog_edit_address, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etName = dialogView.findViewById(R.id.et_dialog_name);
        EditText etPhone = dialogView.findViewById(R.id.et_dialog_phone);
        EditText etAddress = dialogView.findViewById(R.id.et_dialog_address);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_dialog_save);

        tvTitle.setText("Chỉnh sửa địa chỉ: " + title);
        etName.setText(currentName);
        etPhone.setText(currentPhone);
        etAddress.setText(currentAddress);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String addr = etAddress.getText().toString().trim();
            if (!name.isEmpty() && !phone.isEmpty() && !addr.isEmpty()) {
                onSave.onSave(name, phone, addr);
                dialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public void observeViewModel() {
        // Not used
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
