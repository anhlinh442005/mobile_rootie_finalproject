package com.veganbeauty.app.features.myskin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.StoreEntity;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BookingFilterBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "BookingFilterBottomSheet";

    public interface OnFilterAppliedListener {
        void onFilterApplied(String serviceName, String storeName,
                             String date, String time, String month);
    }

    private OnFilterAppliedListener listener;
    private EditText edtServiceName, edtStoreName, edtDate, edtTime, edtMonth;

    private String initServiceName = "";
    private String initStoreName = "";
    private String initDate = "";
    private String initTime = "";
    private String initMonth = "";

    private String[] storeNames = {};

    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    public void setInitialValues(String serviceName, String storeName,
                                  String date, String time, String month) {
        this.initServiceName = serviceName != null ? serviceName : "";
        this.initStoreName = storeName != null ? storeName : "";
        this.initDate = date != null ? date : "";
        this.initTime = time != null ? time : "";
        this.initMonth = month != null ? month : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.skin_bottom_sheet_booking_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtServiceName = view.findViewById(R.id.edt_service_name);
        edtStoreName = view.findViewById(R.id.edt_store_name);
        edtDate = view.findViewById(R.id.edt_date);
        edtTime = view.findViewById(R.id.edt_time);
        edtMonth = view.findViewById(R.id.edt_month);

        edtServiceName.setText(initServiceName);
        edtStoreName.setText(initStoreName);
        edtDate.setText(initDate);
        edtTime.setText(initTime);
        edtMonth.setText(initMonth);

        loadStoreNamesFromData();
        setupStoreDropdown();
        setupDatePicker();
        setupTimePicker();
        setupMonthPicker();

        view.findViewById(R.id.btn_clear_all).setOnClickListener(v -> clearAll());
        view.findViewById(R.id.btn_reset).setOnClickListener(v -> clearAll());
        view.findViewById(R.id.btn_apply).setOnClickListener(v -> applyFilter());
    }

    private void loadStoreNamesFromData() {
        try {
            LocalJsonReader reader = new LocalJsonReader(requireContext());
            List<StoreEntity> stores = reader.getAllStores();
            if (stores != null && !stores.isEmpty()) {
                storeNames = new String[stores.size()];
                for (int i = 0; i < stores.size(); i++) {
                    storeNames[i] = stores.get(i).getTenCuaHang();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupStoreDropdown() {
        edtStoreName.setOnClickListener(v -> {
            if (storeNames.length == 0) return;
            new AlertDialog.Builder(requireContext())
                    .setTitle("Chọn chi nhánh")
                    .setItems(storeNames, (dialog, which) ->
                            edtStoreName.setText(storeNames[which]))
                    .show();
        });
    }

    private void setupDatePicker() {
        edtDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
                String dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
                edtDate.setText(dateStr);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupTimePicker() {
        String[] timeSlots = {
                "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
                "11:00", "11:30", "13:00", "13:30", "14:00", "14:30",
                "15:00", "15:30", "16:00", "16:30", "17:00", "17:30"
        };
        edtTime.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Chọn giờ")
                        .setItems(timeSlots, (dialog, which) ->
                                edtTime.setText(timeSlots[which]))
                        .show());
    }

    private void setupMonthPicker() {
        String[] months = {
                "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
                "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8",
                "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        };
        edtMonth.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Chọn tháng")
                        .setItems(months, (dialog, which) ->
                                edtMonth.setText(months[which]))
                        .show());
    }

    private void clearAll() {
        edtServiceName.setText("");
        edtStoreName.setText("");
        edtDate.setText("");
        edtTime.setText("");
        edtMonth.setText("");
    }

    private void applyFilter() {
        if (listener != null) {
            listener.onFilterApplied(
                    edtServiceName.getText().toString().trim(),
                    edtStoreName.getText().toString().trim(),
                    edtDate.getText().toString().trim(),
                    edtTime.getText().toString().trim(),
                    edtMonth.getText().toString().trim()
            );
        }
        dismiss();
    }
}
