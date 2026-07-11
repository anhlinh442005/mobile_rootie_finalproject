package com.veganbeauty.app.features.routine;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import com.veganbeauty.app.databinding.SkinRoutineSettingsBinding;
import com.veganbeauty.app.features.myskin.SkinDetailHeaderScrollHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SkinRoutineSettingsFragment extends RootieFragment {

    private SkinRoutineSettingsBinding binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;
    private boolean isMorningTabSelected = true;
    private final List<SkincareStep> morningSteps = new ArrayList<>();
    private final List<SkincareStep> eveningSteps = new ArrayList<>();
    private String morningTime = "06:30";
    private String eveningTime = "21:45";
    private boolean isMorningReminderEnabled = false;
    private boolean isEveningReminderEnabled = false;
    private boolean isLeadReminderEnabled = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinRoutineSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        if (binding == null) return;
        Context ctx = requireContext();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnAiSuggestion.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.ai.SkinAiChatFragment())
                        .addToBackStack(null).commit());

        isMorningReminderEnabled = ProfileSession.isMorningReminderEnabled(ctx);
        isEveningReminderEnabled = ProfileSession.isEveningReminderEnabled(ctx);
        isLeadReminderEnabled = ProfileSession.isLeadReminderEnabled(ctx);

        updateSwitchUI(binding.switchMorningReminderContainer, binding.switchMorningReminderThumb, isMorningReminderEnabled);
        updateSwitchUI(binding.switchEveningReminderContainer, binding.switchEveningReminderThumb, isEveningReminderEnabled);
        updateSwitchUI(binding.switchLeadReminderContainer, binding.switchLeadReminderThumb, isLeadReminderEnabled);

        binding.switchMorningReminderContainer.setOnClickListener(v -> {
            isMorningReminderEnabled = !isMorningReminderEnabled;
            updateSwitchUI(binding.switchMorningReminderContainer, binding.switchMorningReminderThumb, isMorningReminderEnabled);
        });
        binding.switchEveningReminderContainer.setOnClickListener(v -> {
            isEveningReminderEnabled = !isEveningReminderEnabled;
            updateSwitchUI(binding.switchEveningReminderContainer, binding.switchEveningReminderThumb, isEveningReminderEnabled);
        });
        binding.switchLeadReminderContainer.setOnClickListener(v -> {
            isLeadReminderEnabled = !isLeadReminderEnabled;
            updateSwitchUI(binding.switchLeadReminderContainer, binding.switchLeadReminderThumb, isLeadReminderEnabled);
        });

        morningTime = ProfileSession.getMorningReminderTime(ctx);
        eveningTime = ProfileSession.getEveningReminderTime(ctx);

        String[] mParts = morningTime.split(":");
        int mHour = 6, mMin = 30;
        try { if(mParts.length>0) mHour = Integer.parseInt(mParts[0]); if(mParts.length>1) mMin = Integer.parseInt(mParts[1]); } catch(Exception ignored){}
        binding.tvMorningTime.setText(formatDisplayTime(mHour, mMin));

        String[] eParts = eveningTime.split(":");
        int eHour = 21, eMin = 45;
        try { if(eParts.length>0) eHour = Integer.parseInt(eParts[0]); if(eParts.length>1) eMin = Integer.parseInt(eParts[1]); } catch(Exception ignored){}
        binding.tvEveningTime.setText(formatDisplayTime(eHour, eMin));

        binding.tvMorningTime.setOnClickListener(v -> showTimePickerDialog(true));
        binding.tvEveningTime.setOnClickListener(v -> showTimePickerDialog(false));

        loadStepsFromPreferences();
        populateStepsList();

        binding.tabMorning.setOnClickListener(v -> {
            if (!isMorningTabSelected) {
                isMorningTabSelected = true;
                binding.tabMorning.setBackgroundResource(R.drawable.skin_bg_tab_selected);
                binding.tabMorning.setTextColor(Color.WHITE);
                binding.tabEvening.setBackgroundColor(Color.TRANSPARENT);
                binding.tabEvening.setTextColor(Color.parseColor("#3E4D44"));
                populateStepsList();
            }
        });

        binding.tabEvening.setOnClickListener(v -> {
            if (isMorningTabSelected) {
                isMorningTabSelected = false;
                binding.tabEvening.setBackgroundResource(R.drawable.skin_bg_tab_selected);
                binding.tabEvening.setTextColor(Color.WHITE);
                binding.tabMorning.setBackgroundColor(Color.TRANSPARENT);
                binding.tabMorning.setTextColor(Color.parseColor("#3E4D44"));
                populateStepsList();
            }
        });

        binding.btnAddStep.setOnClickListener(v -> showAddStepDialog());
        binding.btnSaveConfig.setOnClickListener(v -> saveConfiguration());
        setupScrollHideHeader();
    }

    private void setupScrollHideHeader() {
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                binding.layoutHeader,
                binding.settingsScroll,
                0
        );
        headerScrollHelper.attachToNestedScrollView(binding.settingsScroll);
    }

    private void updateSwitchUI(ViewGroup container, ImageView thumb, boolean enabled) {
        int margin = (int) (2 * getResources().getDisplayMetrics().density);
        if (enabled) {
            container.setBackgroundResource(R.drawable.ic_switch_track_on);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            lp.setMarginStart(0); lp.setMarginEnd(margin);
            thumb.setLayoutParams(lp);
        } else {
            container.setBackgroundResource(R.drawable.ic_switch_track_off);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) thumb.getLayoutParams();
            lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            lp.setMarginEnd(0); lp.setMarginStart(margin);
            thumb.setLayoutParams(lp);
        }
    }

    private void loadStepsFromPreferences() {
        Context ctx = requireContext();
        Set<String> morningRaw = ProfileSession.getMorningSteps(ctx);
        Set<String> eveningRaw = ProfileSession.getEveningSteps(ctx);

        morningSteps.clear();
        morningSteps.addAll(parseRawSteps(morningRaw));
        morningSteps.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));

        eveningSteps.clear();
        eveningSteps.addAll(parseRawSteps(eveningRaw));
        eveningSteps.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
    }

    private List<SkincareStep> parseRawSteps(Set<String> rawSet) {
        List<SkincareStep> list = new ArrayList<>();
        for (String raw : rawSet) {
            String[] parts = raw.split(":");
            if (parts.length >= 4) {
                try {
                    int index = Integer.parseInt(parts[0]);
                    list.add(new SkincareStep(index, parts[1], parts[2], Boolean.parseBoolean(parts[3])));
                } catch (Exception ignored) {}
            } else if (parts.length == 3) {
                list.add(new SkincareStep(99, parts[0], parts[1], Boolean.parseBoolean(parts[2])));
            }
        }
        return list;
    }

    private void populateStepsList() {
        binding.layoutSkincareSteps.removeAllViews();
        List<SkincareStep> steps = isMorningTabSelected ? morningSteps : eveningSteps;
        for (int i = 0; i < steps.size(); i++) {
            final int idx = i;
            SkincareStep step = steps.get(i);
            View stepView = LayoutInflater.from(requireContext()).inflate(R.layout.item_routine_step, binding.layoutSkincareSteps, false);

            TextView tvName = stepView.findViewById(R.id.tvStepName);
            TextView tvDesc = stepView.findViewById(R.id.tvStepDesc);
            ImageView ivCheckbox = stepView.findViewById(R.id.ivStepCheckbox);
            View layoutStepCard = stepView.findViewById(R.id.layoutStepCard);

            tvName.setText(step.getName());
            tvDesc.setText(step.getDescription());

            updateCheckboxUI(ivCheckbox, step.isChecked());

            ivCheckbox.setOnClickListener(v -> {
                step.setChecked(!step.isChecked());
                updateCheckboxUI(ivCheckbox, step.isChecked());
            });

            layoutStepCard.setOnClickListener(v -> showEditStepDialog(step, idx));

            binding.layoutSkincareSteps.addView(stepView);
        }
    }

    private void updateCheckboxUI(ImageView ivCheckbox, boolean isChecked) {
        if (isChecked) {
            ivCheckbox.setImageResource(R.drawable.ic_checkbox_checked);
            ivCheckbox.setImageTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
            ivCheckbox.setBackground(null);
            ivCheckbox.setPadding(0, 0, 0, 0);
        } else {
            ivCheckbox.setImageResource(R.drawable.ic_checkbox_unchecked);
            ivCheckbox.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
            ivCheckbox.setBackground(null);
            ivCheckbox.setPadding(0, 0, 0, 0);
        }
    }

    private void showTimePickerDialog(boolean isMorning) {
        String currentTime = isMorning ? morningTime : eveningTime;
        String[] parts = currentTime.split(":");
        int currentHour = isMorning ? 6 : 21, currentMinute = isMorning ? 30 : 45;
        try { if(parts.length>0) currentHour=Integer.parseInt(parts[0]); if(parts.length>1) currentMinute=Integer.parseInt(parts[1]); } catch(Exception ignored){}

        new TimePickerDialog(requireContext(), (view, hourOfDay, minute) -> {
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            String displayTime = formatDisplayTime(hourOfDay, minute);
            if (isMorning) {
                morningTime = formattedTime;
                binding.tvMorningTime.setText(displayTime);
            } else {
                eveningTime = formattedTime;
                binding.tvEveningTime.setText(displayTime);
            }
        }, currentHour, currentMinute, false).show();
    }

    private String formatDisplayTime(int hour, int minute) {
        String amPm = hour < 12 ? "AM" : "PM";
        int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
        return String.format(Locale.getDefault(), "%02d : %02d %s", displayHour, minute, amPm);
    }

    private void showAddStepDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_step, null);
        TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        EditText etName = view.findViewById(R.id.etStepName);
        EditText etDesc = view.findViewById(R.id.etStepDesc);
        View btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnConfirm = view.findViewById(R.id.btnConfirm);

        tvDialogTitle.setText("Thêm bước dưỡng da mới");
        btnConfirm.setText("Thêm");

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            if (!name.isEmpty() && !desc.isEmpty()) {
                List<SkincareStep> list = isMorningTabSelected ? morningSteps : eveningSteps;
                int newIndex = 0;
                for (SkincareStep s : list) if (s.getIndex() >= newIndex) newIndex = s.getIndex() + 1;
                list.add(new SkincareStep(newIndex, name, desc, true));
                populateStepsList();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void showEditStepDialog(SkincareStep step, int indexInList) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_step, null);
        TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        EditText etName = view.findViewById(R.id.etStepName);
        EditText etDesc = view.findViewById(R.id.etStepDesc);
        View btnDelete = view.findViewById(R.id.btnDelete);
        View btnCancel = view.findViewById(R.id.btnCancel);
        TextView btnConfirm = view.findViewById(R.id.btnConfirm);

        tvDialogTitle.setText("Chỉnh sửa bước dưỡng da");
        btnConfirm.setText("Lưu");
        btnDelete.setVisibility(View.VISIBLE);
        etName.setText(step.getName());
        etDesc.setText(step.getDescription());

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            List<SkincareStep> list = isMorningTabSelected ? morningSteps : eveningSteps;
            list.remove(indexInList);
            populateStepsList();
            dialog.dismiss();
        });
        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            if (!name.isEmpty() && !desc.isEmpty()) {
                step.setName(name);
                step.setDescription(desc);
                populateStepsList();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void saveConfiguration() {
        Context ctx = requireContext();

        if (!ProfileSession.isNotiEnabled(ctx)
                || !com.veganbeauty.app.features.account.notification.LocalSystemNotificationHelper.canPost(ctx)) {
            new AlertDialog.Builder(ctx)
                    .setTitle("Chưa bật thông báo trên máy")
                    .setMessage("Toggle trong app chưa đủ. Hãy bật thông báo Rootie trong Cài đặt hệ thống (và cho phép báo thức/nhắc giờ nếu máy hỏi), rồi lưu lại.")
                    .setPositiveButton("Mở cài đặt", (d, w) ->
                            com.veganbeauty.app.features.account.notification.NotificationScheduleHelper.openAppSettings(ctx))
                    .setNegativeButton("Để sau", null)
                    .show();
            return;
        }

        ProfileSession.setMorningReminderEnabled(ctx, isMorningReminderEnabled);
        ProfileSession.setEveningReminderEnabled(ctx, isEveningReminderEnabled);
        ProfileSession.setLeadReminderEnabled(ctx, isLeadReminderEnabled);

        ProfileSession.setMorningReminderTime(ctx, morningTime);
        ProfileSession.setEveningReminderTime(ctx, eveningTime);

        Set<String> morningRaw = new HashSet<>();
        for (SkincareStep s : morningSteps) morningRaw.add(s.getIndex() + ":" + s.getName() + ":" + s.getDescription() + ":" + s.isChecked());
        Set<String> eveningRaw = new HashSet<>();
        for (SkincareStep s : eveningSteps) eveningRaw.add(s.getIndex() + ":" + s.getName() + ":" + s.getDescription() + ":" + s.isChecked());

        ProfileSession.setMorningSteps(ctx, morningRaw);
        ProfileSession.setEveningSteps(ctx, eveningRaw);

        com.veganbeauty.app.features.account.notification.NotificationScheduleHelper.remindExactAlarmIfNeeded(ctx);
        RoutineAlarmScheduler.rescheduleAlarms(ctx);

        String nextHint = buildNextReminderHint();
        Toast.makeText(ctx, "Đã lưu. " + nextHint, Toast.LENGTH_LONG).show();
        getParentFragmentManager().popBackStack();
    }

    private String buildNextReminderHint() {
        if (!isMorningReminderEnabled && !isEveningReminderEnabled) {
            return "Bạn đang tắt hết nhắc routine.";
        }
        StringBuilder sb = new StringBuilder("Nhắc sẽ tới đúng giờ đã chọn");
        if (isMorningReminderEnabled) sb.append(" · sáng ").append(morningTime);
        if (isEveningReminderEnabled) sb.append(" · tối ").append(eveningTime);
        sb.append(" (không gửi ngay lúc lưu).");
        return sb.toString();
    }

    @Override
    public void observeViewModel() {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
