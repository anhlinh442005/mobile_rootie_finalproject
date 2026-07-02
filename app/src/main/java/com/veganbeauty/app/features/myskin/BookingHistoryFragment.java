package com.veganbeauty.app.features.myskin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.databinding.SkinFragmentBookingHistoryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingHistoryFragment extends RootieFragment {

    private SkinFragmentBookingHistoryBinding _binding;
    private BookingHistoryAdapter adapter;
    private final List<BookingHistoryEntity> allBookings = new ArrayList<>();
    private final List<BookingHistoryEntity> filteredBookings = new ArrayList<>();
    private final List<Object> displayItems = new ArrayList<>();
    private String currentStatusFilter = "ALL";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinFragmentBookingHistoryBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        adapter = new BookingHistoryAdapter(displayItems, this::openBookingDetail, this::openBookingDetailForCancel);
        _binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        _binding.rvHistory.setAdapter(adapter);

        _binding.filterAll.setOnClickListener(v -> setFilter("ALL"));
        _binding.filterUpcoming.setOnClickListener(v -> setFilter("UPCOMING"));
        _binding.filterCompleted.setOnClickListener(v -> setFilter("COMPLETED"));
        _binding.filterCancelled.setOnClickListener(v -> setFilter("CANCELLED"));

        loadBookings();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (_binding != null) {
            loadBookings();
        }
    }

    private void setFilter(String filter) {
        currentStatusFilter = filter;
        updateFilterUI();
        filterBookings();
    }

    private void updateFilterUI() {
        _binding.filterAll.setBackgroundResource(currentStatusFilter.equals("ALL") ? R.drawable.skin_bg_btn_book : R.drawable.skin_bg_outline);
        _binding.filterAll.setTextColor(currentStatusFilter.equals("ALL") ? Color.WHITE : ContextCompat.getColor(requireContext(), R.color.primary));

        _binding.filterUpcoming.setBackgroundResource(currentStatusFilter.equals("UPCOMING") ? R.drawable.skin_bg_btn_book : R.drawable.skin_bg_outline);
        _binding.filterUpcoming.setTextColor(currentStatusFilter.equals("UPCOMING") ? Color.WHITE : ContextCompat.getColor(requireContext(), R.color.primary));

        _binding.filterCompleted.setBackgroundResource(currentStatusFilter.equals("COMPLETED") ? R.drawable.skin_bg_btn_book : R.drawable.skin_bg_outline);
        _binding.filterCompleted.setTextColor(currentStatusFilter.equals("COMPLETED") ? Color.WHITE : ContextCompat.getColor(requireContext(), R.color.primary));

        _binding.filterCancelled.setBackgroundResource(currentStatusFilter.equals("CANCELLED") ? R.drawable.skin_bg_btn_book : R.drawable.skin_bg_outline);
        _binding.filterCancelled.setTextColor(currentStatusFilter.equals("CANCELLED") ? Color.WHITE : ContextCompat.getColor(requireContext(), R.color.primary));
    }

    private void loadBookings() {
        String userEmail = ProfileSession.getEmail(requireContext());
        if (userEmail == null || userEmail.trim().isEmpty()) {
            allBookings.clear();
            filteredBookings.clear();
            displayItems.clear();
            adapter.updateData(displayItems);
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để xem lịch sử đặt hẹn", Toast.LENGTH_SHORT).show();
            return;
        }

        reloadLocalBookings(userEmail.trim());
        syncBookingsFromFirestore(userEmail.trim());
    }

    private void reloadLocalBookings(String userEmail) {
        List<BookingHistoryEntity> list = new LocalJsonReader(requireContext()).getUserBookingHistory(userEmail);
        allBookings.clear();
        if (list != null) {
            allBookings.addAll(list);
        }
        filterBookings();
    }

    private void syncBookingsFromFirestore(String userEmail) {
        final android.content.Context appContext = requireContext().getApplicationContext();
        executor.execute(() -> {
            List<BookingHistoryEntity> remote = new FirestoreService().fetchBookingsForUser(userEmail);
            if (remote != null && !remote.isEmpty()) {
                new LocalJsonReader(appContext).mergeBookingsFromRemote(remote);
            }
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || _binding == null) {
                    return;
                }
                reloadLocalBookings(userEmail);
            });
        });
    }

    private void filterBookings() {
        filteredBookings.clear();
        if ("ALL".equals(currentStatusFilter)) {
            filteredBookings.addAll(allBookings);
        } else if ("UPCOMING".equals(currentStatusFilter)) {
            for (BookingHistoryEntity b : allBookings) {
                if (isUpcomingStatus(b.getStatus())) {
                    filteredBookings.add(b);
                }
            }
        } else if ("COMPLETED".equals(currentStatusFilter)) {
            for (BookingHistoryEntity b : allBookings) {
                if ("Đã hoàn thành".equals(b.getStatus())) {
                    filteredBookings.add(b);
                }
            }
        } else if ("CANCELLED".equals(currentStatusFilter)) {
            for (BookingHistoryEntity b : allBookings) {
                if (isCancelledStatus(b.getStatus())) {
                    filteredBookings.add(b);
                }
            }
        }
        rebuildDisplayItems();
    }

    private void rebuildDisplayItems() {
        displayItems.clear();
        if ("ALL".equals(currentStatusFilter)) {
            appendGroup("Sắp diễn ra", booking -> isUpcomingStatus(booking.getStatus()));
            appendGroup("Đã hoàn thành", booking -> "Đã hoàn thành".equals(booking.getStatus()));
            appendGroup("Đã huỷ", booking -> isCancelledStatus(booking.getStatus()));
        } else {
            for (BookingHistoryEntity booking : filteredBookings) {
                displayItems.add(booking);
            }
        }
        adapter.updateData(displayItems);
    }

    private void appendGroup(String title, BookingFilter filter) {
        List<BookingHistoryEntity> group = new ArrayList<>();
        for (BookingHistoryEntity booking : filteredBookings) {
            if (filter.matches(booking)) {
                group.add(booking);
            }
        }
        if (!group.isEmpty()) {
            displayItems.add(title);
            displayItems.addAll(group);
        }
    }

    private void openBookingDetail(BookingHistoryEntity booking) {
        if (isUpcomingStatus(booking.getStatus())) {
            navigateTo(BookingDetailUpcomingFragment.newInstance(booking));
        } else if ("Đã hoàn thành".equals(booking.getStatus())) {
            navigateTo(BookingDetailCompletedFragment.newInstance(booking));
        } else {
            navigateTo(BookingDetailCancelledFragment.newInstance(booking));
        }
    }

    private void openBookingDetailForCancel(BookingHistoryEntity booking) {
        if (isUpcomingStatus(booking.getStatus())) {
            navigateTo(BookingDetailUpcomingFragment.newInstance(booking));
        } else {
            openBookingDetail(booking);
        }
    }

    private void navigateTo(RootieFragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private static boolean isUpcomingStatus(String status) {
        if (status == null) return false;
        return "Sắp diễn ra".equals(status)
                || "Chờ xác nhận".equals(status)
                || "pending".equalsIgnoreCase(status);
    }

    private static boolean isCancelledStatus(String status) {
        if (status == null) return false;
        return "Đã huỷ".equals(status) || "Đã hủy".equals(status);
    }

    private interface BookingFilter {
        boolean matches(BookingHistoryEntity booking);
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
