package com.veganbeauty.app.features.myskin;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
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
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.databinding.SkinFragmentBookingHistoryBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingHistoryFragment extends RootieFragment {

    private SkinFragmentBookingHistoryBinding _binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;
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
    private ListenerRegistration bookingsListenerByUserId;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        _binding = SkinFragmentBookingHistoryBinding.inflate(inflater, container, false);
        return _binding.getRoot();
    }

    @Override
    protected void setupUI(@NonNull View view) {
        _binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        adapter = new BookingHistoryAdapter(filteredBookings, this::openBookingDetail, this::showCancelDialog);
        _binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        _binding.rvHistory.setAdapter(adapter);

        _binding.filterAll.setOnClickListener(v -> setFilter("ALL"));
        _binding.filterPending.setOnClickListener(v -> setFilter("PENDING"));
        _binding.filterUpcoming.setOnClickListener(v -> setFilter("UPCOMING"));
        _binding.filterCompleted.setOnClickListener(v -> setFilter("COMPLETED"));
        _binding.filterCancelled.setOnClickListener(v -> setFilter("CANCELLED"));

        _binding.btnAdvancedFilter.setOnClickListener(v -> showAdvancedFilter());

        setupScrollHideHeader();
        loadBookings();
    }

    private void setupScrollHideHeader() {
        float density = getResources().getDisplayMetrics().density;
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                _binding.header,
                _binding.rvHistory,
                (int) (12 * density),
                (int) (40 * density),
                _binding.filterRow
        );
        headerScrollHelper.attachToRecyclerView(_binding.rvHistory);
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
        String userId = ProfileSession.getUserId(requireContext());
        if ((userEmail == null || userEmail.trim().isEmpty())
                && (userId == null || userId.trim().isEmpty())) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (userEmail != null && !userEmail.trim().isEmpty()) {
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

        if (userId != null && !userId.trim().isEmpty()) {
            bookingsListenerByUserId = db.collection("bookings")
                    .whereEqualTo("userId", userId.trim())
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null || snapshots == null || !isAdded()) return;
                        onBookingsSnapshotChanged(snapshots.getDocuments());
                    });
        }
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

            List<String> feedbackImages = new ArrayList<>();
            List<?> rawFeedbackImages = (List<?>) doc.get("feedbackImageUrls");
            if (rawFeedbackImages != null) {
                for (Object item : rawFeedbackImages) {
                    if (item != null) {
                        String url = item.toString().trim();
                        if (!url.isEmpty()) {
                            feedbackImages.add(url);
                        }
                    }
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
                    doc.getString("cancelReason") != null ? doc.getString("cancelReason") : "",
                    feedbackImages
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
        if (bookingsListenerByUserId != null) {
            bookingsListenerByUserId.remove();
            bookingsListenerByUserId = null;
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
        styleFilterChip(_binding.filterAll, currentStatusFilter.equals("ALL"));
        styleFilterChip(_binding.filterPending, currentStatusFilter.equals("PENDING"));
        styleFilterChip(_binding.filterUpcoming, currentStatusFilter.equals("UPCOMING"));
        styleFilterChip(_binding.filterCompleted, currentStatusFilter.equals("COMPLETED"));
        styleFilterChip(_binding.filterCancelled, currentStatusFilter.equals("CANCELLED"));
    }

    private void styleFilterChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.skin_bg_filter_chip_active : R.drawable.skin_bg_outline);
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        chip.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void loadBookings() {
        String userEmail = ProfileSession.getEmail(requireContext());
        List<BookingHistoryEntity> list = new LocalJsonReader(requireContext()).getUserBookingHistory(userEmail);
        allBookings.clear();
        if (list != null) {
            allBookings.addAll(list);
        }
        filterBookings();
    }

    private void openBookingDetail(BookingHistoryEntity booking) {
        String status = booking.getStatus() != null ? booking.getStatus().trim() : "";
        if (isCancellableStatus(status)) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, BookingDetailUpcomingFragment.newInstance(booking))
                    .addToBackStack(null)
                    .commit();
        } else if ("Đã hoàn thành".equalsIgnoreCase(status)) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, BookingDetailCompletedFragment.newInstance(booking))
                    .addToBackStack(null)
                    .commit();
        } else {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, BookingDetailCancelledFragment.newInstance(booking))
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showCancelDialog(BookingHistoryEntity data) {
        if (!isCancellableStatus(data.getStatus())) {
            Toast.makeText(requireContext(), "Lịch hẹn này không thể huỷ", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.skin_dialog_cancel_booking, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        String[] parsedDate = com.veganbeauty.app.features.myskin.BookingHistoryAdapter.BookingDateParser.parseDateDisplay(
                data.getDateDisplay(), data.getMonthDisplay(), data.getDayOfWeek());

        TextView tvDate = dialogView.findViewById(R.id.tv_date);
        if (tvDate != null) tvDate.setText(parsedDate[0]);

        TextView tvMonthDay = dialogView.findViewById(R.id.tv_month_day);
        if (tvMonthDay != null) tvMonthDay.setText(parsedDate[1]);

        TextView tvServiceName = dialogView.findViewById(R.id.tv_service_name);
        if (tvServiceName != null) tvServiceName.setText(data.getServiceName());

        TextView tvTime = dialogView.findViewById(R.id.tv_time);
        if (tvTime != null) tvTime.setText(data.getTime() + " (" + data.getDuration() + ")");

        TextView tvStoreName = dialogView.findViewById(R.id.tv_store_name);
        if (tvStoreName != null) tvStoreName.setText(data.getStoreName());

        TextView tvStoreAddress = dialogView.findViewById(R.id.tv_store_address);
        if (tvStoreAddress != null) tvStoreAddress.setText(data.getStoreAddress());

        Spinner spReason = dialogView.findViewById(R.id.sp_reason);
        List<String> reasons = Arrays.asList(
                "Chọn lý do hủy lịch", "Thay đổi lịch trình", "Tìm được địa điểm khác",
                "Lý do sức khoẻ", "Đã đặt nhầm dịch vụ", "Lý do khác");
        if (spReason != null) {
            spReason.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, reasons));
        }

        EditText etOtherReason = dialogView.findViewById(R.id.et_other_reason);

        View btnClose = dialogView.findViewById(R.id.btn_close);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        View btnBack = dialogView.findViewById(R.id.btn_back);
        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());

        View btnConfirmCancel = dialogView.findViewById(R.id.btn_confirm_cancel);
        if (btnConfirmCancel != null) {
            btnConfirmCancel.setOnClickListener(v -> {
                String selectedReason = spReason != null && spReason.getSelectedItem() != null
                        ? spReason.getSelectedItem().toString() : "";
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
                android.content.Context appContext = requireContext().getApplicationContext();
                ioExecutor.execute(() -> {
                    FirestoreService firestoreService = new FirestoreService();
                    if (firestoreService.updateBookingStatus(bookingId, "Đã huỷ", finalReason)) {
                        new LocalJsonReader(appContext).updateBookingStatus(bookingId, "Đã huỷ", finalReason);
                    }
                });

                Toast.makeText(requireContext(), "Huỷ lịch thành công", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadBookings();
            });
        }
        dialog.show();
    }

    static boolean isCancellableStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return false;
        }
        String normalized = status.trim();
        return "Chờ xác nhận".equalsIgnoreCase(normalized)
                || "pending".equalsIgnoreCase(normalized)
                || "Sắp diễn ra".equalsIgnoreCase(normalized)
                || "Đã duyệt".equalsIgnoreCase(normalized);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }

    static class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.ViewHolder> {
        private final List<BookingHistoryEntity> list;
        private final OnBookingClickListener viewDetailListener;
        private final OnBookingClickListener cancelListener;

        BookingHistoryAdapter(List<BookingHistoryEntity> list,
                              OnBookingClickListener viewDetailListener,
                              OnBookingClickListener cancelListener) {
            this.list = list;
            this.viewDetailListener = viewDetailListener;
            this.cancelListener = cancelListener;
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

            boolean cancellable = isCancellableStatus(booking.getStatus());
            if (cancellable) {
                holder.llActions.setVisibility(View.VISIBLE);
                holder.btnCancel.setVisibility(View.VISIBLE);
                holder.btnViewDetail.setVisibility(View.VISIBLE);
                holder.btnCancel.setOnClickListener(v -> {
                    if (cancelListener != null) cancelListener.onClick(booking);
                });
                holder.btnViewDetail.setOnClickListener(v -> {
                    if (viewDetailListener != null) viewDetailListener.onClick(booking);
                });
            } else {
                holder.llActions.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (viewDetailListener != null) viewDetailListener.onClick(booking);
            });
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
            View llActions;
            TextView btnCancel;
            TextView btnViewDetail;

            ViewHolder(View view) {
                super(view);
                tvServiceName = view.findViewById(R.id.tv_history_service_name);
                tvStatusTag = view.findViewById(R.id.tv_history_status_tag);
                tvDateNum = view.findViewById(R.id.tv_history_date_num);
                tvMonthDay = view.findViewById(R.id.tv_history_month_day);
                tvTime = view.findViewById(R.id.tv_history_time);
                tvStore = view.findViewById(R.id.tv_history_store);
                llActions = view.findViewById(R.id.ll_history_actions);
                btnCancel = view.findViewById(R.id.btn_cancel_booking);
                btnViewDetail = view.findViewById(R.id.btn_view_detail);
            }
        }
    }

    interface OnBookingClickListener {
        void onClick(BookingHistoryEntity booking);
    }
}
