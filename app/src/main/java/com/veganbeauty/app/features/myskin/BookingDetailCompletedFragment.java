package com.veganbeauty.app.features.myskin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;


import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.databinding.SkinFragmentBookingDetailCompletedBinding;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingDetailCompletedFragment extends RootieFragment {

    private SkinFragmentBookingDetailCompletedBinding binding;
    private static BookingHistoryEntity bookingData = null;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public static BookingDetailCompletedFragment newInstance(BookingHistoryEntity data) {
        BookingDetailCompletedFragment fragment = new BookingDetailCompletedFragment();
        bookingData = data;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinFragmentBookingDetailCompletedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.skinDetailBtnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.skinDetailBtnNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        if (bookingData != null) {
            populateUI(bookingData);
        } else {
            Toast.makeText(getContext(), "Lỗi dữ liệu", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        }

        binding.skinDetailBtnCopy.setOnClickListener(v -> {
            if (getContext() != null) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Mã đặt lịch", bookingData != null ? bookingData.getId() : "");
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "Đã sao chép mã đặt lịch", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.skinDetailBtnRebook.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new BookingFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.skinDetailBtnBookOther.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new BookingFragment())
                    .addToBackStack(null)
                    .commit();
        });

        binding.skinDetailBtnEditReview.setOnClickListener(v -> showEditReviewDialog());
        binding.skinDetailBtnViewImages.setOnClickListener(v -> openBeforeAfterGallery());
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    private void populateUI(BookingHistoryEntity data) {
        binding.skinDetailServiceName.setText(data.getServiceName());
        binding.skinDetailOrderId.setText("Mã đặt lịch: " + data.getId());
        binding.skinDetailDate.setText(data.getDateDisplay() + " • " + data.getDayOfWeek());
        binding.skinDetailTime.setText(data.getTime() + " (" + data.getDuration() + ")");

        if (data.getCompletedAt() != null && !data.getCompletedAt().isEmpty()) {
            binding.skinDetailCompletedAt.setText("Hoàn thành lúc: " + data.getCompletedAt());
        }

        if (data.getStoreImage() != null && !data.getStoreImage().isEmpty()) {
            com.bumptech.glide.Glide.with(binding.skinDetailStoreImage.getContext()).load(data.getStoreImage()).placeholder(R.drawable.imv_logo).error(R.drawable.imv_logo).into(binding.skinDetailStoreImage);
        }

        binding.skinDetailResultsList.removeAllViews();
        if (data.getSkinResults() != null) {
            for (String result : data.getSkinResults()) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, 0, 0, 8);

                ImageView icon = new ImageView(requireContext());
                icon.setImageResource(R.drawable.ic_check_circle);
                icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
                icon.setLayoutParams(new LinearLayout.LayoutParams(40, 40));

                TextView text = new TextView(requireContext());
                text.setText(result);
                text.setTextColor(ContextCompat.getColor(requireContext(), R.color.content));
                text.setTextSize(10f);
                text.setPadding(12, 0, 0, 0);
                text.setMaxLines(2);
                text.setEllipsize(TextUtils.TruncateAt.END);

                row.addView(icon);
                row.addView(text);
                binding.skinDetailResultsList.addView(row);
            }
        }

        binding.skinDetailStoreName.setText(data.getStoreName());
        binding.skinDetailStoreAddress.setText(data.getStoreAddress());
        binding.skinDetailStorePhone.setText(data.getStorePhone());

        binding.skinDetailConsultantName.setText(data.getConsultantName());
        if (data.getConsultantAvatar() != null && !data.getConsultantAvatar().isEmpty()) {
            com.bumptech.glide.Glide.with(binding.skinDetailConsultantAvatar.getContext()).load(data.getConsultantAvatar()).placeholder(R.drawable.imv_logo).error(R.drawable.imv_logo).into(binding.skinDetailConsultantAvatar);
        }

        binding.skinDetailUserRatingNum.setText(String.format(Locale.US, "%.1f", data.getUserRating()));
        binding.skinDetailUserReviewText.setText("“" + data.getUserReview() + "”");
        binding.skinDetailReviewDate.setText(data.getReviewDate());

        if (data.getBeforeImage() != null && !data.getBeforeImage().isEmpty()) {
            com.bumptech.glide.Glide.with(binding.skinDetailBeforeImg.getContext()).load(data.getBeforeImage()).placeholder(R.drawable.imv_logo).error(R.drawable.imv_logo).into(binding.skinDetailBeforeImg);
        }

        if (data.getAfterImage() != null && !data.getAfterImage().isEmpty()) {
            com.bumptech.glide.Glide.with(binding.skinDetailAfterImg.getContext()).load(data.getAfterImage()).placeholder(R.drawable.imv_logo).error(R.drawable.imv_logo).into(binding.skinDetailAfterImg);
        }

        binding.skinDetailEarnedPoints.setText("+" + data.getEarnedPoints());
        binding.skinDetailTotalPoints.setText("Tổng điểm hiện tại: " + data.getTotalPoints() + " điểm");

        binding.skinDetailNextApptText.setText(data.getNextAppointmentText());
        binding.skinDetailNextApptDate.setText(data.getNextAppointmentDate());
    }

    private void showEditReviewDialog() {
        if (bookingData == null) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.skin_dialog_edit_review, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView btnClose = dialogView.findViewById(R.id.dialog_review_btn_close);
        SeekBar seekBar = dialogView.findViewById(R.id.dialog_review_seekbar);
        EditText etComment = dialogView.findViewById(R.id.dialog_review_comment);
        TextView tvCount = dialogView.findViewById(R.id.dialog_review_count);
        TextView tvRating = dialogView.findViewById(R.id.dialog_review_rating_text);
        TextView tvRatingLabel = dialogView.findViewById(R.id.dialog_review_rating_label);
        TextView tvDate = dialogView.findViewById(R.id.dialog_review_date);
        TextView btnSubmit = dialogView.findViewById(R.id.dialog_review_btn_submit);

        ImageView[] stars = new ImageView[] {
                dialogView.findViewById(R.id.dialog_review_star_1),
                dialogView.findViewById(R.id.dialog_review_star_2),
                dialogView.findViewById(R.id.dialog_review_star_3),
                dialogView.findViewById(R.id.dialog_review_star_4),
                dialogView.findViewById(R.id.dialog_review_star_5)
        };

        float initialRating = bookingData.getUserRating() > 0f ? bookingData.getUserRating() : 5f;
        int initialStars = Math.max(1, Math.min(5, Math.round(initialRating)));
        String initialComment = bookingData.getUserReview() != null ? bookingData.getUserReview() : "";

        seekBar.setProgress(initialStars - 1);
        etComment.setText(initialComment);
        tvCount.setText(initialComment.length() + "/500");
        tvDate.setText(bookingData.getReviewDate() != null && !bookingData.getReviewDate().isEmpty()
                ? bookingData.getReviewDate()
                : new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        updateReviewRatingUI(stars, initialStars, tvRating, tvRatingLabel);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateReviewRatingUI(stars, progress + 1, tvRating, tvRatingLabel);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        etComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCount.setText(s.length() + "/500");
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            float finalRating = seekBar.getProgress() + 1;
            String finalComment = etComment.getText().toString().trim();
            if (finalComment.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập nhận xét của bạn", Toast.LENGTH_SHORT).show();
                return;
            }
            String finalDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            showConfirmSubmitDialog(() -> {
                saveReviewChanges(finalRating, finalComment, finalDate);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showConfirmSubmitDialog(Runnable onConfirm) {
        View confirmView = LayoutInflater.from(requireContext()).inflate(R.layout.skin_dialog_confirm_review_submit, null);
        AlertDialog confirmDialog = new AlertDialog.Builder(requireContext())
                .setView(confirmView)
                .create();
        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        confirmView.findViewById(R.id.dialog_confirm_review_btn_cancel).setOnClickListener(v -> confirmDialog.dismiss());
        confirmView.findViewById(R.id.dialog_confirm_review_btn_ok).setOnClickListener(v -> {
            confirmDialog.dismiss();
            if (onConfirm != null) onConfirm.run();
        });
        confirmDialog.show();
    }

    private void updateReviewRatingUI(ImageView[] stars, int starCount, TextView tvRating, TextView tvLabel) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setColorFilter(i < starCount ? Color.parseColor("#FFC107") : Color.parseColor("#D8D8D8"));
        }
        tvRating.setText(String.format(Locale.US, "%.1f", (float) starCount));
        tvLabel.setText(getRatingLabel(starCount));
    }

    private String getRatingLabel(int rating) {
        switch (rating) {
            case 1: return "Rất tệ";
            case 2: return "Chưa tốt";
            case 3: return "Bình thường";
            case 4: return "Tốt";
            default: return "Tuyệt vời!";
        }
    }

    private void saveReviewChanges(float rating, String review, String reviewDate) {
        if (bookingData == null) return;
        binding.skinDetailUserRatingNum.setText(String.format(Locale.US, "%.1f", rating));
        binding.skinDetailUserReviewText.setText("“" + review + "”");
        binding.skinDetailReviewDate.setText(reviewDate);

        BookingHistoryEntity updated = new BookingHistoryEntity(
                bookingData.getId(),
                bookingData.getUserId(),
                bookingData.getUserName(),
                bookingData.getUserPhone(),
                bookingData.getUserEmail(),
                bookingData.getServiceName(),
                bookingData.getDateDisplay(),
                bookingData.getMonthDisplay(),
                bookingData.getDayOfWeek(),
                bookingData.getTime(),
                bookingData.getDuration(),
                bookingData.getStoreName(),
                bookingData.getStoreAddress(),
                bookingData.getStorePhone(),
                bookingData.getStoreImage(),
                bookingData.getNote(),
                bookingData.getStatus(),
                bookingData.getPolicy(),
                bookingData.getCreatedAt(),
                bookingData.getCompletedAt(),
                bookingData.getSkinResults(),
                bookingData.getConsultantName(),
                bookingData.getConsultantAvatar(),
                bookingData.getConsultantRating(),
                rating,
                review,
                reviewDate,
                bookingData.getBeforeImage(),
                bookingData.getAfterImage(),
                bookingData.getEarnedPoints(),
                bookingData.getTotalPoints(),
                bookingData.getNextAppointmentDate(),
                bookingData.getNextAppointmentText(),
                bookingData.getCancelledAt(),
                bookingData.getCancelReason()
        );
        bookingData = updated;

        ioExecutor.execute(() -> {
            LocalJsonReader localJsonReader = new LocalJsonReader(requireContext());
            localJsonReader.updateBookingReview(updated.getId(), rating, review, reviewDate);
            new FirestoreService().updateBookingReview(updated.getId(), rating, review, reviewDate);
        });
        Toast.makeText(requireContext(), "Đã cập nhật đánh giá", Toast.LENGTH_SHORT).show();
    }

    private void openBeforeAfterGallery() {
        if (bookingData == null) return;
        ArrayList<String> imageUrls = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        if (bookingData.getBeforeImage() != null && !bookingData.getBeforeImage().trim().isEmpty()) {
            imageUrls.add(bookingData.getBeforeImage().trim());
            labels.add("Trước");
        }
        if (bookingData.getAfterImage() != null && !bookingData.getAfterImage().trim().isEmpty()) {
            imageUrls.add(bookingData.getAfterImage().trim());
            labels.add("Sau");
        }

        if (imageUrls.isEmpty()) {
            Toast.makeText(requireContext(), "Chưa có ảnh trước/sau cho lịch hẹn này", Toast.LENGTH_SHORT).show();
            return;
        }

        BeforeAfterGalleryBottomSheet.newInstance(imageUrls, labels)
                .show(getParentFragmentManager(), "BeforeAfterGalleryBottomSheet");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }
}
