package com.veganbeauty.app.features.account.checkin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.databinding.AccountCheckinFragmentBinding;
import com.veganbeauty.app.databinding.ItemCalendarDayBinding;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;
import com.veganbeauty.app.utils.SyncDataHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.lifecycle.FlowLiveDataConversions;

public class AccountCheckinFragment extends RootieFragment {

    private AccountCheckinFragmentBinding binding;
    private RootieDatabase db;
    private Calendar calendarInstance = Calendar.getInstance();
    private Set<String> checkedInDates = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountCheckinFragmentBinding.inflate(inflater, container, false);
        db = RootieDatabase.getDatabase(requireContext());
        return binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.layoutNotification.getRoot().setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountNotificationFragment())
                .addToBackStack(null)
                .commit());

        binding.btnPrevMonth.setOnClickListener(v -> {
            calendarInstance.add(Calendar.MONTH, -1);
            refreshUI();
        });

        binding.btnNextMonth.setOnClickListener(v -> {
            calendarInstance.add(Calendar.MONTH, 1);
            refreshUI();
        });

        View.OnClickListener onCheckInClick = v -> performCheckIn();
        binding.btnBannerCheckin.setOnClickListener(onCheckInClick);
        binding.btnBottomCheckin.setOnClickListener(onCheckInClick);

        refreshUI();
    }

    @Override
    protected void observeViewModel() {
        FlowLiveDataConversions.asLiveData(db.rewardPointDao().getAllRewardHistory())
                .observe(getViewLifecycleOwner(), this::updateFromHistory);
    }

    private void updateFromHistory(List<RewardPointEntity> allHistory) {
        if (binding == null || !isAdded()) return;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Set<String> newCheckedInDates = new HashSet<>();
        if (allHistory != null) {
            for (RewardPointEntity item : allHistory) {
                if (item.getReason() != null && item.getReason().contains("Điểm danh")) {
                    newCheckedInDates.add(sdf.format(new Date(item.getTimestamp())));
                }
            }
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("checkin_prefs", Context.MODE_PRIVATE);
        Set<String> savedDates = prefs.getStringSet("checked_in_dates", new HashSet<>());
        if (savedDates != null) {
            newCheckedInDates.addAll(savedDates);
        }

        checkedInDates.clear();
        checkedInDates.addAll(newCheckedInDates);
        refreshUI();
    }

    private void refreshUI() {
        if (binding == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int month = calendarInstance.get(Calendar.MONTH) + 1;
        int year = calendarInstance.get(Calendar.YEAR);
        binding.tvCalendarMonth.setText("Tháng " + month + ", " + year);

        updateCalendarGrid(checkedInDates);

        int streak = calculateStreak(checkedInDates);
        int displayStreak = (streak == 0) ? 0 : ((streak - 1) % 7) + 1;
        binding.tvStreakCount.setText(displayStreak + "/7");

        highlightRewardCard(streak);

        String todayStr = sdf.format(new Date());
        boolean hasCheckedInToday = checkedInDates.contains(todayStr);

        updateEncouragementText(streak, hasCheckedInToday, displayStreak);

        if (hasCheckedInToday) {
            binding.ivBannerIcon.setImageResource(R.drawable.ic_check);
            binding.ivBannerIcon.setColorFilter(Color.parseColor("#879578"));
            binding.tvBannerText.setText("Đã check-in hôm nay");
            binding.tvBannerText.setTextColor(Color.parseColor("#879578"));
            binding.btnBannerCheckin.setVisibility(View.GONE);
            binding.tvBannerCompletedText.setVisibility(View.VISIBLE);

            binding.btnBottomCheckin.setEnabled(false);
            binding.btnBottomCheckin.setText("Đã điểm danh hôm nay");
            binding.btnBottomCheckin.setBackgroundResource(R.drawable.bg_pill_grey);
            binding.btnBottomCheckin.setTextColor(Color.parseColor("#888888"));
        } else {
            binding.ivBannerIcon.setImageResource(R.drawable.ic_info_olive);
            binding.ivBannerIcon.setColorFilter(Color.parseColor("#8A9A3D"));
            binding.tvBannerText.setText("Chưa check-in hôm nay");
            binding.tvBannerText.setTextColor(Color.parseColor("#8A9A3D"));
            binding.btnBannerCheckin.setVisibility(View.VISIBLE);
            binding.tvBannerCompletedText.setVisibility(View.GONE);

            binding.btnBottomCheckin.setEnabled(true);
            binding.btnBottomCheckin.setText("Check-in ngay");
            binding.btnBottomCheckin.setBackgroundResource(R.drawable.bg_btn_checkin_bottom);
            binding.btnBottomCheckin.setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

    private void updateEncouragementText(int streak, boolean hasCheckedInToday, int displayStreak) {
        String encouragementText;
        if (!hasCheckedInToday) {
            int nextStreakMod = (streak % 7) + 1;
            if (nextStreakMod == 3) {
                encouragementText = "Điểm danh hôm nay để nhận mốc thưởng +50 xu!";
            } else if (nextStreakMod == 7) {
                encouragementText = "Điểm danh hôm nay để nhận mốc thưởng +200 xu!";
            } else if (nextStreakMod < 3) {
                encouragementText = "Điểm danh hôm nay. Thêm " + (3 - nextStreakMod) + " ngày nữa để nhận +50 xu!";
            } else {
                encouragementText = "Điểm danh hôm nay. Thêm " + (7 - nextStreakMod) + " ngày nữa để nhận +200 xu!";
            }
        } else {
            if (displayStreak == 3) {
                encouragementText = "Hôm nay bạn đã nhận mốc thưởng +50 xu! Thêm 4 ngày nữa để nhận +200 xu!";
            } else if (displayStreak == 7) {
                encouragementText = "Chúc mừng bạn đã hoàn thành chuỗi tuần này! Hãy duy trì vào ngày mai nhé!";
            } else if (displayStreak >= 1 && displayStreak <= 2) {
                encouragementText = "Điểm danh thêm " + (3 - displayStreak) + " ngày nữa để nhận mốc thưởng +50 xu!";
            } else {
                encouragementText = "Điểm danh thêm " + (7 - displayStreak) + " ngày nữa để nhận +200 xu!";
            }
        }
        binding.tvStreakEncouragement.setText(encouragementText);
    }

    private void updateCalendarGrid(Set<String> checkedInDates) {
        List<CalendarDay> days = new ArrayList<>();
        Calendar cal = (Calendar) calendarInstance.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int dayOfWeekFirst = cal.get(Calendar.DAY_OF_WEEK);
        int offset = (dayOfWeekFirst + 5) % 7;

        for (int i = 0; i < offset; i++) {
            days.add(new CalendarDay(0, false, null, false, false));
        }

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());

        for (int i = 1; i <= daysInMonth; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);
            String dateStr = sdf.format(cal.getTime());
            boolean isChecked = checkedInDates.contains(dateStr);
            boolean isToday = dateStr.equals(todayStr);
            days.add(new CalendarDay(i, true, cal.getTime(), isChecked, isToday));
        }

        binding.rvCalendarDays.setAdapter(new CalendarAdapter(days, day -> {
            if (day.isToday && !day.isChecked) {
                performCheckIn();
            } else if (day.isChecked) {
                Toast.makeText(getContext(), "Bạn đã điểm danh ngày này!", Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private int calculateStreak(Set<String> checkedInDates) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar todayCal = Calendar.getInstance();
        String todayStr = sdf.format(todayCal.getTime());

        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayStr = sdf.format(yesterdayCal.getTime());

        int streak = 0;
        Calendar checkCal = Calendar.getInstance();

        if (checkedInDates.contains(todayStr)) {
            while (checkedInDates.contains(sdf.format(checkCal.getTime()))) {
                streak++;
                checkCal.add(Calendar.DAY_OF_YEAR, -1);
            }
        } else if (checkedInDates.contains(yesterdayStr)) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1);
            while (checkedInDates.contains(sdf.format(checkCal.getTime()))) {
                streak++;
                checkCal.add(Calendar.DAY_OF_YEAR, -1);
            }
        }
        return streak;
    }

    private void highlightRewardCard(int streak) {
        int currentMod = (streak == 0) ? 0 : ((streak - 1) % 7) + 1;
        binding.cardReward1.setBackgroundResource(R.drawable.bg_reward_card_default);
        binding.cardReward2.setBackgroundResource(R.drawable.bg_reward_card_default);
        binding.cardReward3.setBackgroundResource(R.drawable.bg_reward_card_default);

        if (currentMod == 7) {
            binding.cardReward3.setBackgroundResource(R.drawable.bg_reward_card_selected);
        } else if (currentMod >= 3) {
            binding.cardReward2.setBackgroundResource(R.drawable.bg_reward_card_selected);
        } else {
            binding.cardReward1.setBackgroundResource(R.drawable.bg_reward_card_selected);
        }
    }

    private void performCheckIn() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());

        if (checkedInDates.contains(todayStr)) {
            Toast.makeText(requireContext(), "Bạn đã điểm danh hôm nay rồi!", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterdayStr = sdf.format(yesterdayCal.getTime());

        boolean isYesterdayChecked = checkedInDates.contains(yesterdayStr);
        int currentStreak = calculateStreak(checkedInDates);
        int newStreak = isYesterdayChecked ? currentStreak + 1 : 1;

        int pointsAwarded;
        if (newStreak % 7 == 0) {
            pointsAwarded = 200;
        } else if (newStreak % 7 == 3) {
            pointsAwarded = 50;
        } else {
            pointsAwarded = 10;
        }

        new Thread(() -> {
            try {
                SimpleDateFormat reasonSdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                db.rewardPointDao().insertRewardPoints(new RewardPointEntity(
                        0,
                        "DAILY_CHECKIN",
                        pointsAwarded,
                        "Điểm danh hàng ngày (" + reasonSdf.format(new Date()) + ")",
                        System.currentTimeMillis()
                ));
                SyncDataHelper.syncRewardPointsToFirestore(requireContext());

                SharedPreferences prefs = requireContext().getSharedPreferences("checkin_prefs", Context.MODE_PRIVATE);
                Set<String> savedDates = prefs.getStringSet("checked_in_dates", new HashSet<>());
                Set<String> dates = new HashSet<>(savedDates != null ? savedDates : new HashSet<>());
                dates.add(todayStr);
                prefs.edit().putStringSet("checked_in_dates", dates).apply();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showSuccessDialog(pointsAwarded);
                        refreshUI();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showSuccessDialog(int points) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_checkin_success, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        TextView tvDialogPoints = dialogView.findViewById(R.id.tvDialogPoints);
        if (tvDialogPoints != null) tvDialogPoints.setText("+" + points + " xu");

        View btnDialogDismiss = dialogView.findViewById(R.id.btnDialogDismiss);
        if (btnDialogDismiss != null) btnDialogDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class CalendarDay {
        public final int dayNum;
        public final boolean isCurrentMonth;
        public final Date date;
        public final boolean isChecked;
        public final boolean isToday;

        public CalendarDay(int dayNum, boolean isCurrentMonth, Date date, boolean isChecked, boolean isToday) {
            this.dayNum = dayNum;
            this.isCurrentMonth = isCurrentMonth;
            this.date = date;
            this.isChecked = isChecked;
            this.isToday = isToday;
        }
    }

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private final List<CalendarDay> days;
        private final OnDayClickListener onDayClick;

        public CalendarAdapter(List<CalendarDay> days, OnDayClickListener onDayClick) {
            this.days = days;
            this.onDayClick = onDayClick;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final ItemCalendarDayBinding binding;
            public ViewHolder(ItemCalendarDayBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCalendarDayBinding binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CalendarDay day = days.get(position);
            if (day.dayNum == 0) {
                holder.binding.tvDayNum.setText("");
                holder.binding.viewCheckedBg.setVisibility(View.GONE);
                holder.binding.viewTodayBorder.setVisibility(View.GONE);
                holder.binding.ivTickBadge.setVisibility(View.GONE);
                holder.binding.getRoot().setOnClickListener(null);
            } else {
                holder.binding.tvDayNum.setText(String.valueOf(day.dayNum));

                if (day.isChecked) {
                    holder.binding.viewCheckedBg.setVisibility(View.VISIBLE);
                    holder.binding.ivTickBadge.setVisibility(View.VISIBLE);
                    holder.binding.viewTodayBorder.setVisibility(View.GONE);
                    holder.binding.tvDayNum.setTextColor(Color.parseColor("#3E4D44"));
                } else if (day.isToday) {
                    holder.binding.viewCheckedBg.setVisibility(View.GONE);
                    holder.binding.ivTickBadge.setVisibility(View.GONE);
                    holder.binding.viewTodayBorder.setVisibility(View.VISIBLE);
                    holder.binding.tvDayNum.setTextColor(Color.parseColor("#E05D3B"));
                } else {
                    holder.binding.viewCheckedBg.setVisibility(View.GONE);
                    holder.binding.ivTickBadge.setVisibility(View.GONE);
                    holder.binding.viewTodayBorder.setVisibility(View.GONE);
                    holder.binding.tvDayNum.setTextColor(Color.parseColor("#333333"));
                }

                holder.binding.getRoot().setOnClickListener(v -> onDayClick.onDayClick(day));
            }
        }

        @Override
        public int getItemCount() {
            return days.size();
        }
    }
}
