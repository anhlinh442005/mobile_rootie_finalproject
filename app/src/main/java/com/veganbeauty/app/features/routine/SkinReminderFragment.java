package com.veganbeauty.app.features.routine;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.FlowLiveDataConversions;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.databinding.ItemStreakDayBinding;
import com.veganbeauty.app.databinding.SkinReminderBinding;
import com.veganbeauty.app.features.myskin.SkinDetailHeaderScrollHelper;
import com.veganbeauty.app.utils.CoinRewardDialogHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class SkinReminderFragment extends RootieFragment {

    private SkinReminderBinding binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinReminderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        refreshUI();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.btnNotification.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                        .addToBackStack(null)
                        .commit());

        binding.btnStartMorningRoutine.setOnClickListener(v -> {
            SkinTimeRoutineFragment fragment = new SkinTimeRoutineFragment();
            Bundle args = new Bundle();
            args.putString("routine_type", "morning");
            fragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnStartEveningRoutine.setOnClickListener(v -> {
            SkinTimeRoutineFragment fragment = new SkinTimeRoutineFragment();
            Bundle args = new Bundle();
            args.putString("routine_type", "evening");
            fragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.btnSetupRoutine.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new SkinRoutineSettingsFragment())
                        .addToBackStack(null)
                        .commit());

        binding.btnSkincareCalendar.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new SkinCalendarFragment())
                        .addToBackStack(null)
                        .commit());

        binding.btnSocialTask.setOnClickListener(v -> triggerSocialTaskReward());
        setupScrollHideHeader();
    }

    private void setupScrollHideHeader() {
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                binding.topBar,
                binding.reminderScroll,
                0
        );
        headerScrollHelper.attachToNestedScrollView(binding.reminderScroll);
    }

    private void refreshUI() {
        Context ctx = requireContext();

        String avatarUrl = ProfileSession.getAvatar(ctx);
        com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, avatarUrl);

        String fullName = ProfileSession.getFullName(ctx);
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greetingWord;
        if (hour >= 5 && hour <= 10) greetingWord = "Chào buổi sáng";
        else if (hour >= 11 && hour <= 13) greetingWord = "Chào buổi trưa";
        else if (hour >= 14 && hour <= 17) greetingWord = "Chào buổi chiều";
        else greetingWord = "Chào buổi tối";
        binding.tvUserGreeting.setText(greetingWord + ", " + fullName);

        int currentStreak = ProfileSession.getSkinStreak(ctx);
        binding.tvStreakDays.setText(String.valueOf(currentStreak));
        binding.tvStreakDays.setTextColor(Color.parseColor("#FF5722"));

        String motivationMsg;
        if (currentStreak == 0) motivationMsg = "Hãy bắt đầu ngày đầu tiên để chăm sóc làn da của bạn nhé! ✨";
        else if (currentStreak == 1) motivationMsg = "Khởi đầu tuyệt vời! Hãy duy trì chuỗi chăm sóc da ngày mai nhé! \uD83C\uDF31";
        else if (currentStreak >= 2 && currentStreak <= 4) motivationMsg = "Tuyệt vời! Bạn đang dần hình thành thói quen chăm da tốt đó! \uD83D\uDC4F";
        else if (currentStreak >= 5 && currentStreak <= 6) motivationMsg = "Cố lên! Sắp đạt chuỗi 7 ngày để nhận thưởng lớn rồi! \uD83D\uDD25";
        else motivationMsg = "Bạn đang duy trì thói quen rất tốt cho làn da của mình! Rất tự hào về bạn! \uD83C\uDF1F";
        binding.tvStreakMotivation.setText(motivationMsg);

        updateWeekCalendar();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());

        // Morning Logic
        boolean hasCompletedMorningToday = ProfileSession.isMorningRewardAwarded(ctx, todayStr);
        boolean isMorningSubmitted = ProfileSession.isRoutineSubmitted(ctx, "morning", todayStr);

        Set<String> rawMorningSteps = ProfileSession.getMorningSteps(ctx);
        int activeMorningStepsCount = 0;
        for (String raw : rawMorningSteps) {
            String[] parts = raw.split(":");
            if (parts.length >= 4 && Boolean.parseBoolean(parts[3])) activeMorningStepsCount++;
        }

        int completedMorningStepsCount;
        if (hasCompletedMorningToday) {
            completedMorningStepsCount = activeMorningStepsCount;
        } else {
            Set<String> completedStepIds = ProfileSession.getCompletedStepIdsForDate(ctx, todayStr);
            int count = 0;
            for (String raw : rawMorningSteps) {
                String[] parts = raw.split(":");
                if (parts.length >= 4 && Boolean.parseBoolean(parts[3])) {
                    try {
                        int index = Integer.parseInt(parts[0]);
                        if (completedStepIds.contains("morning_" + index)) count++;
                    } catch (Exception ignored) {}
                }
            }
            completedMorningStepsCount = count;
        }

        int morningProgressPercentage = activeMorningStepsCount > 0 ? (completedMorningStepsCount * 100) / activeMorningStepsCount : 0;
        binding.tvMorningProgressText.setText(completedMorningStepsCount + "/" + activeMorningStepsCount + " BƯỚC");
        binding.viewMorningProgressBar.setProgress(morningProgressPercentage);

        if (isMorningSubmitted) {
            binding.tvMorningHeaderStatus.setText("SÁNG NAY • ĐÃ HOÀN THÀNH");
            binding.tvMorningHeaderStatus.setTextColor(Color.parseColor("#677559"));
            binding.btnStartMorningRoutine.setText("Đã hoàn thành");
            binding.btnStartMorningRoutine.setEnabled(true);
            applyRoutineButtonIcon(binding.btnStartMorningRoutine, R.drawable.ic_check);
        } else {
            if (hour < 6) {
                binding.tvMorningHeaderStatus.setText("SÁNG NAY • CHƯA ĐẾN GIỜ");
                binding.tvMorningHeaderStatus.setTextColor(Color.parseColor("#3E4D44"));
                binding.btnStartMorningRoutine.setText("Chưa đến giờ");
                binding.btnStartMorningRoutine.setEnabled(true);
                applyRoutineButtonIcon(binding.btnStartMorningRoutine, R.drawable.ic_leaf);
            } else if (hour >= 11) {
                binding.tvMorningHeaderStatus.setText("SÁNG NAY • ĐÃ BỎ LỠ");
                binding.tvMorningHeaderStatus.setTextColor(Color.parseColor("#FF3B30"));
                binding.btnStartMorningRoutine.setText("Đã bỏ lỡ");
                binding.btnStartMorningRoutine.setEnabled(true);
                applyRoutineButtonIcon(binding.btnStartMorningRoutine, R.drawable.ic_leaf);
            } else {
                if (completedMorningStepsCount > 0) {
                    binding.tvMorningHeaderStatus.setText("SÁNG NAY • ĐANG THỰC HIỆN");
                    binding.btnStartMorningRoutine.setText("Tiếp tục Routine");
                } else {
                    binding.tvMorningHeaderStatus.setText("SÁNG NAY • CHƯA BẮT ĐẦU");
                    binding.btnStartMorningRoutine.setText("Bắt đầu Routine");
                }
                binding.tvMorningHeaderStatus.setTextColor(Color.parseColor("#3E4D44"));
                binding.btnStartMorningRoutine.setEnabled(true);
                applyRoutineButtonIcon(binding.btnStartMorningRoutine, R.drawable.ic_leaf);
            }
        }

        boolean morningMissed = !isMorningSubmitted && hour >= 11;
        boolean isEveningActive = hour >= 18 || hour < 2;
        String eveningTargetDate;
        if (hour < 2) {
            Calendar prevCal = Calendar.getInstance();
            prevCal.add(Calendar.DAY_OF_YEAR, -1);
            eveningTargetDate = sdf.format(prevCal.getTime());
        } else {
            eveningTargetDate = todayStr;
        }

        boolean hasCompletedEveningToday = ProfileSession.isEveningRewardAwarded(ctx, eveningTargetDate);
        boolean isEveningSubmitted = ProfileSession.isRoutineSubmitted(ctx, "evening", eveningTargetDate);

        Set<String> rawEveningSteps = ProfileSession.getEveningSteps(ctx);
        int activeEveningStepsCount = 0;
        for (String raw : rawEveningSteps) {
            String[] parts = raw.split(":");
            if (parts.length >= 4 && Boolean.parseBoolean(parts[3])) activeEveningStepsCount++;
        }

        int completedEveningStepsCount;
        if (hasCompletedEveningToday) {
            completedEveningStepsCount = activeEveningStepsCount;
        } else {
            Set<String> completedStepIds = ProfileSession.getCompletedStepIdsForDate(ctx, eveningTargetDate);
            int count = 0;
            for (String raw : rawEveningSteps) {
                String[] parts = raw.split(":");
                if (parts.length >= 4 && Boolean.parseBoolean(parts[3])) {
                    try {
                        int index = Integer.parseInt(parts[0]);
                        if (completedStepIds.contains("evening_" + index)) count++;
                    } catch (Exception ignored) {}
                }
            }
            completedEveningStepsCount = count;
        }

        int eveningProgressPercentage = activeEveningStepsCount > 0 ? (completedEveningStepsCount * 100) / activeEveningStepsCount : 0;
        binding.tvEveningProgressText.setText(completedEveningStepsCount + "/" + activeEveningStepsCount + " BƯỚC");
        binding.viewEveningProgressBar.setProgress(eveningProgressPercentage);

        if (isEveningSubmitted) {
            binding.tvEveningHeaderStatus.setText(hour < 2 ? "TỐI QUA • ĐÃ HOÀN THÀNH" : "TỐI NAY • ĐÃ HOÀN THÀNH");
            binding.tvEveningHeaderStatus.setTextColor(Color.parseColor("#677559"));
            binding.btnStartEveningRoutine.setText("Đã hoàn thành");
            binding.btnStartEveningRoutine.setEnabled(true);
            applyRoutineButtonIcon(binding.btnStartEveningRoutine, R.drawable.ic_check);
        } else {
            if (isEveningActive) {
                String prefixStr = hour < 2 ? "TỐI QUA" : "TỐI NAY";
                if (completedEveningStepsCount > 0) {
                    binding.tvEveningHeaderStatus.setText(prefixStr + " • ĐANG THỰC HIỆN");
                    binding.btnStartEveningRoutine.setText("Tiếp tục Routine");
                } else {
                    binding.tvEveningHeaderStatus.setText(prefixStr + " • CHƯA BẮT ĐẦU");
                    binding.btnStartEveningRoutine.setText("Bắt đầu Routine");
                }
                binding.tvEveningHeaderStatus.setTextColor(Color.parseColor("#3E4D44"));
                binding.btnStartEveningRoutine.setEnabled(true);
                applyRoutineButtonIcon(binding.btnStartEveningRoutine, R.drawable.ic_leaf);
            } else {
                binding.tvEveningHeaderStatus.setText("TỐI NAY • CHƯA ĐẾN GIỜ");
                binding.tvEveningHeaderStatus.setTextColor(Color.parseColor("#3E4D44"));
                binding.btnStartEveningRoutine.setText("Chưa đến giờ");
                binding.btnStartEveningRoutine.setEnabled(true);
                applyRoutineButtonIcon(binding.btnStartEveningRoutine, R.drawable.ic_leaf);
            }
        }

        Set<String> completedSocialDates = ProfileSession.getSkinSocialCompletedDates(ctx);
        boolean hasCompletedSocialToday = completedSocialDates.contains(todayStr);
        binding.btnSocialTask.setEnabled(!hasCompletedSocialToday);

        updateRewardCards(
                isMorningSubmitted,
                hasCompletedMorningToday,
                morningMissed,
                isEveningSubmitted,
                hasCompletedEveningToday,
                currentStreak >= 7,
                currentStreak >= 30,
                hasCompletedSocialToday
        );
    }

    private String getRoutineDate(String type) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if ("evening".equals(type) && hour < 2) {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
        return sdf.format(calendar.getTime());
    }

    private void updateWeekCalendar() {
        Context ctx = requireContext();
        binding.layoutDaysContainer.removeAllViews();

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());
        Set<String> completedMornings = ProfileSession.getCompletedMorningDates(ctx);
        Set<String> completedEvenings = ProfileSession.getCompletedEveningDates(ctx);

        for (int i = 0; i < 14; i++) {
            String dateStr = sdf.format(calendar.getTime());
            boolean isCompleted = completedMornings.contains(dateStr) && completedEvenings.contains(dateStr);
            boolean isToday = dateStr.equals(todayStr);

            ItemStreakDayBinding itemBinding = ItemStreakDayBinding.inflate(
                    LayoutInflater.from(ctx),
                    binding.layoutDaysContainer,
                    false
            );

            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            String dayLabel;
            switch (dayOfWeek) {
                case Calendar.MONDAY: dayLabel = "T2"; break;
                case Calendar.TUESDAY: dayLabel = "T3"; break;
                case Calendar.WEDNESDAY: dayLabel = "T4"; break;
                case Calendar.THURSDAY: dayLabel = "T5"; break;
                case Calendar.FRIDAY: dayLabel = "T6"; break;
                case Calendar.SATURDAY: dayLabel = "T7"; break;
                case Calendar.SUNDAY: dayLabel = "CN"; break;
                default: dayLabel = ""; break;
            }

            itemBinding.tvDayLabel.setText(dayLabel);
            itemBinding.tvDayNum.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));

            if (isToday) {
                itemBinding.tvDayLabel.setTextColor(Color.parseColor("#3E4D44"));
                itemBinding.tvDayLabel.setAlpha(1.0f);
                itemBinding.tvDayNum.setTextColor(Color.parseColor("#FF5722"));
            } else {
                itemBinding.tvDayLabel.setTextColor(Color.parseColor("#803E4D44"));
                itemBinding.tvDayNum.setTextColor(Color.parseColor("#3E4D44"));
            }

            if (isCompleted) {
                itemBinding.layoutIconContainer.setBackgroundResource(R.drawable.com_bg_post);
                itemBinding.ivDayIcon.setImageResource(R.drawable.ic_circle_checked);
                itemBinding.ivDayIcon.setColorFilter(Color.parseColor("#677559"));
            } else {
                if (isToday) {
                    itemBinding.layoutIconContainer.setBackgroundResource(R.drawable.bg_circle_today_border);
                    itemBinding.ivDayIcon.setImageResource(R.drawable.ic_calendar_outline);
                    itemBinding.ivDayIcon.setColorFilter(Color.parseColor("#FF5722"));
                } else {
                    itemBinding.layoutIconContainer.setBackgroundResource(R.drawable.bg_circle_white_border);
                    itemBinding.ivDayIcon.setImageResource(R.drawable.ic_calendar_outline);
                    itemBinding.ivDayIcon.setColorFilter(Color.parseColor("#677559"));
                }
            }

            binding.layoutDaysContainer.addView(itemBinding.getRoot());
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void triggerSocialTaskReward() {
        Context ctx = requireContext();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());
        Set<String> completedSocialDates = ProfileSession.getSkinSocialCompletedDates(ctx);

        if (completedSocialDates.contains(todayStr)) {
            Toast.makeText(ctx, "Bạn đã nhận phần thưởng liên kết hôm nay rồi!", Toast.LENGTH_SHORT).show();
            return;
        }

        final int rewardPoints = 10;
        new Thread(() -> {
            try {
                com.veganbeauty.app.utils.RewardPointsHelper.awardPoints(
                        ctx,
                        "SOCIAL_TASK",
                        rewardPoints,
                        "Kết nối bạn bè (Social Task)",
                        "từ nhiệm vụ kết nối bạn bè"
                );
                ProfileSession.addSkinSocialCompletedDate(ctx, todayStr);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(ctx, "Kết nối thành công!", Toast.LENGTH_SHORT).show();
                        refreshUI();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUI();
    }

    @Override
    public void observeViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        FlowLiveDataConversions.asLiveData(db.rewardPointDao().getTotalPointsFlow())
                .observe(getViewLifecycleOwner(), points -> {
                    if (binding == null || !isAdded()) return;
                    int totalPoints = (points != null && !points.isEmpty()) ? points.get(0).total : 0;
                    binding.tvUserCoins.setText(totalPoints + " ROOTIE COINS");
                });
    }

    private void updateRewardCards(
            boolean morningCompleted,
            boolean morningRewardClaimed,
            boolean morningMissed,
            boolean eveningCompleted,
            boolean eveningRewardClaimed,
            boolean weeklyCompleted,
            boolean loyaltyCompleted,
            boolean socialCompleted
    ) {
        Context ctx = requireContext();
        applyRewardCardStyle(
                binding.layoutMorningReward,
                binding.flMorningRewardIcon,
                binding.ivMorningRewardIcon,
                binding.tvMorningRewardTitle,
                binding.tvMorningRewardSubtitle,
                morningCompleted,
                morningMissed ? 0.55f : 1.0f
        );
        applyRewardXuStyle(
                binding.ivMorningRewardCoin,
                binding.tvMorningRewardXu,
                binding.tvMorningRewardFrequency,
                morningRewardClaimed,
                ContextCompat.getColor(ctx, R.color.reward_gold)
        );

        applyRewardCardStyle(
                binding.layoutEveningReward,
                binding.flEveningRewardIcon,
                binding.ivEveningRewardIcon,
                binding.tvEveningRewardTitle,
                binding.tvEveningRewardSubtitle,
                eveningCompleted,
                1.0f
        );
        applyRewardXuStyle(
                binding.ivEveningRewardCoin,
                binding.tvEveningRewardXu,
                binding.tvEveningRewardFrequency,
                eveningRewardClaimed,
                ContextCompat.getColor(ctx, R.color.reward_gold)
        );

        applyRewardCardStyle(
                binding.layoutWeeklyReward,
                binding.flWeeklyRewardIcon,
                binding.ivWeeklyRewardIcon,
                binding.tvWeeklyRewardTitle,
                binding.tvWeeklyRewardSubtitle,
                weeklyCompleted,
                1.0f
        );
        applyRewardXuStyle(
                binding.ivWeeklyRewardCoin,
                binding.tvWeeklyRewardXu,
                binding.tvWeeklyRewardFrequency,
                weeklyCompleted,
                ContextCompat.getColor(ctx, R.color.reward_gold_weekly)
        );

        applyRewardCardStyle(
                binding.layoutLoyaltyReward,
                binding.flLoyaltyRewardIcon,
                binding.ivLoyaltyRewardIcon,
                binding.tvLoyaltyRewardTitle,
                binding.tvLoyaltyRewardSubtitle,
                loyaltyCompleted,
                1.0f
        );
        applyRewardXuStyle(
                binding.ivLoyaltyRewardCoin,
                binding.tvLoyaltyRewardXu,
                binding.tvLoyaltyRewardFrequency,
                loyaltyCompleted,
                ContextCompat.getColor(ctx, R.color.reward_gold)
        );

        applyRewardCardStyle(
                binding.btnSocialTask,
                binding.flSocialRewardIcon,
                binding.ivSocialRewardIcon,
                binding.tvSocialRewardTitle,
                binding.tvSocialRewardSubtitle,
                socialCompleted,
                1.0f
        );
        applyRewardXuStyle(
                binding.ivSocialRewardCoin,
                binding.tvSocialRewardXu,
                binding.tvSocialRewardFrequency,
                socialCompleted,
                ContextCompat.getColor(ctx, R.color.reward_gold)
        );
    }

    private void applyRewardCardStyle(
            View card,
            View iconContainer,
            ImageView icon,
            TextView title,
            TextView subtitle,
            boolean completed,
            float cardAlpha
    ) {
        card.setAlpha(cardAlpha);
        Context ctx = requireContext();
        int primary = ContextCompat.getColor(ctx, R.color.primary);
        int secondary = ContextCompat.getColor(ctx, R.color.secondary);
        int white = ContextCompat.getColor(ctx, R.color.white);

        if (completed) {
            card.setBackgroundResource(R.drawable.skin_bg_reward_card_completed);
            iconContainer.setBackgroundResource(R.drawable.skin_bg_reward_icon_completed);
            title.setTextColor(primary);
            subtitle.setTextColor(secondary);
            icon.setColorFilter(white, PorterDuff.Mode.SRC_IN);
        } else {
            card.setBackgroundResource(R.drawable.skin_bg_reward_card_pending);
            iconContainer.setBackgroundResource(R.drawable.skin_bg_reward_icon_pending);
            title.setTextColor(primary);
            subtitle.setTextColor(ContextCompat.getColor(ctx, R.color.tertiary));
            icon.setColorFilter(secondary, PorterDuff.Mode.SRC_IN);
        }
    }

    private void applyRewardXuStyle(
            ImageView coinIcon,
            TextView xuText,
            TextView frequencyText,
            boolean claimed,
            int activeGoldColor
    ) {
        int muted = Color.parseColor("#8E8E93");
        int frequency = Color.parseColor("#AEAEB2");
        if (claimed) {
            xuText.setTextColor(muted);
            coinIcon.setColorFilter(muted, PorterDuff.Mode.SRC_IN);
        } else {
            xuText.setTextColor(activeGoldColor);
            coinIcon.clearColorFilter();
        }
        frequencyText.setTextColor(frequency);
    }

    private void applyRoutineButtonIcon(AppCompatButton button, @DrawableRes int iconRes) {
        Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes);
        if (icon == null) {
            button.setCompoundDrawables(null, null, null, null);
            return;
        }
        icon = icon.mutate();
        float density = getResources().getDisplayMetrics().density;
        int iconDp = iconRes == R.drawable.ic_check ? 15 : 20;
        int sizePx = (int) (iconDp * density);
        icon.setBounds(0, 0, sizePx, sizePx);
        icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        button.setCompoundDrawables(icon, null, null, null);
        button.setCompoundDrawablePadding((int) ((iconRes == R.drawable.ic_check ? 6 : 8) * density));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
