package com.veganbeauty.app.features.myskin;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BookingCalendarBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "BookingCalendarBottomSheet";

    public interface OnDateSelectedListener {
        void onDateSelected(Calendar calendar);
    }

    private OnDateSelectedListener listener;
    private Calendar calendarInstance = Calendar.getInstance();
    private Calendar selectedCalendar;
    private Calendar minCalendar;
    private Calendar maxCalendar;
    private RecyclerView rvCalendarDays;
    private TextView tvCalendarMonth;
    private BookingCalendarDayAdapter dayAdapter;

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    public void setInitialDate(Calendar initial) {
        if (initial != null) {
            calendarInstance = (Calendar) initial.clone();
            selectedCalendar = (Calendar) initial.clone();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        minCalendar = Calendar.getInstance();
        maxCalendar = Calendar.getInstance();
        maxCalendar.add(Calendar.MONTH, 2);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.skin_bottom_sheet_booking_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCalendarDays = view.findViewById(R.id.rv_calendar_days);
        tvCalendarMonth = view.findViewById(R.id.tv_calendar_month);

        rvCalendarDays.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        dayAdapter = new BookingCalendarDayAdapter(new ArrayList<>(), this::onDayClicked);
        rvCalendarDays.setAdapter(dayAdapter);

        view.findViewById(R.id.btn_close_calendar_sheet).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            calendarInstance.add(Calendar.MONTH, -1);
            refreshCalendar();
        });
        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            calendarInstance.add(Calendar.MONTH, 1);
            refreshCalendar();
        });
        view.findViewById(R.id.btn_confirm_calendar).setOnClickListener(v -> {
            if (selectedCalendar == null) {
                Toast.makeText(requireContext(), "Vui lòng chọn một ngày", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) {
                listener.onDateSelected(selectedCalendar);
            }
            dismiss();
        });

        refreshCalendar();
    }

    private void onDayClicked(BookingCalendarDay day) {
        if (!day.isValid || day.isDisabled) return;
        selectedCalendar = (Calendar) day.calendar.clone();
        refreshCalendar();
    }

    private void refreshCalendar() {
        int month = calendarInstance.get(Calendar.MONTH) + 1;
        int year = calendarInstance.get(Calendar.YEAR);
        tvCalendarMonth.setText(String.format(Locale.getDefault(), "Tháng %d, %d", month, year));
        dayAdapter.updateDays(buildDays());
    }

    private List<BookingCalendarDay> buildDays() {
        List<BookingCalendarDay> days = new ArrayList<>();
        Calendar cal = (Calendar) calendarInstance.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int dayOfWeekFirst = cal.get(Calendar.DAY_OF_WEEK);
        int offset = (dayOfWeekFirst + 5) % 7;
        for (int i = 0; i < offset; i++) {
            days.add(new BookingCalendarDay(0, false, null, false, false, false));
        }

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(today.getTime());

        for (int i = 1; i <= daysInMonth; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);
            Calendar dayCal = (Calendar) cal.clone();
            String dateStr = sdf.format(dayCal.getTime());
            boolean isToday = dateStr.equals(todayStr);
            boolean isSelected = selectedCalendar != null
                    && selectedCalendar.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR)
                    && selectedCalendar.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR);
            boolean isDisabled = dayCal.before(stripTime(minCalendar)) || dayCal.after(stripTime(maxCalendar));
            days.add(new BookingCalendarDay(i, true, dayCal, isToday, isSelected, isDisabled));
        }
        return days;
    }

    private Calendar stripTime(Calendar cal) {
        Calendar stripped = (Calendar) cal.clone();
        stripped.set(Calendar.HOUR_OF_DAY, 0);
        stripped.set(Calendar.MINUTE, 0);
        stripped.set(Calendar.SECOND, 0);
        stripped.set(Calendar.MILLISECOND, 0);
        return stripped;
    }

    static class BookingCalendarDay {
        final int dayNum;
        final boolean isValid;
        final Calendar calendar;
        final boolean isToday;
        final boolean isSelected;
        final boolean isDisabled;

        BookingCalendarDay(int dayNum, boolean isValid, Calendar calendar,
                           boolean isToday, boolean isSelected, boolean isDisabled) {
            this.dayNum = dayNum;
            this.isValid = isValid;
            this.calendar = calendar;
            this.isToday = isToday;
            this.isSelected = isSelected;
            this.isDisabled = isDisabled;
        }
    }

    static class BookingCalendarDayAdapter extends RecyclerView.Adapter<BookingCalendarDayAdapter.ViewHolder> {

        interface OnDayClickListener {
            void onDayClick(BookingCalendarDay day);
        }

        private List<BookingCalendarDay> days;
        private final OnDayClickListener listener;

        BookingCalendarDayAdapter(List<BookingCalendarDay> days, OnDayClickListener listener) {
            this.days = days;
            this.listener = listener;
        }

        void updateDays(List<BookingCalendarDay> newDays) {
            this.days = newDays;
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final View container;
            final View bg;
            final TextView tvDay;

            ViewHolder(View itemView) {
                super(itemView);
                container = itemView.findViewById(R.id.calendar_day_container);
                bg = itemView.findViewById(R.id.calendar_day_bg);
                tvDay = itemView.findViewById(R.id.tv_calendar_day);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.skin_item_booking_calendar_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookingCalendarDay day = days.get(position);
            if (!day.isValid) {
                holder.tvDay.setText("");
                holder.bg.setVisibility(View.GONE);
                holder.container.setOnClickListener(null);
                holder.container.setClickable(false);
                return;
            }

            holder.tvDay.setText(String.valueOf(day.dayNum));
            holder.container.setClickable(!day.isDisabled);

            if (day.isDisabled) {
                holder.bg.setVisibility(View.GONE);
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.gray_light));
                holder.container.setAlpha(0.45f);
            } else if (day.isSelected) {
                holder.bg.setVisibility(View.VISIBLE);
                holder.bg.setBackgroundResource(R.drawable.skin_bg_booking_calendar_day_selected);
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
                holder.tvDay.setTypeface(holder.tvDay.getTypeface(), Typeface.BOLD);
                holder.container.setAlpha(1f);
            } else if (day.isToday) {
                holder.bg.setVisibility(View.VISIBLE);
                holder.bg.setBackgroundResource(R.drawable.skin_bg_booking_calendar_day_today);
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
                holder.tvDay.setTypeface(holder.tvDay.getTypeface(), Typeface.BOLD);
                holder.container.setAlpha(1f);
            } else {
                holder.bg.setVisibility(View.GONE);
                holder.tvDay.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
                holder.tvDay.setTypeface(Typeface.DEFAULT);
                holder.container.setAlpha(1f);
            }

            holder.container.setOnClickListener(v -> {
                if (listener != null) listener.onDayClick(day);
            });
        }

        @Override
        public int getItemCount() {
            return days != null ? days.size() : 0;
        }
    }
}
