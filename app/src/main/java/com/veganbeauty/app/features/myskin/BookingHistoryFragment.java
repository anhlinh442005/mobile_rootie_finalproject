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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.databinding.SkinFragmentBookingHistoryBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookingHistoryFragment extends RootieFragment {

    private SkinFragmentBookingHistoryBinding _binding;
    private BookingHistoryAdapter adapter;
    private final List<BookingHistoryEntity> allBookings = new ArrayList<>();
    private final List<BookingHistoryEntity> filteredBookings = new ArrayList<>();
    private String currentStatusFilter = "ALL";

    private String filterServiceName = "";
    private String filterStoreName = "";
    private String filterDate = "";
    private String filterTime = "";
    private String filterMonth = "";

    private ListenerRegistration bookingsListener;
    private ListenerRegistration bookingsListenerByEmail;

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
        _binding.filterPending.setOnClickListener(v -> setFilter("PENDING"));
        _binding.filterUpcoming.setOnClickListener(v -> setFilter("UPCOMING"));
        _binding.filterCompleted.setOnClickListener(v -> setFilter("COMPLETED"));
        _binding.filterCancelled.setOnClickListener(v -> setFilter("CANCELLED"));

        _binding.btnAdvancedFilter.setOnClickListener(v -> showAdvancedFilter());

        loadBookings();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncBookingsFromRemote();
        startRealtimeListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRealtimeListener();
    }

    private void startRealtimeListener() {
        stopRealtimeListener();
        String userEmail = ProfileSession.getEmail(requireContext());
        if (userEmail == null || userEmail.trim().isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String normalizedEmail = userEmail.trim();

        bookingsListener = db.collection("bookings")
                .whereEqualTo("userEmail", normalizedEmail)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || !isAdded()) return;
                    onBookingsSnapshotChanged(snapshots.getDocuments());
                });

        bookingsListenerByEmail = db.collection("bookings")
                .whereEqualTo("email", normalizedEmail)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || !isAdded()) return;
                    onBookingsSnapshotChanged(snapshots.getDocuments());
                });
    }

    private void onBookingsSnapshotChanged(List<DocumentSnapshot> docs) {
        List<BookingHistoryEntity> remoteBookings = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            BookingHistoryEntity entity = mapBookingDoc(doc);
            if (entity != null) remoteBookings.add(entity);
        }
        if (remoteBookings.isEmpty()) return;

        new LocalJsonReader(requireContext()).mergeBookingsFromRemote(remoteBookings);
        requireActivity().runOnUiThread(this::loadBookings);
    }

    private BookingHistoryEntity mapBookingDoc(DocumentSnapshot doc) {
        try {
            String userEmail = doc.getString("userEmail");
            if (userEmail == null || userEmail.isEmpty()) {
                userEmail = doc.getString("email") != null ? doc.getString("email") : "";
            }
            List<String> skinResults = new ArrayList<>();
            List<?> rawSkinResults = (List<?>) doc.get("skinResults");
            if (rawSkinResults != null) {
                for (Object item : rawSkinResults) {
                    if (item != null) skinResults.add(item.toString());
                }
            }
            String docId = doc.getString("id");
            if (docId == null || docId.isEmpty()) docId = doc.getId();

            return new BookingHistoryEntity(
                    docId,
                    doc.getString("userId") != null ? doc.getString("userId") : "",
                    doc.getString("userName") != null ? doc.getString("userName") : "",
                    doc.getString("userPhone") != null ? doc.getString("userPhone") : "",
                    userEmail,
                    doc.getString("serviceName") != null ? doc.getString("serviceName") : "",
                    doc.getString("dateDisplay") != null ? doc.getString("dateDisplay") : "",
                    doc.getString("monthDisplay") != null ? doc.getString("monthDisplay") : "",
                    doc.getString("dayOfWeek") != null ? doc.getString("dayOfWeek") : "",
                    doc.getString("time") != null ? doc.getString("time") : "",
                    doc.getString("duration") != null ? doc.getString("duration") : "",
                    doc.getString("storeName") != null ? doc.getString("storeName") : "",
                    doc.getString("storeAddress") != null ? doc.getString("storeAddress") : "",
                    doc.getString("storePhone") != null ? doc.getString("storePhone") : "",
                    doc.getString("storeImage") != null ? doc.getString("storeImage") : "",
                    doc.getString("note") != null ? doc.getString("note") : "",
                    doc.getString("status") != null ? doc.getString("status") : "",
                    doc.getString("policy") != null ? doc.getString("policy") : "",
                    doc.getString("createdAt") != null ? doc.getString("createdAt") : "",
                    doc.getString("completedAt") != null ? doc.getString("completedAt") : "",
                    skinResults,
                    doc.getString("consultantName") != null ? doc.getString("consultantName") : "",
                    doc.getString("consultantAvatar") != null ? doc.getString("consultantAvatar") : "",
                    doc.getDouble("consultantRating") != null ? doc.getDouble("consultantRating").floatValue() : 0f,
                    doc.getDouble("userRating") != null ? doc.getDouble("userRating").floatValue() : 0f,
                    doc.getString("userReview") != null ? doc.getString("userReview") : "",
                    doc.getString("reviewDate") != null ? doc.getString("reviewDate") : "",
                    doc.getString("beforeImage") != null ? doc.getString("beforeImage") : "",
                    doc.getString("afterImage") != null ? doc.getString("afterImage") : "",
                    doc.getLong("earnedPoints") != null ? doc.getLong("earnedPoints").intValue() : 0,
                    doc.getLong("totalPoints") != null ? doc.getLong("totalPoints").intValue() : 0,
                    doc.getString("nextAppointmentDate") != null ? doc.getString("nextAppointmentDate") : "",
                    doc.getString("nextAppointmentText") != null ? doc.getString("nextAppointmentText") : "",
                    doc.getString("cancelledAt") != null ? doc.getString("cancelledAt") : "",
                    doc.getString("cancelReason") != null ? doc.getString("cancelReason") : ""
            );
        } catch (Exception e) {
            return null;
        }
    }

    private void stopRealtimeListener() {
        if (bookingsListener != null) {
            bookingsListener.remove();
            bookingsListener = null;
        }
        if (bookingsListenerByEmail != null) {
            bookingsListenerByEmail.remove();
            bookingsListenerByEmail = null;
        }
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

        _binding.filterPending.setBackgroundResource(currentStatusFilter.equals("PENDING") ? R.drawable.skin_bg_btn_book : R.drawable.skin_bg_outline);
        _binding.filterPending.setTextColor(currentStatusFilter.equals("PENDING") ? Color.WHITE : ContextCompat.getColor(requireContext(), R.color.primary));

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
                if ("Sắp diễn ra".equals(b.getStatus()) || "Chờ xác nhận".equals(b.getStatus()) || "pending".equalsIgnoreCase(b.getStatus())) {
                    java.util.Calendar bookingTime = parseBookingTime(b);
                    if (bookingTime != null && bookingTime.before(now)) {
                        String oldStatus = b.getStatus();
                        b.setStatus("Đã huỷ");
                        if ("Chờ xác nhận".equals(oldStatus) || "pending".equalsIgnoreCase(oldStatus)) {
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

    private void showAdvancedFilter() {
        BookingFilterBottomSheet sheet = new BookingFilterBottomSheet();
        sheet.setInitialValues(filterServiceName, filterStoreName,
                filterDate, filterTime, filterMonth);

        sheet.setOnFilterAppliedListener((serviceName, storeName, date, time, month) -> {
            filterServiceName = serviceName;
            filterStoreName = storeName;
            filterDate = date;
            filterTime = time;
            filterMonth = month;
            filterBookings();
        });

        sheet.show(getChildFragmentManager(), BookingFilterBottomSheet.TAG);
    }

    private void filterBookings() {
        filteredBookings.clear();

        for (BookingHistoryEntity b : allBookings) {
            if (!matchesStatusFilter(b)) continue;
            if (!matchesAdvancedFilter(b)) continue;
            filteredBookings.add(b);
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private boolean matchesStatusFilter(BookingHistoryEntity b) {
        if ("ALL".equals(currentStatusFilter)) return true;
        String status = b.getStatus();
        switch (currentStatusFilter) {
            case "PENDING":
                return "Chờ xác nhận".equals(status) || "pending".equalsIgnoreCase(status);
            case "UPCOMING":
                return "Sắp diễn ra".equals(status);
            case "COMPLETED":
                return "Đã hoàn thành".equals(status);
            case "CANCELLED":
                return "Đã huỷ".equals(status) || "Đã hủy".equals(status);
            default:
                return true;
        }
    }

    private boolean matchesAdvancedFilter(BookingHistoryEntity b) {
        if (!filterServiceName.isEmpty()) {
            String sn = b.getServiceName() != null ? b.getServiceName().toLowerCase(Locale.getDefault()) : "";
            if (!sn.contains(filterServiceName.toLowerCase(Locale.getDefault()))) return false;
        }
        if (!filterStoreName.isEmpty()) {
            String st = b.getStoreName() != null ? b.getStoreName().toLowerCase(Locale.getDefault()) : "";
            if (!st.contains(filterStoreName.toLowerCase(Locale.getDefault()))) return false;
        }
        if (!filterDate.isEmpty()) {
            String dd = b.getDateDisplay() != null ? b.getDateDisplay() : "";
            String created = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            if (!dd.contains(filterDate) && !created.contains(filterDate)) return false;
        }
        if (!filterTime.isEmpty()) {
            String time = b.getTime() != null ? b.getTime() : "";
            if (!time.contains(filterTime)) return false;
        }
        if (!filterMonth.isEmpty()) {
            String month = b.getMonthDisplay() != null ? b.getMonthDisplay().toLowerCase(Locale.getDefault()) : "";
            String created = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            String filterMonthLower = filterMonth.toLowerCase(Locale.getDefault());
            if (!month.contains(filterMonthLower) && !created.contains(filterMonthLower)) return false;
        }
        return true;
    }

    private boolean hasActiveAdvancedFilter() {
        return !filterServiceName.isEmpty() || !filterStoreName.isEmpty()
                || !filterDate.isEmpty()
                || !filterTime.isEmpty() || !filterMonth.isEmpty();
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredBookings.isEmpty();
        boolean hasActiveFilter = hasActiveAdvancedFilter() || !"ALL".equals(currentStatusFilter);

        _binding.rvHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        _binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (hasActiveFilter && !isEmpty) {
            _binding.tvResultCount.setVisibility(View.VISIBLE);
            _binding.tvResultCount.setText("Tìm thấy " + filteredBookings.size() + " lịch hẹn");
        } else {
            _binding.tvResultCount.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopRealtimeListener();
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

            String[] parsedDate = com.veganbeauty.app.features.myskin.BookingHistoryAdapter.BookingDateParser.parseDateDisplay(
                    booking.getDateDisplay(), booking.getMonthDisplay(), booking.getDayOfWeek()
            );
            holder.tvDateNum.setText(parsedDate[0]);
            holder.tvMonthDay.setText(parsedDate[1]);

            holder.tvStatusTag.setText(booking.getStatus());
            
            if ("Chờ xác nhận".equals(booking.getStatus()) || "pending".equalsIgnoreCase(booking.getStatus())) {
                holder.tvStatusTag.setText("Chờ xác nhận");
                holder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_pending);
                holder.tvStatusTag.setTextColor(Color.parseColor("#E65100"));
            } else if ("Sắp diễn ra".equals(booking.getStatus())) {
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
