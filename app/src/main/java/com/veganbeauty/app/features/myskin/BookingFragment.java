package com.veganbeauty.app.features.myskin;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingFragment extends RootieFragment {

    private String storeNameStr, storeAddressStr, storeImageUrlStr;
    private BookingService selectedService;
    private BookingDate selectedDate;
    private BookingTime selectedTime;
    private BookingTimeAdapter timeAdapter;
    private BookingDateAdapter dateAdapter;
    private RecyclerView rvDates;

    private final List<String> baseTimes = Arrays.asList("09:00","10:00","11:00","14:00","15:00","16:00","17:00","18:00");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static BookingFragment newInstance(String storeName, String storeAddress, String storeImageUrl) {
        Bundle args = new Bundle();
        args.putString("STORE_NAME", storeName);
        args.putString("STORE_ADDRESS", storeAddress);
        args.putString("STORE_IMAGE_URL", storeImageUrl);
        BookingFragment f = new BookingFragment();
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.skin_fragment_booking, container, false);
    }

    @Override
    public void setupUI(View view) {
        Bundle args = getArguments();
        storeNameStr = args != null ? args.getString("STORE_NAME", "Rootie Gò Vấp") : "Rootie Gò Vấp";
        storeAddressStr = args != null ? args.getString("STORE_ADDRESS", "27 Quang Trung, P.10, Gò Vấp") : "27 Quang Trung, P.10, Gò Vấp";
        storeImageUrlStr = args != null ? args.getString("STORE_IMAGE_URL", "") : "";

        TextView storeName = view.findViewById(R.id.booking_store_name);
        TextView storeAddress = view.findViewById(R.id.booking_store_address);
        ImageView storeImage = view.findViewById(R.id.booking_store_image);
        TextView btnChangeStore = view.findViewById(R.id.btn_change_store);
        ImageView btnBack = view.findViewById(R.id.btn_back);
        TextView btnConfirm = view.findViewById(R.id.btn_confirm_booking);
        ImageView btnCalendar = view.findViewById(R.id.booking_btn_calendar);
        RecyclerView rvServices = view.findViewById(R.id.rv_services);
        rvDates = view.findViewById(R.id.rv_dates);
        RecyclerView rvTimes = view.findViewById(R.id.rv_times);

        storeName.setText(storeNameStr);
        storeAddress.setText(storeAddressStr);
        if (!storeImageUrlStr.isEmpty()) {
            com.bumptech.glide.Glide.with(requireContext())
                    .load(storeImageUrlStr)
                    .placeholder(R.drawable.imv_logo)
                    .error(R.drawable.imv_logo)
                    .into(storeImage);
        } else {
            storeImage.setImageResource(R.drawable.imv_logo);
        }

        List<BookingService> mockServices = Arrays.asList(
                new BookingService("1", "Soi da cơ bản", "30 phút * Miễn phí", "30 phút"),
                new BookingService("2", "Soi da chuyên sâu", "45 phút * 199.000 đ", "45 phút"),
                new BookingService("3", "Soi da & tư vấn 1:1", "60 phút * 299.000 đ", "60 phút")
        );
        rvServices.setLayoutManager(new LinearLayoutManager(getContext()));
        rvServices.setAdapter(new BookingServiceAdapter(mockServices, service -> selectedService = service));

        timeAdapter = new BookingTimeAdapter(new ArrayList<>(), time -> selectedTime = time);
        rvTimes.setLayoutManager(new GridLayoutManager(getContext(), 4));
        rvTimes.setAdapter(timeAdapter);

        setupDateList(Calendar.getInstance());

        btnCalendar.setOnClickListener(v -> {
            Calendar currentCal = Calendar.getInstance();
            Calendar maxCal = Calendar.getInstance();
            maxCal.add(Calendar.MONTH, 2);
            DatePickerDialog picker = new DatePickerDialog(requireContext(), android.R.style.Theme_DeviceDefault_Light_Dialog_Alert,
                    (dp, year, month, day) -> {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(year, month, day);
                        setupDateList(chosen);
                    },
                    currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH));
            picker.getDatePicker().setMinDate(currentCal.getTimeInMillis());
            picker.getDatePicker().setMaxDate(maxCal.getTimeInMillis());
            picker.show();
            if (picker.getButton(DatePickerDialog.BUTTON_POSITIVE) != null)
                picker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.primary, null));
            if (picker.getButton(DatePickerDialog.BUTTON_NEGATIVE) != null)
                picker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.content, null));
        });

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnChangeStore.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnConfirm.setOnClickListener(v -> {
            if (selectedService == null || selectedDate == null || selectedTime == null) {
                Toast.makeText(getContext(), "Vui lòng chọn đầy đủ Dịch vụ, Ngày và Giờ", Toast.LENGTH_SHORT).show();
                return;
            }
            showConfirmationDialog();
        });
    }

    private void setupDateList(Calendar startCal) {
        List<BookingDate> dates = new ArrayList<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar tempCal = (Calendar) startCal.clone();
        for (int i = 0; i < 5; i++) {
            String dow;
            switch (tempCal.get(Calendar.DAY_OF_WEEK)) {
                case Calendar.MONDAY: dow = "Thứ 2"; break;
                case Calendar.TUESDAY: dow = "Thứ 3"; break;
                case Calendar.WEDNESDAY: dow = "Thứ 4"; break;
                case Calendar.THURSDAY: dow = "Thứ 5"; break;
                case Calendar.FRIDAY: dow = "Thứ 6"; break;
                case Calendar.SATURDAY: dow = "Thứ 7"; break;
                case Calendar.SUNDAY: dow = "CN"; break;
                default: dow = ""; break;
            }
            Calendar cloned = (Calendar) tempCal.clone();
            dates.add(new BookingDate(String.valueOf(i + 1), dow, dayFormat.format(tempCal.getTime()), cloned));
            tempCal.add(Calendar.DAY_OF_MONTH, 1);
        }
        selectedDate = null;
        selectedTime = null;
        updateTimeSlots(null);

        dateAdapter = new BookingDateAdapter(dates, date -> {
            selectedDate = date;
            selectedTime = null;
            updateTimeSlots(date.getFullDate());
        });
        if (rvDates.getLayoutManager() == null)
            rvDates.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvDates.setAdapter(dateAdapter);
    }

    private void updateTimeSlots(Calendar selectedCal) {
        List<BookingTime> times = new ArrayList<>();
        if (selectedCal == null) {
            for (int i = 0; i < baseTimes.size(); i++)
                times.add(new BookingTime(String.valueOf(i + 1), baseTimes.get(i), false));
        } else {
            Calendar now = Calendar.getInstance();
            boolean isToday = now.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)
                    && now.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR);
            int curH = now.get(Calendar.HOUR_OF_DAY);
            for (int i = 0; i < baseTimes.size(); i++) {
                int slotHour = Integer.parseInt(baseTimes.get(i).split(":")[0]);
                boolean locked = isToday && slotHour <= curH;
                times.add(new BookingTime(String.valueOf(i + 1), baseTimes.get(i), locked));
            }
        }
        timeAdapter.updateData(times);
    }

    private void showConfirmationDialog() {
        if (selectedDate == null || selectedTime == null || selectedService == null) return;
        int year = selectedDate.getFullDate() != null ? selectedDate.getFullDate().get(Calendar.YEAR) : 2026;
        String msg = "Bạn có chắc chắn muốn đặt lịch:\n\n" +
                "Dịch vụ: " + selectedService.getName() + "\n" +
                "Thời gian: " + selectedTime.getTime() + " - " + selectedDate.getDayOfWeek() + ", " + selectedDate.getDate() + "/" + year + "\n" +
                "Chi nhánh: " + storeNameStr;

        new AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle("Xác nhận đặt lịch")
                .setMessage(msg)
                .setPositiveButton("Đồng ý", (d, w) -> completeBooking())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void completeBooking() {
        if (selectedDate == null || selectedTime == null) return;
        int year = selectedDate.getFullDate() != null ? selectedDate.getFullDate().get(Calendar.YEAR) : 2026;
        String dateTime = selectedDate.getDate() + "/" + year + " - " + selectedTime.getTime();
        String service = selectedService != null ? selectedService.getName() : "";
        String specialist = "Nguyễn Khánh Xuân";
        SimpleDateFormat monthFmt = new SimpleDateFormat("MM", Locale.getDefault());
        String monthDisplay = "Tháng " + monthFmt.format(selectedDate.getFullDate() != null ? selectedDate.getFullDate().getTime() : new Date());
        String userId = ProfileSession.getUserId(requireContext());
        String isoTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        BookingHistoryEntity booking = new BookingHistoryEntity(
                "RS" + String.valueOf(System.currentTimeMillis()).substring(String.valueOf(System.currentTimeMillis()).length() - 8),
                userId,
                ProfileSession.getFullName(requireContext()),
                ProfileSession.getPhone(requireContext()),
                ProfileSession.getEmail(requireContext()),
                service,
                selectedDate.getDate() + "/" + year,
                selectedDate.getDayOfWeek(),
                selectedTime.getTime(),
                selectedService != null ? selectedService.getDuration() : "",
                storeNameStr, storeAddressStr,
                "Chờ xác nhận"
        );
        booking.setMonthDisplay(monthDisplay);
        booking.setStorePhone("1900 1234");
        booking.setStoreImage(storeImageUrlStr);
        booking.setCreatedAt(isoTime);
        booking.setConsultantName(specialist);
        booking.setConsultantAvatar("https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg");
        booking.setConsultantRating(5.0f);

        executor.execute(() -> {
            new FirestoreService().addBooking(booking);
            new LocalJsonReader(requireContext()).addBooking(booking);
            new FirestoreService().uploadBooking(booking);
        });

        BookingSuccessFragment successFragment = BookingSuccessFragment.newInstance(storeNameStr, dateTime, specialist, service);
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.slide_out_right)
                .replace(R.id.main_container, successFragment)
                .addToBackStack(null).commit();
    }

    @Override
    public void observeViewModel() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
