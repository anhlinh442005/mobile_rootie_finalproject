package com.veganbeauty.app.features.myskin;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.BookingEntity;
import com.veganbeauty.app.databinding.MySkinBookingHistoryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingHistoryFragment extends RootieFragment {

    private MySkinBookingHistoryBinding _binding;
    private BookingHistoryAdapter adapter;
    private final List<BookingEntity> allBookings = new ArrayList<>();
    private final List<BookingEntity> filteredBookings = new ArrayList<>();
    private String currentStatusFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = MySkinBookingHistoryBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        adapter = new BookingHistoryAdapter(filteredBookings, booking -> {
            if ("UPCOMING".equals(booking.getStatus()) || "CONFIRMED".equals(booking.getStatus()) || "PENDING".equals(booking.getStatus())) {
                BookingDetailUpcomingFragment fragment = BookingDetailUpcomingFragment.newInstance(booking);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                BookingDetailCompletedFragment fragment = BookingDetailCompletedFragment.newInstance(booking);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        _binding.rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        _binding.rvBookings.setAdapter(adapter);

        _binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentStatusFilter = "ALL"; break;
                    case 1: currentStatusFilter = "UPCOMING"; break;
                    case 2: currentStatusFilter = "COMPLETED"; break;
                    case 3: currentStatusFilter = "CANCELLED"; break;
                }
                filterBookings();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadBookings();
    }

    private void loadBookings() {
        _binding.progressBar.setVisibility(View.VISIBLE);
        _binding.rvBookings.setVisibility(View.GONE);
        _binding.llEmptyState.setVisibility(View.GONE);

        String userId = ProfileSession.getCurrentUserId(requireContext());
        if (userId == null || userId.equals("guest_user")) {
            _binding.progressBar.setVisibility(View.GONE);
            _binding.llEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .collection("bookings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (_binding == null) return;
                    _binding.progressBar.setVisibility(View.GONE);
                    allBookings.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String serviceName = document.getString("serviceName") != null ? document.getString("serviceName") : "";
                            String status = document.getString("status") != null ? document.getString("status") : "PENDING";
                            String type = document.getString("type") != null ? document.getString("type") : "ONLINE";
                            long timestamp = document.getLong("timestamp") != null ? document.getLong("timestamp") : 0L;
                            String doctorName = document.getString("doctorName") != null ? document.getString("doctorName") : "";
                            String contactPhone = document.getString("contactPhone") != null ? document.getString("contactPhone") : "";
                            String note = document.getString("note") != null ? document.getString("note") : "";
                            long createdAt = document.getLong("createdAt") != null ? document.getLong("createdAt") : 0L;
                            String location = document.getString("location") != null ? document.getString("location") : "";
                            String meetLink = document.getString("meetLink") != null ? document.getString("meetLink") : "";

                            BookingEntity booking = new BookingEntity(
                                    document.getId(), userId, serviceName, status, type, timestamp,
                                    doctorName, contactPhone, note, createdAt, location, meetLink
                            );
                            allBookings.add(booking);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    filterBookings();
                })
                .addOnFailureListener(e -> {
                    if (_binding == null) return;
                    _binding.progressBar.setVisibility(View.GONE);
                    _binding.llEmptyState.setVisibility(View.VISIBLE);
                });
    }

    private void filterBookings() {
        filteredBookings.clear();
        if ("ALL".equals(currentStatusFilter)) {
            filteredBookings.addAll(allBookings);
        } else if ("UPCOMING".equals(currentStatusFilter)) {
            for (BookingEntity b : allBookings) {
                if ("PENDING".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus())) {
                    filteredBookings.add(b);
                }
            }
        } else {
            for (BookingEntity b : allBookings) {
                if (currentStatusFilter.equals(b.getStatus())) {
                    filteredBookings.add(b);
                }
            }
        }

        if (filteredBookings.isEmpty()) {
            _binding.rvBookings.setVisibility(View.GONE);
            _binding.llEmptyState.setVisibility(View.VISIBLE);
        } else {
            _binding.rvBookings.setVisibility(View.VISIBLE);
            _binding.llEmptyState.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        _binding = null;
    }

    class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.ViewHolder> {
        private final List<BookingEntity> list;
        private final OnBookingClickListener listener;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi"));

        BookingHistoryAdapter(List<BookingEntity> list, OnBookingClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.myskin_item_booking_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BookingEntity booking = list.get(position);

            holder.tvServiceName.setText(booking.getServiceName());
            holder.tvDate.setText(dateFormat.format(new Date(booking.getTimestamp())));

            if ("OFFLINE".equals(booking.getType())) {
                holder.tvType.setText("Khám trực tiếp");
                holder.ivTypeIcon.setImageResource(R.drawable.ic_location);
            } else {
                holder.tvType.setText("Khám trực tuyến");
                holder.ivTypeIcon.setImageResource(R.drawable.ic_video_camera);
            }

            Context ctx = holder.itemView.getContext();
            switch (booking.getStatus()) {
                case "PENDING":
                    holder.tvStatus.setText("Chờ xác nhận");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_pending_text));
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    break;
                case "CONFIRMED":
                    holder.tvStatus.setText("Đã xác nhận");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_confirmed_text));
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                    break;
                case "COMPLETED":
                    holder.tvStatus.setText("Đã hoàn thành");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_completed_text));
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
                    break;
                case "CANCELLED":
                    holder.tvStatus.setText("Đã hủy");
                    holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_cancelled_text));
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
                    break;
                default:
                    holder.tvStatus.setText(booking.getStatus());
                    break;
            }

            if (booking.getDoctorName() != null && !booking.getDoctorName().isEmpty()) {
                holder.tvDoctorName.setVisibility(View.VISIBLE);
                holder.tvDoctorName.setText("BS. " + booking.getDoctorName());
            } else {
                holder.tvDoctorName.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> listener.onClick(booking));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvServiceName;
            TextView tvStatus;
            TextView tvDate;
            TextView tvType;
            ImageView ivTypeIcon;
            TextView tvDoctorName;

            ViewHolder(View view) {
                super(view);
                tvServiceName = view.findViewById(R.id.tvServiceName);
                tvStatus = view.findViewById(R.id.tvStatus);
                tvDate = view.findViewById(R.id.tvDate);
                tvType = view.findViewById(R.id.tvType);
                ivTypeIcon = view.findViewById(R.id.ivTypeIcon);
                tvDoctorName = view.findViewById(R.id.tvDoctorName);
            }
        }
    }

    interface OnBookingClickListener {
        void onClick(BookingEntity booking);
    }
}
