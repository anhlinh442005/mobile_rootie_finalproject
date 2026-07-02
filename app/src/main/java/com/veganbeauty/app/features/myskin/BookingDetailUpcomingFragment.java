package com.veganbeauty.app.features.myskin;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.databinding.SkinFragmentBookingDetailUpcomingBinding;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingDetailUpcomingFragment extends RootieFragment {

    private SkinFragmentBookingDetailUpcomingBinding _binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private static BookingHistoryEntity bookingData = null;

    public static BookingDetailUpcomingFragment newInstance(BookingHistoryEntity data) {
        BookingDetailUpcomingFragment fragment = new BookingDetailUpcomingFragment();
        bookingData = data;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinFragmentBookingDetailUpcomingBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        _binding.skinDetailBtnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (bookingData == null) {
            Toast.makeText(requireContext(), "Lỗi dữ liệu", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        populateUI(bookingData);

        _binding.skinDetailBtnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Mã đặt lịch", bookingData.getId());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "Đã sao chép mã đặt lịch", Toast.LENGTH_SHORT).show();
            }
        });

        _binding.skinDetailBtnNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        _binding.skinDetailBtnCancel.setOnClickListener(v -> showCancelDialog(bookingData));
    }

    private void showCancelDialog(BookingHistoryEntity data) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.skin_dialog_cancel_booking, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        kotlin.Pair<String, String> parsedDate = BookingDateParser.parseDateDisplay(data.getDateDisplay(), data.getMonthDisplay(), data.getDayOfWeek());
        String dayNum = parsedDate.getFirst();
        String monthDayStr = parsedDate.getSecond();

        TextView tvDate = dialogView.findViewById(R.id.tv_date);
        if (tvDate != null) tvDate.setText(dayNum);

        TextView tvMonthDay = dialogView.findViewById(R.id.tv_month_day);
        if (tvMonthDay != null) tvMonthDay.setText(monthDayStr);

        TextView tvServiceName = dialogView.findViewById(R.id.tv_service_name);
        if (tvServiceName != null) tvServiceName.setText(data.getServiceName());

        TextView tvTime = dialogView.findViewById(R.id.tv_time);
        if (tvTime != null) tvTime.setText(data.getTime() + " (" + data.getDuration() + ")");

        TextView tvStoreName = dialogView.findViewById(R.id.tv_store_name);
        if (tvStoreName != null) tvStoreName.setText(data.getStoreName());

        TextView tvStoreAddress = dialogView.findViewById(R.id.tv_store_address);
        if (tvStoreAddress != null) tvStoreAddress.setText(data.getStoreAddress());

        Spinner spReason = dialogView.findViewById(R.id.sp_reason);
        List<String> reasons = Arrays.asList("Chọn lý do hủy lịch", "Thay đổi lịch trình", "Tìm được địa điểm khác", "Lý do sức khoẻ", "Đã đặt nhầm dịch vụ", "Lý do khác");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, reasons);
        if (spReason != null) spReason.setAdapter(adapter);

        EditText etOtherReason = dialogView.findViewById(R.id.et_other_reason);

        View btnClose = dialogView.findViewById(R.id.btn_close);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        View btnBack = dialogView.findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());

        View btnConfirmCancel = dialogView.findViewById(R.id.btn_confirm_cancel);
        if (btnConfirmCancel != null) {
            btnConfirmCancel.setOnClickListener(v -> {
                String selectedReason = spReason != null && spReason.getSelectedItem() != null ? spReason.getSelectedItem().toString() : "";
                String otherReason = etOtherReason != null ? etOtherReason.getText().toString().trim() : "";

                String finalReason;
                if ("Lý do khác".equals(selectedReason) && !otherReason.isEmpty()) {
                    finalReason = otherReason;
                } else if ("Lý do khác".equals(selectedReason)) {
                    finalReason = "Lý do khác";
                } else if ("Chọn lý do hủy lịch".equals(selectedReason) && !otherReason.isEmpty()) {
                    finalReason = otherReason;
                } else if ("Chọn lý do hủy lịch".equals(selectedReason)) {
                    finalReason = "Không có lý do cụ thể";
                } else {
                    finalReason = selectedReason;
                }

                final String bookingId = data.getId();
                final android.content.Context appContext = requireContext().getApplicationContext();
                new LocalJsonReader(appContext).updateBookingStatus(bookingId, "Đã huỷ", finalReason);

                data.setStatus("Đã huỷ");
                data.setCancelReason(finalReason);
                bookingData = data;
                populateUI(data);

                executor.execute(() -> new FirestoreService().updateBookingStatus(bookingId, "Đã huỷ", finalReason));

                Toast.makeText(requireContext(), "Hủy lịch thành công", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                getParentFragmentManager().popBackStack();
            });
        }
        dialog.show();
    }

    private void populateUI(BookingHistoryEntity data) {
        _binding.skinDetailServiceName.setText(data.getServiceName());
        _binding.skinDetailOrderId.setText("Mã đặt lịch: " + data.getId());
        _binding.skinDetailDate.setText(data.getDateDisplay() + "\n" + data.getDayOfWeek());
        _binding.skinDetailTime.setText(data.getTime() + " (" + data.getDuration() + ")");

        _binding.skinDetailStatusTag.setText(data.getStatus());
        String status = data.getStatus() != null ? data.getStatus() : "";
        switch (status) {
            case "Sắp diễn ra":
            case "Chờ xác nhận":
            case "pending":
                if ("Chờ xác nhận".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) {
                    _binding.skinDetailStatusTag.setText("Chờ xác nhận");
                    _binding.skinDetailStatusTag.setBackgroundColor(Color.parseColor("#FF9800"));
                } else {
                    _binding.skinDetailStatusTag.setBackgroundResource(R.drawable.skin_bg_btn_book);
                }
                _binding.skinDetailStatusTag.setTextColor(Color.WHITE);
                _binding.skinDetailBottomActions.setVisibility(View.VISIBLE);
                break;
            case "Đã hoàn thành":
                _binding.skinDetailStatusTag.setBackgroundColor(Color.parseColor("#CD853F"));
                _binding.skinDetailStatusTag.setTextColor(Color.WHITE);
                _binding.skinDetailBottomActions.setVisibility(View.GONE);
                break;
            case "Đã huỷ":
                _binding.skinDetailStatusTag.setBackgroundColor(Color.parseColor("#CD5C5C"));
                _binding.skinDetailStatusTag.setTextColor(Color.WHITE);
                _binding.skinDetailBottomActions.setVisibility(View.GONE);
                break;
            default:
                _binding.skinDetailStatusTag.setBackgroundColor(Color.GRAY);
                _binding.skinDetailStatusTag.setTextColor(Color.WHITE);
                _binding.skinDetailBottomActions.setVisibility(View.GONE);
                break;
        }

        if (data.getStoreImage() != null && !data.getStoreImage().isEmpty()) {
            com.bumptech.glide.Glide.with(_binding.skinDetailStoreImage.getContext()).load(data.getStoreImage()).placeholder(R.drawable.imv_logo).error(R.drawable.imv_logo).into(_binding.skinDetailStoreImage);
        } else {
            _binding.skinDetailStoreImage.setImageResource(R.drawable.imv_logo);
        }

        _binding.skinDetailStoreName.setText(data.getStoreName());
        _binding.skinDetailStoreAddress.setText(data.getStoreAddress());
        _binding.skinDetailStorePhone.setText(data.getStorePhone() != null && !data.getStorePhone().isEmpty() ? data.getStorePhone() : "(027) 7100 2020");

        _binding.skinDetailUserName.setText(data.getUserName());
        _binding.skinDetailUserPhone.setText(data.getUserPhone());
        _binding.skinDetailUserEmail.setText(data.getUserEmail());

        if (data.getNote() != null && !data.getNote().isEmpty()) {
            _binding.skinDetailNote.setText(data.getNote());
            _binding.skinDetailNoteContainer.setVisibility(View.VISIBLE);
        } else {
            _binding.skinDetailNoteContainer.setVisibility(View.GONE);
        }

        if (data.getPolicy() != null && !data.getPolicy().isEmpty()) {
            _binding.skinDetailPolicy.setText(data.getPolicy());
            _binding.skinDetailPolicyContainer.setVisibility(View.VISIBLE);
        } else {
            _binding.skinDetailPolicyContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
