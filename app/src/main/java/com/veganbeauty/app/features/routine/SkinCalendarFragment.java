package com.veganbeauty.app.features.routine;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.databinding.SkinCalendarBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.myskin.SkinDetailHeaderScrollHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SkinCalendarFragment extends RootieFragment {

    private SkinCalendarBinding binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat sdfYearMonth = new SimpleDateFormat("'Tháng' MM, yyyy", new Locale("vi", "VN"));
    private final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private int selectedTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(View view) {
        if (binding == null) return;
        Context ctx = requireContext();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.layoutNotification.getRoot().setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                        .addToBackStack(null).commit());

        com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, ProfileSession.getAvatar(ctx));
        binding.ivAvatar.setOnClickListener(v -> BottomNavHelper.navigate(this, R.id.nav_account));

        Set<String> morningDates = ProfileSession.getCompletedMorningDates(ctx);
        Set<String> eveningDates = ProfileSession.getCompletedEveningDates(ctx);

        int currentStreak = calculateCurrentStreak(morningDates, eveningDates);
        binding.tvCurrentStreak.setText(String.valueOf(currentStreak));
        ProfileSession.setSkinStreak(ctx, currentStreak);

        int maxStreak = calculateMaxStreak(morningDates, eveningDates);
        SharedPreferences prefs = ctx.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE);
        prefs.edit().putInt("skin_max_streak", maxStreak).apply();
        binding.tvMaxStreak.setText(maxStreak + " ngày");

        Set<String> unionDates = new HashSet<>(morningDates);
        unionDates.addAll(eveningDates);
        binding.tvTotalCompletedDays.setText(unionDates.size() + " ngày");

        String pillText;
        if (currentStreak == 0) pillText = "Bắt đầu chuỗi chăm da ngay hôm nay nhé!";
        else if (currentStreak == 1) pillText = "Vượt 10% người dùng khác. Bạn hãy duy trì nhé!";
        else if (currentStreak == 2) pillText = "Vượt 25% người dùng khác. Bạn hãy duy trì nhé!";
        else if (currentStreak == 3) pillText = "Vượt 40% người dùng khác. Bạn hãy duy trì nhé!";
        else if (currentStreak == 4) pillText = "Vượt 55% người dùng khác. Bạn hãy duy trì nhé!";
        else if (currentStreak == 5) pillText = "Vượt 70% người dùng khác. Bạn hãy duy trì nhé!";
        else if (currentStreak == 6) pillText = "Vượt 80% người dùng khác. Bạn hãy duy trì nhé!";
        else if (currentStreak >= 7 && currentStreak <= 13) pillText = "Vượt 90% người dùng khác. Bạn hãy duy trì nhé!";
        else if (currentStreak >= 14 && currentStreak <= 29) pillText = "Vượt 95% người dùng khác. Bạn hãy duy trì nhé!";
        else pillText = "Vượt 99% người dùng khác. Bạn hãy duy trì nhé!";
        binding.tvStreakPillText.setText(pillText);

        calculateHabitAnalysis(ctx);
        renderCalendar(ctx);

        binding.btnPrevMonth.setOnClickListener(v -> { calendar.add(Calendar.MONTH, -1); renderCalendar(ctx); populateTimeline(ctx); });
        binding.btnNextMonth.setOnClickListener(v -> { calendar.add(Calendar.MONTH, 1); renderCalendar(ctx); populateTimeline(ctx); });

        setupTabs(ctx);
        populateTimeline(ctx);

        SimpleDateFormat currentMonthFormat = new SimpleDateFormat("'Nhật ký chi tiết tháng' M", new Locale("vi", "VN"));
        binding.btnDetailedHistory.setText(currentMonthFormat.format(new Date()));
        setupScrollHideHeader();
    }

    private void setupScrollHideHeader() {
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                binding.layoutHeader,
                binding.calendarScroll,
                0
        );
        headerScrollHelper.attachToNestedScrollView(binding.calendarScroll);
    }

    private int calculateMaxStreak(Set<String> morningDates, Set<String> eveningDates) {
        Set<String> intersect = new HashSet<>(morningDates);
        intersect.retainAll(eveningDates);

        List<Date> dates = new ArrayList<>();
        for (String str : intersect) {
            try { dates.add(sdfDate.parse(str)); } catch (Exception ignored) {}
        }
        Collections.sort(dates);

        if (dates.isEmpty()) return 0;

        int maxStreak = 0, currentStreak = 0;
        Date prevDate = null;
        long msInDay = 24 * 60 * 60 * 1000L;

        for (Date date : dates) {
            if (prevDate == null) currentStreak = 1;
            else {
                long diffMs = date.getTime() - prevDate.getTime();
                int diffDays = (int) Math.round((double) diffMs / msInDay);
                if (diffDays == 1) currentStreak++;
                else if (diffDays > 1) {
                    if (currentStreak > maxStreak) maxStreak = currentStreak;
                    currentStreak = 1;
                }
            }
            prevDate = date;
        }
        if (currentStreak > maxStreak) maxStreak = currentStreak;
        return maxStreak;
    }

    private int calculateCurrentStreak(Set<String> morningDates, Set<String> eveningDates) {
        Set<String> completed = new HashSet<>(morningDates);
        completed.retainAll(eveningDates);
        if (completed.isEmpty()) return 0;

        String todayStr = sdfDate.format(new Date());
        Calendar yesterdayCal = Calendar.getInstance(); yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayStr = sdfDate.format(yesterdayCal.getTime());

        if (!completed.contains(todayStr) && !completed.contains(yesterdayStr)) return 0;

        int streak = 0;
        Calendar checkCal = Calendar.getInstance();
        String dateStr = sdfDate.format(checkCal.getTime());

        if (!completed.contains(dateStr)) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1);
            dateStr = sdfDate.format(checkCal.getTime());
        }

        while (completed.contains(dateStr)) {
            streak++;
            checkCal.add(Calendar.DAY_OF_YEAR, -1);
            dateStr = sdfDate.format(checkCal.getTime());
        }
        return streak;
    }

    private void calculateHabitAnalysis(Context ctx) {
        Set<String> morningDates = ProfileSession.getCompletedMorningDates(ctx);
        Set<String> eveningDates = ProfileSession.getCompletedEveningDates(ctx);

        int morningCount = 0, eveningCount = 0;
        Calendar tempCal = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            String dateStr = sdfDate.format(tempCal.getTime());
            if (ProfileSession.isMorningRewardAwarded(ctx, dateStr) || morningDates.contains(dateStr)) morningCount++;
            if (ProfileSession.isEveningRewardAwarded(ctx, dateStr) || eveningDates.contains(dateStr)) eveningCount++;
            tempCal.add(Calendar.DAY_OF_YEAR, -1);
        }

        int morningRate = (morningCount * 100) / 30;
        int eveningRate = (eveningCount * 100) / 30;

        if (morningCount == 0 && eveningCount == 0) {
            binding.tvHabitAnalysisHeading.setText("Hãy bắt đầu hành trình chăm sóc da của bạn!");
            binding.tvHabitAnalysisSub.setText("Chưa có dữ liệu hoàn thành routine nào trong 30 ngày qua.");
            binding.tvHabitTip.setText("Hãy thử đặt báo thức nhắc nhở và chuẩn bị sẵn các sản phẩm skincare ở vị trí dễ thấy nhé!");
        } else if (morningRate == eveningRate) {
            binding.tvHabitAnalysisHeading.setText("Thói quen chăm da của bạn cực kỳ cân bằng và đều đặn!");
            binding.tvHabitAnalysisSub.setText("Tỷ lệ hoàn thành cả sáng và tối đều đạt " + morningRate + "%");
            binding.tvHabitTip.setText("Bạn đang làm rất tốt! Hãy tiếp tục duy trì nhịp điệu tuyệt vời này nhé!");
        } else if (morningRate > eveningRate) {
            binding.tvHabitAnalysisHeading.setText("Bạn thường hoàn thành Routine buổi sáng tốt hơn buổi tối.");
            binding.tvHabitAnalysisSub.setText("Tỷ lệ hoàn thành buổi sáng đạt " + morningRate + "%, buổi tối đạt " + eveningRate + "%");
            binding.tvHabitTip.setText("Hãy thử đặt báo thức skincare tối sớm hơn.");
        } else {
            binding.tvHabitAnalysisHeading.setText("Bạn thường hoàn thành Routine buổi tối tốt hơn buổi sáng.");
            binding.tvHabitAnalysisSub.setText("Tỷ lệ hoàn thành buổi tối đạt " + eveningRate + "%, buổi sáng chỉ đạt " + morningRate + "%");
            binding.tvHabitTip.setText("Hãy thử chuẩn bị đồ skincare từ tối hôm trước lên bàn trang điểm để dễ duy trì buổi sáng.");
        }
    }

    private void renderCalendar(Context ctx) {
        binding.tvMonthYear.setText(sdfYearMonth.format(calendar.getTime()));
        binding.layoutCalendarGrid.removeAllViews();

        Calendar tempCal = (Calendar) calendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);
        int leadDays = 0;
        switch (dayOfWeek) {
            case Calendar.MONDAY: leadDays = 0; break; case Calendar.TUESDAY: leadDays = 1; break;
            case Calendar.WEDNESDAY: leadDays = 2; break; case Calendar.THURSDAY: leadDays = 3; break;
            case Calendar.FRIDAY: leadDays = 4; break; case Calendar.SATURDAY: leadDays = 5; break;
            case Calendar.SUNDAY: leadDays = 6; break;
        }

        int maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        LinearLayout rowLayout = new LinearLayout(ctx);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        binding.layoutCalendarGrid.addView(rowLayout);

        for (int i = 0; i < leadDays; i++) {
            View cell = new View(ctx);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
            rowLayout.addView(cell);
        }

        String todayStr = sdfDate.format(new Date());
        Set<String> morningDates = ProfileSession.getCompletedMorningDates(ctx);
        Set<String> eveningDates = ProfileSession.getCompletedEveningDates(ctx);

        for (int day = 1; day <= maxDays; day++) {
            if (rowLayout.getChildCount() == 7) {
                rowLayout = new LinearLayout(ctx);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.topMargin = (int) (12 * ctx.getResources().getDisplayMetrics().density);
                rowLayout.setLayoutParams(lp);
                binding.layoutCalendarGrid.addView(rowLayout);
            }

            View cell = LayoutInflater.from(ctx).inflate(R.layout.item_skin_calendar_day, rowLayout, false);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvDayNum = cell.findViewById(R.id.tvDayNum);
            View viewDot = cell.findViewById(R.id.viewDot);

            tvDayNum.setText(String.valueOf(day));
            tempCal.set(Calendar.DAY_OF_MONTH, day);
            String cellDateStr = sdfDate.format(tempCal.getTime());

            if (cellDateStr.equals(todayStr)) {
                tvDayNum.setTextColor(Color.WHITE);
                tvDayNum.setBackgroundResource(R.drawable.com_bg_tab_active);
            } else {
                tvDayNum.setTextColor(Color.parseColor("#3E4D44"));
                tvDayNum.setBackground(null);
            }

            boolean isMorningDone = ProfileSession.isMorningRewardAwarded(ctx, cellDateStr) || morningDates.contains(cellDateStr);
            boolean isEveningDone = ProfileSession.isEveningRewardAwarded(ctx, cellDateStr) || eveningDates.contains(cellDateStr);
            boolean isMorningFull = ProfileSession.isMorningRewardAwarded(ctx, cellDateStr);
            boolean isEveningFull = ProfileSession.isEveningRewardAwarded(ctx, cellDateStr);

            if (tempCal.getTime().after(new Date())) {
                viewDot.setVisibility(View.INVISIBLE);
            } else {
                viewDot.setVisibility(View.VISIBLE);
                if (isMorningFull && isEveningFull) viewDot.setBackgroundResource(R.drawable.bg_circle_green_indicator);
                else if (isMorningDone || isEveningDone) viewDot.setBackgroundResource(R.drawable.bg_circle_yellow_indicator);
                else viewDot.setBackgroundResource(R.drawable.bg_circle_red_indicator);
            }
            rowLayout.addView(cell);
        }

        while (rowLayout.getChildCount() < 7) {
            View cell = new View(ctx);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
            rowLayout.addView(cell);
        }
    }

    private void setupTabs(Context ctx) {
        binding.btnTabMonth.setOnClickListener(v -> { selectTab(0); populateTimeline(ctx); });
        binding.btnTabWeek.setOnClickListener(v -> { selectTab(1); populateTimeline(ctx); });
        binding.btnTabToday.setOnClickListener(v -> { selectTab(2); populateTimeline(ctx); });
        selectTab(0);
    }

    private void selectTab(int index) {
        selectedTab = index;
        Context ctx = getContext();
        if (ctx == null) return;

        Drawable activeBg = ContextCompat.getDrawable(ctx, R.drawable.skin_bg_calendar_tab_active);
        Drawable inactiveBg = ContextCompat.getDrawable(ctx, R.drawable.skin_bg_calendar_tab_inactive);

        binding.btnTabMonth.setBackground(index == 0 ? activeBg : inactiveBg);
        binding.btnTabMonth.setTextColor(index == 0 ? Color.WHITE : Color.parseColor("#3E4D44"));
        binding.btnTabMonth.setBackgroundTintList(null);

        binding.btnTabWeek.setBackground(index == 1 ? activeBg : inactiveBg);
        binding.btnTabWeek.setTextColor(index == 1 ? Color.WHITE : Color.parseColor("#3E4D44"));
        binding.btnTabWeek.setBackgroundTintList(null);

        binding.btnTabToday.setBackground(index == 2 ? activeBg : inactiveBg);
        binding.btnTabToday.setTextColor(index == 2 ? Color.WHITE : Color.parseColor("#3E4D44"));
        binding.btnTabToday.setBackgroundTintList(null);
    }

    private void populateTimeline(Context ctx) {
        binding.layoutTimelineContainer.removeAllViews();

        Calendar tempCal = (Calendar) calendar.clone();
        Calendar today = Calendar.getInstance();
        if (tempCal.get(Calendar.YEAR) < today.get(Calendar.YEAR) ||
                (tempCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && tempCal.get(Calendar.MONTH) < today.get(Calendar.MONTH))) {
            tempCal.set(Calendar.DAY_OF_MONTH, tempCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else {
            tempCal.setTime(new Date());
        }

        int listDaysCount;
        if (selectedTab == 0) {
            listDaysCount = tempCal.get(Calendar.DAY_OF_MONTH);
        } else if (selectedTab == 1) {
            listDaysCount = 7;
        } else {
            listDaysCount = 1;
        }

        Set<String> morningDates = ProfileSession.getCompletedMorningDates(ctx);
        Set<String> eveningDates = ProfileSession.getCompletedEveningDates(ctx);

        SimpleDateFormat formatEnglish = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
        SimpleDateFormat formatVietnamese = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));

        List<SkincareStep> activeMorningStepsRaw = new ArrayList<>();
        for (String raw : ProfileSession.getMorningSteps(ctx)) {
            String[] parts = raw.split(":");
            if (parts.length >= 4 && Boolean.parseBoolean(parts[3])) {
                try { activeMorningStepsRaw.add(new SkincareStep(Integer.parseInt(parts[0]), parts[1], parts[2])); } catch (Exception ignored) {}
            }
        }
        Collections.sort(activeMorningStepsRaw, (a, b) -> Integer.compare(a.getIndex(), b.getIndex()));

        List<SkincareStep> activeEveningStepsRaw = new ArrayList<>();
        for (String raw : ProfileSession.getEveningSteps(ctx)) {
            String[] parts = raw.split(":");
            if (parts.length >= 4 && Boolean.parseBoolean(parts[3])) {
                try { activeEveningStepsRaw.add(new SkincareStep(Integer.parseInt(parts[0]), parts[1], parts[2])); } catch (Exception ignored) {}
            }
        }
        Collections.sort(activeEveningStepsRaw, (a, b) -> Integer.compare(a.getIndex(), b.getIndex()));

        for (int i = 0; i < listDaysCount; i++) {
            String cellDateStr = sdfDate.format(tempCal.getTime());
            View viewDay = LayoutInflater.from(ctx).inflate(R.layout.item_skin_timeline_day, binding.layoutTimelineContainer, false);
            TextView tvDate = viewDay.findViewById(R.id.tvTimelineDate);
            TextView tvDayOfWeek = viewDay.findViewById(R.id.tvTimelineDayOfWeek);

            tvDate.setText(formatEnglish.format(tempCal.getTime()));
            tvDayOfWeek.setText(formatVietnamese.format(tempCal.getTime()).toUpperCase(Locale.getDefault()));

            View cardMorning = viewDay.findViewById(R.id.cardMorningTimeline);
            View cardEvening = viewDay.findViewById(R.id.cardEveningTimeline);
            TextView tvMorningBadge = viewDay.findViewById(R.id.tvMorningBadge);
            TextView tvEveningBadge = viewDay.findViewById(R.id.tvEveningBadge);
            LinearLayout layoutMorningSteps = viewDay.findViewById(R.id.layoutMorningSteps);
            LinearLayout layoutEveningSteps = viewDay.findViewById(R.id.layoutEveningSteps);
            TextView tvNoActivityMsg = viewDay.findViewById(R.id.tvNoActivityMsg);
            View layoutRewardPill = viewDay.findViewById(R.id.layoutRewardPill);

            Set<String> completedStepIds = ProfileSession.getCompletedStepIdsForDate(ctx, cellDateStr);
            boolean isMorningRewardAwarded = ProfileSession.isMorningRewardAwarded(ctx, cellDateStr);
            boolean isEveningRewardAwarded = ProfileSession.isEveningRewardAwarded(ctx, cellDateStr);

            boolean hasMorningCheckIn = morningDates.contains(cellDateStr) || isMorningRewardAwarded;
            boolean hasEveningCheckIn = eveningDates.contains(cellDateStr) || isEveningRewardAwarded;

            if (hasMorningCheckIn) {
                cardMorning.setVisibility(View.VISIBLE);
                if (isMorningRewardAwarded) {
                    tvMorningBadge.setText("HOÀN THÀNH");
                    tvMorningBadge.setBackgroundResource(R.drawable.bg_timeline_badge_completed);
                    tvMorningBadge.setTextColor(Color.parseColor("#3E4D44"));
                } else {
                    tvMorningBadge.setText("MỘT PHẦN");
                    tvMorningBadge.setBackgroundResource(R.drawable.bg_timeline_badge_partial);
                    tvMorningBadge.setTextColor(Color.parseColor("#F04758"));
                }

                layoutMorningSteps.removeAllViews();
                for (SkincareStep step : activeMorningStepsRaw) {
                    View stepView = LayoutInflater.from(ctx).inflate(R.layout.item_skin_timeline_step, layoutMorningSteps, false);
                    TextView tvName = stepView.findViewById(R.id.tvStepName);
                    ImageView imvCheck = stepView.findViewById(R.id.imvStepCheck);

                    tvName.setText(step.getName());
                    boolean isTicked = completedStepIds.contains("morning_" + step.getIndex());
                    if (isTicked) {
                        imvCheck.setImageResource(R.drawable.ic_circle_checked);
                        imvCheck.setImageTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
                        tvName.setTextColor(Color.parseColor("#3E4D44"));
                    } else {
                        imvCheck.setImageResource(R.drawable.ic_cancel);
                        imvCheck.setImageTintList(null);
                        tvName.setTextColor(Color.parseColor("#AEAEB2"));
                    }
                    layoutMorningSteps.addView(stepView);
                }
            } else {
                cardMorning.setVisibility(View.GONE);
            }

            if (hasEveningCheckIn) {
                cardEvening.setVisibility(View.VISIBLE);
                if (isEveningRewardAwarded) {
                    tvEveningBadge.setText("HOÀN THÀNH");
                    tvEveningBadge.setBackgroundResource(R.drawable.bg_timeline_badge_completed);
                    tvEveningBadge.setTextColor(Color.parseColor("#3E4D44"));
                } else {
                    tvEveningBadge.setText("MỘT PHẦN");
                    tvEveningBadge.setBackgroundResource(R.drawable.bg_timeline_badge_partial);
                    tvEveningBadge.setTextColor(Color.parseColor("#F04758"));
                }

                layoutEveningSteps.removeAllViews();
                for (SkincareStep step : activeEveningStepsRaw) {
                    View stepView = LayoutInflater.from(ctx).inflate(R.layout.item_skin_timeline_step, layoutEveningSteps, false);
                    TextView tvName = stepView.findViewById(R.id.tvStepName);
                    ImageView imvCheck = stepView.findViewById(R.id.imvStepCheck);

                    tvName.setText(step.getName());
                    boolean isTicked = completedStepIds.contains("evening_" + step.getIndex());
                    if (isTicked) {
                        imvCheck.setImageResource(R.drawable.ic_circle_checked);
                        imvCheck.setImageTintList(ColorStateList.valueOf(Color.parseColor("#3E4D44")));
                        tvName.setTextColor(Color.parseColor("#3E4D44"));
                    } else {
                        imvCheck.setImageResource(R.drawable.ic_cancel);
                        imvCheck.setImageTintList(null);
                        tvName.setTextColor(Color.parseColor("#AEAEB2"));
                    }
                    layoutEveningSteps.addView(stepView);
                }
            } else {
                cardEvening.setVisibility(View.GONE);
            }

            tvNoActivityMsg.setVisibility(!hasMorningCheckIn && !hasEveningCheckIn ? View.VISIBLE : View.GONE);
            layoutRewardPill.setVisibility(isMorningRewardAwarded && isEveningRewardAwarded ? View.VISIBLE : View.GONE);

            binding.layoutTimelineContainer.addView(viewDay);
            tempCal.add(Calendar.DAY_OF_YEAR, -1);
        }
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

    public static class SkincareStep {
        private int index;
        private String name;
        private String description;

        public SkincareStep(int index, String name, String description) {
            this.index = index;
            this.name = name;
            this.description = description;
        }

        public int getIndex() { return index; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
}
