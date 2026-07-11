package com.veganbeauty.app.features.profile;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.databinding.AccountProfileAddressBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.myskin.SkinDetailHeaderScrollHelper;
import com.veganbeauty.app.utils.AddressBookHelper;

import java.util.List;

public class AccountProfileAddressFragment extends RootieFragment {

    private AccountProfileAddressBinding binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountProfileAddressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        Context context = requireContext();
        AddressBookHelper.ensureLoadedForCurrentUser(context);

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnAddAddress.setOnClickListener(v -> showAddressDialog(null));

        loadAddressData();
        BottomNavHelper.setup(this, view, R.id.nav_account, tabId -> BottomNavHelper.navigate(this, tabId));
        setupScrollHideHeader();
    }

    private void setupScrollHideHeader() {
        float density = getResources().getDisplayMetrics().density;
        int bottomPadding = (int) (getResources().getDimension(R.dimen.home_nav_bar_height) + (76f * density));
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                binding.rlHeader,
                binding.scrollContent,
                bottomPadding
        );
        headerScrollHelper.attachToNestedScrollView(binding.scrollContent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            AddressBookHelper.ensureLoadedForCurrentUser(requireContext());
            loadAddressData();
        }
    }

    private void loadAddressData() {
        Context context = requireContext();
        List<AddressBookHelper.AddressEntry> list = AddressBookHelper.getSessionAddresses(context);
        LinearLayout container = binding.llAddressList;
        container.removeAllViews();

        if (list.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            container.setVisibility(View.GONE);
            return;
        }

        binding.layoutEmpty.setVisibility(View.GONE);
        container.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(context);
        for (AddressBookHelper.AddressEntry entry : list) {
            View item = inflater.inflate(R.layout.account_item_address, container, false);
            TextView tvLabel = item.findViewById(R.id.tv_label);
            TextView badgeDefault = item.findViewById(R.id.badge_default);
            TextView tvName = item.findViewById(R.id.tv_name);
            TextView tvPhone = item.findViewById(R.id.tv_phone);
            TextView tvAddress = item.findViewById(R.id.tv_address);
            ImageView ivIcon = item.findViewById(R.id.iv_label_icon);
            FrameLayout btnEdit = item.findViewById(R.id.btn_edit);
            FrameLayout btnDelete = item.findViewById(R.id.btn_delete);

            tvLabel.setText(entry.label);
            tvName.setText(entry.name);
            tvPhone.setText(entry.phone);
            tvAddress.setText(entry.address);
            applyLabelIcon(ivIcon, entry.label);

            if (entry.isDefault) {
                badgeDefault.setVisibility(View.VISIBLE);
                badgeDefault.setBackgroundResource(R.drawable.bg_dialog_btn_confirm);
                badgeDefault.setTextColor(Color.parseColor("#D9D9D9"));
            } else {
                badgeDefault.setVisibility(View.VISIBLE);
                badgeDefault.setBackgroundResource(R.drawable.tab_inactive_bg);
                badgeDefault.setTextColor(Color.parseColor("#000000"));
                badgeDefault.setOnClickListener(v -> {
                    AddressBookHelper.setDefaultById(context, entry.id);
                    loadAddressData();
                    Toast.makeText(context, "Đã đặt \"" + entry.label + "\" làm địa chỉ mặc định", Toast.LENGTH_SHORT).show();
                });
            }

            btnEdit.setOnClickListener(v -> showAddressDialog(entry));
            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(context)
                    .setTitle("Xóa địa chỉ")
                    .setMessage("Bạn có chắc muốn xóa địa chỉ \"" + entry.label + "\"?")
                    .setPositiveButton("Xóa", (d, w) -> {
                        AddressBookHelper.deleteById(context, entry.id);
                        loadAddressData();
                        Toast.makeText(context, "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show());

            container.addView(item);
        }
    }

    private void applyLabelIcon(ImageView iv, String label) {
        String lower = label != null ? label.toLowerCase() : "";
        if (lower.contains("nhà") || lower.contains("home")) {
            iv.setImageResource(R.drawable.ic_home);
        } else if (lower.contains("văn phòng") || lower.contains("office") || lower.contains("công ty")) {
            iv.setImageResource(R.drawable.ic_address_office);
        } else {
            iv.setImageResource(R.drawable.ic_address_location_pin);
        }
    }

    private void showAddressDialog(@Nullable AddressBookHelper.AddressEntry existing) {
        Context context = requireContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.account_dialog_edit_address, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        ChipGroup chipGroup = dialogView.findViewById(R.id.chip_group_label);
        EditText etLabel = dialogView.findViewById(R.id.et_dialog_label);
        EditText etName = dialogView.findViewById(R.id.et_dialog_name);
        EditText etPhone = dialogView.findViewById(R.id.et_dialog_phone);
        EditText etAddress = dialogView.findViewById(R.id.et_dialog_address);
        View btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        View btnSave = dialogView.findViewById(R.id.btn_dialog_save);

        boolean isEdit = existing != null;
        tvTitle.setText(isEdit ? "Chỉnh sửa địa chỉ" : "Thêm địa chỉ mới");

        String currentLabel = isEdit ? existing.label : "Nhà riêng";
        setupLabelChips(chipGroup, etLabel, currentLabel);

        if (isEdit) {
            etName.setText(existing.name);
            etPhone.setText(existing.phone);
            etAddress.setText(existing.address);
        } else {
            String sessionName = com.veganbeauty.app.data.local.ProfileSession.getFullName(context);
            String sessionPhone = com.veganbeauty.app.data.local.ProfileSession.getPhone(context);
            if (sessionName != null && !sessionName.trim().isEmpty()) etName.setText(sessionName.trim());
            if (sessionPhone != null && !sessionPhone.trim().isEmpty()) etPhone.setText(sessionPhone.trim());
        }

        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String label = resolveSelectedLabel(chipGroup, etLabel);
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String addr = etAddress.getText().toString().trim();
            if (label.isEmpty() || name.isEmpty() || phone.isEmpty() || addr.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isEdit) {
                existing.label = label;
                existing.name = name;
                existing.phone = phone;
                existing.address = addr;
                AddressBookHelper.updateAddress(context, existing);
                Toast.makeText(context, "Đã cập nhật địa chỉ", Toast.LENGTH_SHORT).show();
            } else {
                AddressBookHelper.addAddress(context, label, name, phone, addr, false);
                Toast.makeText(context, "Đã thêm địa chỉ mới", Toast.LENGTH_SHORT).show();
            }
            loadAddressData();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupLabelChips(ChipGroup chipGroup, EditText etCustom, String currentLabel) {
        chipGroup.removeAllViews();
        int matchedChipId = View.NO_ID;
        for (String suggested : AddressBookHelper.SUGGESTED_LABELS) {
            Chip chip = new Chip(requireContext());
            chip.setText(suggested);
            chip.setCheckable(true);
            chip.setClickable(true);
            chipGroup.addView(chip);
            if (suggested.equalsIgnoreCase(currentLabel)) {
                matchedChipId = chip.getId();
            }
        }
        if (matchedChipId != View.NO_ID) {
            chipGroup.check(matchedChipId);
            etCustom.setVisibility(View.GONE);
        } else {
            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                Chip chip = (Chip) chipGroup.getChildAt(i);
                if ("Khác".equals(chip.getText().toString())) {
                    chipGroup.check(chip.getId());
                    break;
                }
            }
            etCustom.setVisibility(View.VISIBLE);
            etCustom.setText(currentLabel);
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip selected = group.findViewById(checkedIds.get(0));
            if (selected != null && "Khác".equals(selected.getText().toString())) {
                etCustom.setVisibility(View.VISIBLE);
                etCustom.requestFocus();
            } else {
                etCustom.setVisibility(View.GONE);
            }
        });
    }

    @NonNull
    private String resolveSelectedLabel(ChipGroup chipGroup, EditText etCustom) {
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = chipGroup.findViewById(checkedId);
            if (chip != null) {
                String text = chip.getText().toString();
                if ("Khác".equals(text)) {
                    String custom = etCustom.getText().toString().trim();
                    return custom.isEmpty() ? "Khác" : custom;
                }
                return text;
            }
        }
        String custom = etCustom.getText().toString().trim();
        return custom.isEmpty() ? "Địa chỉ" : custom;
    }

    @Override
    public void observeViewModel() {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
