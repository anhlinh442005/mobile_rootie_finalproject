package com.veganbeauty.app.features.myskin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.databinding.SkinFragmentBookingHistoryBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public class BookingHistoryFragment extends RootieFragment {

    private SkinFragmentBookingHistoryBinding _binding;
    private BookingHistoryAdapter adapter;
    private final List<BookingHistoryEntity> allBookings = new ArrayList<>();
    private String currentStatusFilter = "Tất Cả";
    private LocalJsonReader jsonReader;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinFragmentBookingHistoryBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        if (!ProfileSession.isLoggedIn(requireContext())) {
            BottomNavHelper.showLoginRequiredDialog(requireContext());
            getParentFragmentManager().popBackStack();
            return;
        }

        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        _binding.btnNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        adapter = new BookingHistoryAdapter(new ArrayList<>(), booking -> {
            BookingHistoryEntity selectedBooking = booking;
            String status = selectedBooking.getStatus();
            RootieFragment detailFragment;
            if ("Đã hoàn thành".equalsIgnoreCase(status)) {
                detailFragment = BookingDetailCompletedFragment.newInstance(selectedBooking);
            } else if ("Đã huỷ".equalsIgnoreCase(status) || "Đã hủy".equalsIgnoreCase(status)) {
                detailFragment = BookingDetailCancelledFragment.newInstance(selectedBooking);
            } else {
                detailFragment = BookingDetailUpcomingFragment.newInstance(selectedBooking);
            }
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        }, this::showCancelDialog);
        _binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        _binding.rvHistory.setAdapter(adapter);

        _binding.filterAll.setOnClickListener(v -> setFilter("Tất Cả", _binding.filterAll));
        _binding.filterUpcoming.setOnClickListener(v -> setFilter("Sắp diễn ra", _binding.filterUpcoming));
        _binding.filterCompleted.setOnClickListener(v -> setFilter("Đã hoàn thành", _binding.filterCompleted));
        _binding.filterCancelled.setOnClickListener(v -> setFilter("Đã huỷ", _binding.filterCancelled));

        jsonReader = new LocalJsonReader(requireContext());
        String userId = ProfileSession.getUserId(requireContext());
        List<BookingHistoryEntity> local = jsonReader.getUserBookingHistory(userId);
        allBookings.clear();
        allBookings.addAll(local);
        applyFilter();
        loadDataFromFirestore();
    }

    private void setFilter(String filter, TextView selectedTextView) {
        currentStatusFilter = filter;
        int unselectedBg = R.drawable.skin_bg_outline;
        int unselectedColor = ContextCompat.getColor(requireContext(), R.color.primary);

        _binding.filterAll.setBackgroundResource(unselectedBg);
        _binding.filterUpcoming.setBackgroundResource(unselectedBg);
        _binding.filterCompleted.setBackgroundResource(unselectedBg);
        _binding.filterCancelled.setBackgroundResource(unselectedBg);

        _binding.filterAll.setTextColor(unselectedColor);
        _binding.filterUpcoming.setTextColor(unselectedColor);
        _binding.filterCompleted.setTextColor(unselectedColor);
        _binding.filterCancelled.setTextColor(unselectedColor);

        selectedTextView.setBackgroundResource(R.drawable.skin_bg_btn_book);
        selectedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral));

        applyFilter();
    }

    private void applyFilter() {
        List<BookingHistoryEntity> filteredList = new ArrayList<>();
        for (BookingHistoryEntity item : allBookings) {
            String status = normalizeStatus(item.getStatus());
            if ("Tất Cả".equals(currentStatusFilter)) {
                filteredList.add(item);
            } else if ("Sắp diễn ra".equals(currentStatusFilter)) {
                if ("Sắp diễn ra".equals(status)) filteredList.add(item);
            } else if ("Đã hoàn thành".equals(currentStatusFilter)) {
                if ("Đã hoàn thành".equals(status)) filteredList.add(item);
            } else if ("Đã huỷ".equals(currentStatusFilter)) {
                if ("Đã huỷ".equals(status)) filteredList.add(item);
            }
        }

        Map<String, List<BookingHistoryEntity>> grouped = new LinkedHashMap<>();
        grouped.put("Sắp diễn ra", new ArrayList<>());
        grouped.put("Đã hoàn thành", new ArrayList<>());
        grouped.put("Đã huỷ", new ArrayList<>());

        for (BookingHistoryEntity item : filteredList) {
            String status = normalizeStatus(item.getStatus());
            if (grouped.containsKey(status)) {
                grouped.get(status).add(item);
            }
        }

        List<Object> displayItems = new ArrayList<>();
        for (String status : grouped.keySet()) {
            List<BookingHistoryEntity> group = grouped.get(status);
            if (group != null && !group.isEmpty()) {
                displayItems.add(status);
                displayItems.addAll(group);
            }
        }
        adapter.updateData(displayItems);
    }

    private String normalizeStatus(String status) {
        if (status == null) return "";
        String s = status.trim().toLowerCase(Locale.ROOT);
        if ("chờ xác nhận".equals(s) || "pending".equals(s) || "sắp diễn ra".equals(s)) {
            return "Sắp diễn ra";
        }
        if ("đã hoàn thành".equals(s)) return "Đã hoàn thành";
        if ("đã huỷ".equals(s) || "đã hủy".equals(s)) return "Đã huỷ";
        return status;
    }

    private void loadDataFromFirestore() {
        BuildersKt.launch(
                LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()),
                Dispatchers.getIO(),
                kotlinx.coroutines.CoroutineStart.DEFAULT,
                (scope, continuation) -> {
                    String userId = ProfileSession.getUserId(requireContext());
                    List<BookingHistoryEntity> remote = new FirestoreService().getUserBookingHistory(userId);
                    if (remote != null && !remote.isEmpty()) {
                        for (BookingHistoryEntity remoteItem : remote) {
                            jsonReader.addBooking(remoteItem);
                        }
                        List<BookingHistoryEntity> merged = jsonReader.getUserBookingHistory(userId);
                        requireActivity().runOnUiThread(() -> {
                            allBookings.clear();
                            allBookings.addAll(merged);
                            applyFilter();
                        });
                    }
                    return kotlin.Unit.INSTANCE;
                }
        );
    }

    private void showCancelDialog(BookingHistoryEntity data) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.skin_dialog_cancel_booking, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        kotlin.Pair<String, String> parsed = BookingDateParser.parseDateDisplay(data.getDateDisplay(), data.getMonthDisplay(), data.getDayOfWeek());
        ((TextView) dialogView.findViewById(R.id.tv_date)).setText(parsed.getFirst());
        ((TextView) dialogView.findViewById(R.id.tv_month_day)).setText(parsed.getSecond());
        ((TextView) dialogView.findViewById(R.id.tv_service_name)).setText(data.getServiceName());
        ((TextView) dialogView.findViewById(R.id.tv_time)).setText(data.getTime() + " (" + data.getDuration() + ")");
        ((TextView) dialogView.findViewById(R.id.tv_store_name)).setText(data.getStoreName());
        ((TextView) dialogView.findViewById(R.id.tv_store_address)).setText(data.getStoreAddress());

        android.widget.Spinner spReason = dialogView.findViewById(R.id.sp_reason);
        List<String> reasons = java.util.Arrays.asList(
                "Chọn lý do hủy lịch",
                "Thay đổi lịch trình",
                "Tìm được địa điểm khác",
                "Lý do sức khoẻ",
                "Đã đặt nhầm dịch vụ",
                "Lý do khác"
        );
        spReason.setAdapter(new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, reasons));
        android.widget.EditText etOtherReason = dialogView.findViewById(R.id.et_other_reason);

        dialogView.findViewById(R.id.btn_close).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_back).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btn_confirm_cancel).setOnClickListener(v -> {
            String selectedReason = spReason.getSelectedItem() != null ? spReason.getSelectedItem().toString() : "";
            String otherReason = etOtherReason.getText().toString().trim();
            String finalReason;
            if ("Lý do khác".equals(selectedReason) && !otherReason.isEmpty()) finalReason = otherReason;
            else if ("Lý do khác".equals(selectedReason)) finalReason = "Lý do khác";
            else if ("Chọn lý do hủy lịch".equals(selectedReason) && !otherReason.isEmpty()) finalReason = otherReason;
            else if ("Chọn lý do hủy lịch".equals(selectedReason)) finalReason = "Không có lý do cụ thể";
            else finalReason = selectedReason;

            jsonReader.updateBookingStatus(data.getId(), "Đã huỷ", finalReason);
            BuildersKt.launch(
                    LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()),
                    Dispatchers.getIO(),
                    kotlinx.coroutines.CoroutineStart.DEFAULT,
                    (scope, continuation) -> {
                        new FirestoreService().updateBookingStatus(data.getId(), "Đã huỷ", finalReason);
                        return kotlin.Unit.INSTANCE;
                    }
            );

            List<BookingHistoryEntity> updated = jsonReader.getUserBookingHistory(ProfileSession.getUserId(requireContext()));
            allBookings.clear();
            allBookings.addAll(updated);
            applyFilter();
            Toast.makeText(requireContext(), "Hủy lịch thành công", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }
}
