package com.veganbeauty.app.features.myskin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.databinding.SkinFragmentBookingHistoryBinding;

import java.util.ArrayList;
import java.util.List;

public class BookingHistoryFragment extends RootieFragment {

    private SkinFragmentBookingHistoryBinding _binding;
    private BookingHistoryAdapter adapter;
    private final List<BookingHistoryEntity> allBookings = new ArrayList<>();
    private final List<BookingHistoryEntity> filteredBookings = new ArrayList<>();
    private String currentStatusFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinFragmentBookingHistoryBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        adapter = new BookingHistoryAdapter(filteredBookings, booking -> {
            if ("Sắp diễn ra".equals(booking.getStatus()) || "Chờ xác nhận".equals(booking.getStatus())) {
                BookingDetailUpcomingFragment fragment = BookingDetailUpcomingFragment.newInstance(booking);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit();
            } else if ("Đã hoàn thành".equals(booking.getStatus())) {
                BookingDetailCompletedFragment fragment = BookingDetailCompletedFragment.newInstance(booking);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                BookingDetailCancelledFragment fragment = BookingDetailCancelledFragment.newInstance(booking);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
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
        syncBookingsFromRemote();
    }

    private void syncBookingsFromRemote() {
        String userEmail = ProfileSession.getEmail(requireContext());
        BookingSyncHelper.syncUserBookings(requireContext(), userEmail, () -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(this::loadBookings);
        });
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
        List<BookingHistoryEntity> list = new LocalJsonReader(requireContext()).getUserBookingHistory(userEmail);
        allBookings.clear();
        if (list != null) {
            java.util.Calendar now = java.util.Calendar.getInstance();
            for (BookingHistoryEntity b : list) {
                if ("Sắp diễn ra".equals(b.getStatus()) || "Chờ xác nhận".equals(b.getStatus())) {
                    java.util.Calendar bookingTime = parseBookingTime(b);
                    if (bookingTime != null && bookingTime.before(now)) {
                        String oldStatus = b.getStatus();
                        b.setStatus("Đã huỷ");
                        if ("Chờ xác nhận".equals(oldStatus)) {
                            b.setCancelReason("Hệ thống tự động huỷ do quá thời gian hẹn nhưng chưa được Admin xác nhận lịch.");
                        } else {
                            b.setCancelReason("Hệ thống tự động huỷ do đã quá thời gian hẹn mà khách không đến Spa.");
                        }
                        
                        final String finalId = b.getId();
                        final String finalReason = b.getCancelReason();
                        final android.content.Context ctx = requireContext().getApplicationContext();
                        new Thread(() -> {
                            new LocalJsonReader(ctx).updateBookingStatus(finalId, "Đã huỷ", finalReason);
                            new com.veganbeauty.app.data.remote.FirestoreService().updateBookingStatus(finalId, "Đã huỷ", finalReason);
                        }).start();
                    }
                }
            }
            allBookings.addAll(list);
        }
        filterBookings();
    }

    private java.util.Calendar parseBookingTime(BookingHistoryEntity b) {
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int currentYear = cal.get(java.util.Calendar.YEAR);
            
            String dateDisplay = b.getDateDisplay();
            String time = b.getTime();
            
            int day = 1;
            int month = cal.get(java.util.Calendar.MONTH);
            int year = currentYear;
            
            if (dateDisplay != null && !dateDisplay.isEmpty()) {
                if (dateDisplay.contains("/")) {
                    String[] parts = dateDisplay.split("/");
                    if (parts.length >= 1) day = Integer.parseInt(parts[0].trim());
                    if (parts.length >= 2) month = Integer.parseInt(parts[1].trim()) - 1;
                    if (parts.length >= 3) year = Integer.parseInt(parts[2].trim());
                } else {
                    day = Integer.parseInt(dateDisplay.trim().split(" ")[0]);
                    String monthDisplay = b.getMonthDisplay();
                    if (monthDisplay != null && monthDisplay.contains("Tháng")) {
                        String firstLine = monthDisplay.split("\n")[0];
                        String mStr = firstLine.replaceAll("[^0-9]", "");
                        if (!mStr.isEmpty()) {
                            month = Integer.parseInt(mStr) - 1;
                        }
                    }
                }
            }
            
            int hour = 0;
            int min = 0;
            if (time != null && !time.isEmpty()) {
                String startTime = time.split("-")[0].trim();
                String[] tParts = startTime.split(":");
                if (tParts.length >= 2) {
                    hour = Integer.parseInt(tParts[0].trim());
                    min = Integer.parseInt(tParts[1].trim());
                }
            }
            
            cal.set(year, month, day, hour, min, 0);
            return cal;
        } catch (Exception e) {
            return null;
        }
    }

    private void filterBookings() {
        filteredBookings.clear();
        if ("ALL".equals(currentStatusFilter)) {
            filteredBookings.addAll(allBookings);
        } else if ("UPCOMING".equals(currentStatusFilter)) {
            for (BookingHistoryEntity b : allBookings) {
                if ("Sắp diễn ra".equals(b.getStatus()) || "Chờ xác nhận".equals(b.getStatus())) {
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
                if ("Đã huỷ".equals(b.getStatus()) || "Đã hủy".equals(b.getStatus())) {
                    filteredBookings.add(b);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }

    static class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.ViewHolder> {
        private final List<BookingHistoryEntity> list;
        private final OnBookingClickListener listener;

        BookingHistoryAdapter(List<BookingHistoryEntity> list, OnBookingClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.skin_item_booking_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookingHistoryEntity booking = list.get(position);

            holder.tvServiceName.setText(booking.getServiceName());
            holder.tvTime.setText(booking.getTime());
            holder.tvStore.setText(booking.getStoreName() + "\n" + booking.getStoreAddress());

            kotlin.Pair<String, String> parsedDate = BookingDateParser.parseDateDisplay(booking.getDateDisplay(), booking.getMonthDisplay(), booking.getDayOfWeek());
            holder.tvDateNum.setText(parsedDate.getFirst());
            holder.tvMonthDay.setText(parsedDate.getSecond());

            holder.tvStatusTag.setText(booking.getStatus());
            
            if ("Sắp diễn ra".equals(booking.getStatus()) || "Chờ xác nhận".equals(booking.getStatus())) {
                holder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_upcoming);
                holder.tvStatusTag.setTextColor(Color.parseColor("#1976D2"));
            } else if ("Đã hoàn thành".equals(booking.getStatus())) {
                holder.tvStatusTag.setBackgroundResource(R.drawable.bg_card_status_green);
                holder.tvStatusTag.setTextColor(Color.parseColor("#388E3C"));
            } else {
                holder.tvStatusTag.setBackgroundResource(R.drawable.bg_card_status_red);
                holder.tvStatusTag.setTextColor(Color.parseColor("#D32F2F"));
            }

            holder.itemView.setOnClickListener(v -> listener.onClick(booking));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvServiceName;
            TextView tvStatusTag;
            TextView tvDateNum;
            TextView tvMonthDay;
            TextView tvTime;
            TextView tvStore;

            ViewHolder(View view) {
                super(view);
                tvServiceName = view.findViewById(R.id.tv_history_service_name);
                tvStatusTag = view.findViewById(R.id.tv_history_status_tag);
                tvDateNum = view.findViewById(R.id.tv_history_date_num);
                tvMonthDay = view.findViewById(R.id.tv_history_month_day);
                tvTime = view.findViewById(R.id.tv_history_time);
                tvStore = view.findViewById(R.id.tv_history_store);
            }
        }
    }

    interface OnBookingClickListener {
        void onClick(BookingHistoryEntity booking);
    }
}
