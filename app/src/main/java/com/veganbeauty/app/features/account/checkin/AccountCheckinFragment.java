package com.veganbeauty.app.features.account.checkin;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.databinding.AccountCheckinFragmentBinding;
import com.veganbeauty.app.databinding.ItemCalendarDayBinding;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;
import com.veganbeauty.app.utils.CoinRewardDialogHelper;
import com.veganbeauty.app.features.myskin.SkinDetailHeaderScrollHelper;
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
    private SkinDetailHeaderScrollHelper headerScrollHelper;
    private int bottomNavInsetPx;
    private static final int BOTTOM_NAV_BAR_DP = 56;
    private static final int BOTTOM_BUTTON_AREA_DP = 68;
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

        setupScrollHideHeader();
        setupBottomInsets();
        refreshUI();
    }

    private void setupBottomInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            bottomNavInsetPx = navBars.bottom;
            applyBottomSpacing();
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());
    }

    private void applyBottomSpacing() {
        if (binding == null) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        int navBarPx = (int) (BOTTOM_NAV_BAR_DP * density);
        int buttonAreaPx = (int) (BOTTOM_BUTTON_AREA_DP * density);

        ViewGroup.MarginLayoutParams buttonParams =
                (ViewGroup.MarginLayoutParams) binding.layoutBottomButton.getLayoutParams();
        buttonParams.bottomMargin = navBarPx + bottomNavInsetPx;
        binding.layoutBottomButton.setLayoutParams(buttonParams);

        updateScrollBottomPadding(navBarPx, buttonAreaPx);
    }

    private void updateScrollBottomPadding(int navBarPx, int buttonAreaPx) {
        int bottomPadding = navBarPx + bottomNavInsetPx + (int) (16 * getResources().getDisplayMetrics().density);
        if (binding.layoutBottomButton.getVisibility() == View.VISIBLE) {
            bottomPadding += buttonAreaPx;
        }
        binding.checkinScroll.setPadding(
                binding.checkinScroll.getPaddingLeft(),
                binding.checkinScroll.getPaddingTop(),
                binding.checkinScroll.getPaddingRight(),
                bottomPadding
        );
        if (headerScrollHelper != null) {
            headerScrollHelper.setBottomPaddingPx(bottomPadding);
        }
    }

    private void setupScrollHideHeader() {
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                binding.topBar,
                binding.checkinScroll,
                0
        );
        headerScrollHelper.attachToNestedScrollView(binding.checkinScroll);
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

            binding.layoutBottomButton.setVisibility(View.GONE);
            applyBottomSpacing();
        } else {
            binding.ivBannerIcon.setImageResource(R.drawable.ic_warning_outline);
            binding.ivBannerIcon.setColorFilter(Color.parseColor("#8A9A3D"));
            binding.tvBannerText.setText("Chưa check-in hôm nay");
            binding.tvBannerText.setTextColor(Color.parseColor("#8A9A3D"));
            binding.btnBannerCheckin.setVisibility(View.VISIBLE);
            binding.tvBannerCompletedText.setVisibility(View.GONE);

            binding.layoutBottomButton.setVisibility(View.VISIBLE);
            binding.btnBottomCheckin.setEnabled(true);
            binding.btnBottomCheckin.setText("Điểm danh ngay");
            binding.btnBottomCheckin.setBackgroundResource(R.drawable.skin_bg_btn_green);
            binding.btnBottomCheckin.setTextColor(Color.WHITE);
            applyCheckinButtonIcon(binding.btnBottomCheckin, R.drawable.ic_hand_tap);
            applyBottomSpacing();
        }
    }

    private void applyCheckinButtonIcon(AppCompatButton button, @DrawableRes int iconRes) {
        Drawable icon = ContextCompat.getDrawable(requireContext(), iconRes);
        if (icon == null) {
            button.setCompoundDrawables(null, null, null, null);
            return;
        }
        icon = icon.mutate();
        int sizePx = (int) (20 * getResources().getDisplayMetrics().density);
        icon.setBounds(0, 0, sizePx, sizePx);
        icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        button.setCompoundDrawables(icon, null, null, null);
        button.setCompoundDrawablePadding((int) (8 * getResources().getDisplayMetrics().density));
    }

    private void setRewardIconStyle(FrameLayout container, ImageView icon, boolean active, boolean isCoinIcon) {
        if (active) {
            container.setBackgroundResource(R.drawable.skin_bg_reward_icon_completed);
            if (isCoinIcon) {
                icon.clearColorFilter();
            } else {
                icon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.white),
                        PorterDuff.Mode.SRC_IN
                );
            }
        } else {
            container.setBackgroundResource(R.drawable.skin_bg_reward_icon_pending);
            if (isCoinIcon) {
                icon.clearColorFilter();
            } else {
                icon.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.primary),
                        PorterDuff.Mode.SRC_IN
                );
            }
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
        binding.cardReward1.setBackgroundResource(R.drawable.bg_dialog_qr);
        binding.cardReward2.setBackgroundResource(R.drawable.bg_dialog_qr);
        binding.cardReward3.setBackgroundResource(R.drawable.bg_dialog_qr);

        setRewardIconStyle(binding.flRewardIcon1, binding.ivRewardIcon1, false, true);
        setRewardIconStyle(binding.flRewardIcon2, binding.ivRewardIcon2, false, false);
        setRewardIconStyle(binding.flRewardIcon3, binding.ivRewardIcon3, false, false);

        if (currentMod == 7) {
            binding.cardReward3.setBackgroundResource(R.drawable.bg_reward_card_selected);
            setRewardIconStyle(binding.flRewardIcon3, binding.ivRewardIcon3, true, false);
        } else if (currentMod >= 3) {
            binding.cardReward2.setBackgroundResource(R.drawable.bg_reward_card_selected);
            setRewardIconStyle(binding.flRewardIcon2, binding.ivRewardIcon2, true, false);
        } else {
            binding.cardReward1.setBackgroundResource(R.drawable.bg_reward_card_selected);
            setRewardIconStyle(binding.flRewardIcon1, binding.ivRewardIcon1, true, true);
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
        CoinRewardDialogHelper.show(this, points, "từ điểm danh hàng ngày");
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
