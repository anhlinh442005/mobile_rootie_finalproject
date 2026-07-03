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
        if (list != null) allBookings.addAll(list);
        filterBookings();
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

            String dayNum = "01";
            if (booking.getDateDisplay() != null && booking.getDateDisplay().contains(" ")) {
                dayNum = booking.getDateDisplay().split(" ")[0];
            }
            holder.tvDateNum.setText(dayNum);
            holder.tvMonthDay.setText(booking.getMonthDisplay() + "\n" + booking.getDayOfWeek());

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
