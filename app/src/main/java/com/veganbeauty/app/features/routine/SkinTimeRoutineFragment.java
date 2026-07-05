package com.veganbeauty.app.features.routine;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.lifecycle.LifecycleOwnerKt;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.databinding.ItemTimeRoutineStepBinding;
import com.veganbeauty.app.databinding.SkinTimeRoutineBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class SkinTimeRoutineFragment extends RootieFragment {

    private SkinTimeRoutineBinding binding;
    private String routineType = "morning";
    private final List<SkincareStep> activeSteps = new ArrayList<>();
    private final Map<Integer, ProductEntity> stepProducts = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinTimeRoutineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        android.content.Context ctx = requireContext();

        if (getArguments() != null) {
            routineType = getArguments().getString("routine_type", "morning");
        }

        com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, ProfileSession.getAvatar(ctx));
        binding.ivAvatar.setOnClickListener(v -> BottomNavHelper.navigate(this, R.id.nav_account));

        String fullName = ProfileSession.getFullName(ctx);
        if ("morning".equals(routineType)) {
            binding.tvTitle.setText("Morning Routine");
            binding.tvMotivationalSlogan.setText("Bắt đầu ngày mới với làn da tươi tắn và tràn đầy năng lượng nhé " + fullName + " ơi!");
            binding.tvTipTitle.setText("Mẹo nhỏ sáng nay");
            binding.tvTipDesc.setText("Đừng quên thoa kem chống nắng ngay cả khi bạn làm việc trong nhà nhé!");
        } else {
            binding.tvTitle.setText("Evening Routine");
            binding.tvMotivationalSlogan.setText("Nuôi dưỡng làn da phục hồi và thư giãn sâu sau một ngày dài nhé " + fullName + " ơi!");
            binding.tvTipTitle.setText("Mẹo nhỏ tối nay");
            binding.tvTipDesc.setText("Hãy thoa kem dưỡng trước 20 phút trước khi ngủ để dưỡng chất thẩm thấu tốt nhất nhé!");
        }

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.btnCustomize.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new SkinRoutineSettingsFragment())
                        .addToBackStack(null).commit());

        binding.btnCompleteRoutine.setOnClickListener(v -> completeRoutineAction());

        loadSteps();

        binding.layoutNotification.getRoot().setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                        .addToBackStack(null).commit());
    }

    private String getRoutineDate(String type) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if ("evening".equals(type) && hour < 2) cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }

    private boolean isWithinTimeWindow(String type) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if ("morning".equals(type)) return hour >= 6 && hour <= 10;
        else return hour >= 18 || hour < 2;
    }

    private void completeRoutineAction() {
        android.content.Context ctx = requireContext();
        String targetDate = getRoutineDate(routineType);

        if (ProfileSession.isRoutineSubmitted(ctx, routineType, targetDate)) {
            Toast.makeText(ctx, "Routine đã được hoàn tất trước đó!", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        ProfileSession.setRoutineSubmitted(ctx, routineType, targetDate, true);

        Set<String> completedSteps = ProfileSession.getCompletedStepIdsForDate(ctx, targetDate);
        int totalCount = activeSteps.size();
        int completedCount = 0;
        for (SkincareStep s : activeSteps) if (completedSteps.contains(routineType + "_" + s.getIndex())) completedCount++;

        if (isWithinTimeWindow(routineType) && completedCount > 0) {
            if ("morning".equals(routineType)) ProfileSession.addCompletedMorningDate(ctx, targetDate);
            else ProfileSession.addCompletedEveningDate(ctx, targetDate);
        } else {
            if ("morning".equals(routineType)) {
                Set<String> set = new HashSet<>(ProfileSession.getCompletedMorningDates(ctx));
                set.remove(targetDate);
                ProfileSession.setCompletedMorningDates(ctx, set);
            } else {
                Set<String> set = new HashSet<>(ProfileSession.getCompletedEveningDates(ctx));
                set.remove(targetDate);
                ProfileSession.setCompletedEveningDates(ctx, set);
            }
        }

        if (!isWithinTimeWindow(routineType)) {
            Toast.makeText(ctx, "Đã chốt phiên Routine! Ngoài khung giờ quy định nên không được cộng xu.", Toast.LENGTH_LONG).show();
            checkStreakAndUpdateAsync(routineType);
            return;
        }

        if (totalCount > 0 && completedCount == totalCount) {
            boolean isRewardGiven = "morning".equals(routineType) ? ProfileSession.isMorningRewardAwarded(ctx, targetDate) : ProfileSession.isEveningRewardAwarded(ctx, targetDate);
            if (!isRewardGiven) {
                if ("morning".equals(routineType)) ProfileSession.setMorningRewardAwarded(ctx, targetDate, true);
                else ProfileSession.setEveningRewardAwarded(ctx, targetDate, true);

                RootieDatabase db = RootieDatabase.getDatabase(ctx);
                final int rewardPoints = 10;
                LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                        BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                            try {
                                db.rewardPointDao().insertRewardPoints(new RewardPointEntity(
                                        0, "morning".equals(routineType) ? "MORNING_ROUTINE" : "EVENING_ROUTINE", rewardPoints,
                                        "morning".equals(routineType) ? "Hoàn thành Routine Sáng" : "Hoàn thành Routine Tối", System.currentTimeMillis()
                                ));
                                com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsToFirestore(ctx);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        String source = "morning".equals(routineType)
                                                ? "từ Routine Sáng"
                                                : "từ Routine Tối";
                                        com.veganbeauty.app.utils.CoinRewardDialogHelper.show(
                                                SkinTimeRoutineFragment.this, rewardPoints, source
                                        );
                                    });
                                }
                                checkStreakAndUpdateAsync(routineType);
                            } catch (Exception e) { e.printStackTrace(); }
                            return kotlin.Unit.INSTANCE;
                        }, cont));
            } else {
                Toast.makeText(ctx, "Routine đã được hoàn tất và nhận thưởng trước đó!", Toast.LENGTH_SHORT).show();
                checkStreakAndUpdateAsync(routineType);
            }
        } else {
            Toast.makeText(ctx, "Đã chốt phiên Routine! Bạn chưa hoàn thành 100% các bước nên không được cộng xu.", Toast.LENGTH_LONG).show();
            checkStreakAndUpdateAsync(routineType);
        }
    }

    private void checkStreakAndUpdateAsync(String type) {
        android.content.Context ctx = getContext();
        if (ctx == null) return;
        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                    try {
                        checkStreakAndUpdate(type);
                        com.veganbeauty.app.utils.SyncDataHelper.pushSkincareHistoryToFirestore(ctx, getRoutineDate(type));
                    } catch (Exception e) { e.printStackTrace(); }
                    getParentFragmentManager().popBackStack();
                    return kotlin.Unit.INSTANCE;
                }, cont));
    }

    private void loadSteps() {
        android.content.Context ctx = requireContext();
        Set<String> rawSteps = "morning".equals(routineType) ? ProfileSession.getMorningSteps(ctx) : ProfileSession.getEveningSteps(ctx);

        activeSteps.clear();
        for (String raw : rawSteps) {
            String[] parts = raw.split(":");
            if (parts.length >= 4) {
                try {
                    int index = Integer.parseInt(parts[0]);
                    boolean isChecked = Boolean.parseBoolean(parts[3]);
                    if (isChecked) activeSteps.add(new SkincareStep(index, parts[1], parts[2], true));
                } catch (Exception ignored) {}
            }
        }
        Collections.sort(activeSteps, (a, b) -> Integer.compare(a.getIndex(), b.getIndex()));

        populateStepsList();
        updateStatsAndProgress();
    }

    private void populateStepsList() {
        if (binding == null) return;
        android.content.Context ctx = requireContext();
        binding.layoutStepsContainer.removeAllViews();

        String targetDate = getRoutineDate(routineType);
        Set<String> completedSteps = ProfileSession.getCompletedStepIdsForDate(ctx, targetDate);
        boolean isSubmitted = ProfileSession.isRoutineSubmitted(ctx, routineType, targetDate);

        for (SkincareStep step : activeSteps) {
            ItemTimeRoutineStepBinding stepBinding = ItemTimeRoutineStepBinding.inflate(LayoutInflater.from(ctx), binding.layoutStepsContainer, false);

            stepBinding.tvStepName.setText(step.getName());
            stepBinding.tvStepDesc.setText(step.getDescription());
            stepBinding.tvStepTime.setText(getStepTime(step.getName()));
            stepBinding.ivStepIcon.setImageResource(getStepIconRes(step.getName()));

            String stepId = routineType + "_" + step.getIndex();
            boolean isStepCompleted = completedSteps.contains(stepId);

            if (isStepCompleted) {
                stepBinding.ivCheckbox.setImageResource(R.drawable.ic_circle_checked);
                ImageViewCompat.setImageTintList(stepBinding.ivCheckbox, ColorStateList.valueOf(0xFF3E4D44));
            } else {
                stepBinding.ivCheckbox.setImageResource(R.drawable.ic_circle);
                ImageViewCompat.setImageTintList(stepBinding.ivCheckbox, ColorStateList.valueOf(0xFFD9D9D9));
            }

            ProductEntity matchedProduct = stepProducts.get(step.getIndex());
            if (matchedProduct != null) {
                stepBinding.tvViewProductLink.setVisibility(View.VISIBLE);
                stepBinding.tvViewProductLink.setText("🛒 Gợi ý: " + matchedProduct.getName());
                stepBinding.tvViewProductLink.setOnClickListener(v -> {
                    ProductDetailLauncher.open(this, matchedProduct);
                });
            } else {
                stepBinding.tvViewProductLink.setVisibility(View.GONE);
            }

            View.OnClickListener clickListener = v -> toggleStep(step.getIndex());
            stepBinding.getRoot().setOnClickListener(clickListener);
            stepBinding.ivCheckbox.setOnClickListener(clickListener);

            binding.layoutStepsContainer.addView(stepBinding.getRoot());
        }
    }

    private void toggleStep(int stepIndex) {
        android.content.Context ctx = requireContext();
        String targetDate = getRoutineDate(routineType);

        if (ProfileSession.isRoutineSubmitted(ctx, routineType, targetDate)) {
            Toast.makeText(ctx, "Routine đã được chốt và hoàn thành, không thể thay đổi!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isWithinTimeWindow(routineType)) {
            Toast.makeText(ctx, "Chưa đến giờ làm routine hoặc đã quá giờ quy định!", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> currentSet = new HashSet<>(ProfileSession.getCompletedStepIdsForDate(ctx, targetDate));
        String stepId = routineType + "_" + stepIndex;

        if (currentSet.contains(stepId)) currentSet.remove(stepId);
        else currentSet.add(stepId);
        ProfileSession.setCompletedStepIdsForDate(ctx, targetDate, currentSet);

        int count = 0;
        for (SkincareStep s : activeSteps) if (currentSet.contains(routineType + "_" + s.getIndex())) count++;

        if ("morning".equals(routineType)) {
            Set<String> set = new HashSet<>(ProfileSession.getCompletedMorningDates(ctx));
            if (count > 0 && isWithinTimeWindow(routineType)) set.add(targetDate);
            else set.remove(targetDate);
            ProfileSession.setCompletedMorningDates(ctx, set);
        } else {
            Set<String> set = new HashSet<>(ProfileSession.getCompletedEveningDates(ctx));
            if (count > 0 && isWithinTimeWindow(routineType)) set.add(targetDate);
            else set.remove(targetDate);
            ProfileSession.setCompletedEveningDates(ctx, set);
        }

        if (isWithinTimeWindow(routineType)) {
            LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                    BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                        try { checkStreakAndUpdate(routineType); } catch(Exception e) { e.printStackTrace(); }
                        return kotlin.Unit.INSTANCE;
                    }, cont));
        }

        populateStepsList();
        updateStatsAndProgress();
    }

    private void updateStatsAndProgress() {
        if (binding == null) return;
        android.content.Context ctx = requireContext();
        String targetDate = getRoutineDate(routineType);
        Set<String> completedSteps = ProfileSession.getCompletedStepIdsForDate(ctx, targetDate);

        int totalCount = activeSteps.size();
        int completedCount = 0;
        for (SkincareStep s : activeSteps) if (completedSteps.contains(routineType + "_" + s.getIndex())) completedCount++;

        binding.tvStepCountBadge.setText(totalCount + " Bước");

        int totalMinutes = 0, completedMinutes = 0;
        boolean isSubmitted = ProfileSession.isRoutineSubmitted(ctx, routineType, targetDate);
        for (SkincareStep s : activeSteps) {
            int mins = getStepTimeVal(getStepTime(s.getName()));
            totalMinutes += mins;
            if (completedSteps.contains(routineType + "_" + s.getIndex())) completedMinutes += mins;
        }
        binding.tvDurationMins.setText(completedMinutes + "/" + totalMinutes + " Phút");

        int percentage = totalCount > 0 ? (completedCount * 100) / totalCount : 0;
        binding.progressBar.setProgress(percentage);

        if (isSubmitted) {
            binding.btnCompleteRoutine.setText("Đã hoàn thành");
            binding.btnCompleteRoutine.setEnabled(false);
            binding.btnCompleteRoutine.setBackgroundResource(R.drawable.skin_bg_btn_green);
            binding.btnCompleteRoutine.setTextColor(Color.WHITE);
            applyCompleteButtonIcon(R.drawable.ic_check);
        } else if (!isWithinTimeWindow(routineType)) {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if ("morning".equals(routineType)) {
                if (hour < 6) binding.btnCompleteRoutine.setText("Chưa đến giờ");
                else binding.btnCompleteRoutine.setText("Đã bỏ lỡ");
            } else binding.btnCompleteRoutine.setText("Chưa đến giờ");
            binding.btnCompleteRoutine.setEnabled(false);
            binding.btnCompleteRoutine.setBackgroundResource(R.drawable.skin_bg_btn_disabled);
            binding.btnCompleteRoutine.setTextColor(Color.parseColor("#8E8E93"));
            applyCompleteButtonIcon(0);
        } else {
            binding.btnCompleteRoutine.setText("Hoàn tất Routine");
            binding.btnCompleteRoutine.setEnabled(true);
            binding.btnCompleteRoutine.setBackgroundResource(R.drawable.skin_bg_btn_green);
            binding.btnCompleteRoutine.setTextColor(Color.WHITE);
            applyCompleteButtonIcon(0);
        }
    }

    private void applyCompleteButtonIcon(@DrawableRes int iconRes) {
        if (binding == null) return;
        if (iconRes == 0) {
            binding.btnCompleteRoutine.setCompoundDrawables(null, null, null, null);
            return;
        }
        Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes);
        if (icon == null) {
            binding.btnCompleteRoutine.setCompoundDrawables(null, null, null, null);
            return;
        }
        icon = icon.mutate();
        int sizePx = (int) (15 * getResources().getDisplayMetrics().density);
        icon.setBounds(0, 0, sizePx, sizePx);
        icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        binding.btnCompleteRoutine.setCompoundDrawables(icon, null, null, null);
        binding.btnCompleteRoutine.setCompoundDrawablePadding((int) (6 * getResources().getDisplayMetrics().density));
    }

    private void checkStreakAndUpdate(String type) throws Exception {
        android.content.Context ctx = requireContext();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String targetDate = getRoutineDate(type);

        Set<String> completedMornings = ProfileSession.getCompletedMorningDates(ctx);
        Set<String> completedEvenings = ProfileSession.getCompletedEveningDates(ctx);

        if (completedMornings.contains(targetDate) && completedEvenings.contains(targetDate)) {
            String lastCompletedStr = ProfileSession.getSkinLastCompletedDate(ctx);
            if (lastCompletedStr.equals(targetDate)) return;

            int currentStreak = ProfileSession.getSkinStreak(ctx);
            int newStreak;
            if (!lastCompletedStr.isEmpty()) {
                Calendar lastCal = Calendar.getInstance();
                Date d = sdf.parse(lastCompletedStr);
                if (d != null) lastCal.setTime(d);
                lastCal.add(Calendar.DAY_OF_YEAR, 1);
                if (sdf.format(lastCal.getTime()).equals(targetDate)) newStreak = currentStreak + 1;
                else newStreak = 1;
            } else {
                newStreak = 1;
            }

            ProfileSession.setSkinStreak(ctx, newStreak);
            ProfileSession.setSkinLastCompletedDate(ctx, targetDate);

            RootieDatabase db = RootieDatabase.getDatabase(ctx);
            if (newStreak > 0 && newStreak % 30 == 0) {
                db.rewardPointDao().insertRewardPoints(new RewardPointEntity(
                        0, "MONTHLY_STREAK", 200, "Thưởng chuỗi 30 ngày chăm da", System.currentTimeMillis()
                ));
                com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsToFirestore(ctx);
                Toast.makeText(ctx, "Tuyệt vời! Đạt chuỗi 30 ngày chăm da +200 xu!", Toast.LENGTH_LONG).show();
            } else if (newStreak > 0 && newStreak % 7 == 0) {
                db.rewardPointDao().insertRewardPoints(new RewardPointEntity(
                        0, "WEEKLY_STREAK", 50, "Thưởng chuỗi 7 ngày chăm da", System.currentTimeMillis()
                ));
                com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsToFirestore(ctx);
                Toast.makeText(ctx, "Tuyệt vời! Đạt chuỗi 7 ngày chăm da +50 xu!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getStepTime(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("cleanser") || lower.contains("sữa rửa mặt") || lower.contains("rửa mặt")) return "2p";
        if (lower.contains("toner") || lower.contains("nước hoa hồng") || lower.contains("cân bằng")) return "1p";
        if (lower.contains("serum") || lower.contains("tinh chất")) return "3p";
        if (lower.contains("moisturizer") || lower.contains("kem dưỡng ẩm") || lower.contains("dưỡng ẩm") || lower.contains("khóa ẩm")) return "2p";
        if (lower.contains("sunscreen") || lower.contains("chống nắng") || lower.contains("kem chống nắng")) return "5p";
        if (lower.contains("makeup remover") || lower.contains("tẩy trang")) return "5p";
        return "2p";
    }

    private int getStepTimeVal(String timeStr) {
        try { return Integer.parseInt(timeStr.replace("p", "")); } catch (Exception e) { return 2; }
    }

    private int getStepIconRes(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("cleanser") || lower.contains("sữa rửa mặt") || lower.contains("rửa mặt")) return R.drawable.ic_water_drop_outline;
        if (lower.contains("toner") || lower.contains("nước hoa hồng") || lower.contains("cân bằng")) return R.drawable.ic_water_drop;
        if (lower.contains("serum") || lower.contains("tinh chất")) return R.drawable.ic_chemistry_flask;
        if (lower.contains("moisturizer") || lower.contains("kem dưỡng ẩm") || lower.contains("dưỡng ẩm") || lower.contains("khóa ẩm")) return R.drawable.ic_face;
        if (lower.contains("sunscreen") || lower.contains("chống nắng") || lower.contains("kem chống nắng")) return R.drawable.ic_sun;
        if (lower.contains("makeup remover") || lower.contains("tẩy trang")) return R.drawable.ic_water_drop_outline;
        return R.drawable.ic_face;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, ProfileSession.getAvatar(requireContext()));
        }
    }

    @Override
    public void observeViewModel() {}

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
