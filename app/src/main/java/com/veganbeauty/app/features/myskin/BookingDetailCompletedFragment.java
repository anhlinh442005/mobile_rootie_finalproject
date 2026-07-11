package com.veganbeauty.app.features.myskin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.databinding.SkinFragmentBookingDetailCompletedBinding;
import com.veganbeauty.app.utils.CoinRewardDialogHelper;
import com.veganbeauty.app.utils.FeedbackImageUploadHelper;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookingDetailCompletedFragment extends RootieFragment {

    private static final int REVIEW_BONUS_POINTS = 50;

    private SkinFragmentBookingDetailCompletedBinding binding;
    private SkinDetailHeaderScrollHelper headerScrollHelper;
    private static BookingHistoryEntity bookingData = null;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final List<String> feedbackImages = new ArrayList<>();

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    addFeedbackImageUri(uri);
                }
            }
    );

    private final ActivityResultLauncher<String> pickMultipleImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris == null || uris.isEmpty()) {
                    return;
                }
                for (Uri uri : uris) {
                    addFeedbackImageUri(uri);
                }
            }
    );

    private final ActivityResultLauncher<Void> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    String path = saveBitmapToCache(bitmap);
                    if (path != null) {
                        addFeedbackImagePath(path);
                    }
                }
            }
    );

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
        binding.skinDetailBtnAddReview.setOnClickListener(v -> showEditReviewDialog());
        binding.skinDetailReviewPrompt.setOnClickListener(v -> showEditReviewDialog());
        binding.skinDetailBtnViewScanReport.setOnClickListener(v -> openSkinScanReport());
        setupFeedbackImages();
        setupScrollHideHeader();
    }

    private void setupScrollHideHeader() {
        headerScrollHelper = new SkinDetailHeaderScrollHelper(
                binding.skinDetailHeader,
                binding.skinDetailScroll,
                0
        );
        headerScrollHelper.attachToNestedScrollView(binding.skinDetailScroll);
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

        binding.skinDetailStoreImage.setImageResource(R.drawable.mascot_success);

        binding.skinDetailResultsList.removeAllViews();
        if (data.getSkinResults() != null) {
            for (String result : data.getSkinResults()) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, 0, 0, 8);

                ImageView icon = new ImageView(requireContext());
                icon.setImageResource(R.drawable.ic_circle_checked);
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

        updateReviewSection(data);
        refreshFeedbackImagesFromData(data);

        binding.skinDetailEarnedPoints.setText("+" + data.getEarnedPoints());
        binding.skinDetailTotalPoints.setText("Tổng điểm hiện tại: " + data.getTotalPoints() + " điểm");

        binding.skinDetailNextApptText.setText(data.getNextAppointmentText());
        binding.skinDetailNextApptDate.setText(data.getNextAppointmentDate());
    }

    private boolean hasUserReview(BookingHistoryEntity data) {
        return data != null
                && data.getUserRating() > 0f
                && !TextUtils.isEmpty(data.getUserReview().trim());
    }

    private void updateReviewSection(BookingHistoryEntity data) {
        if (hasUserReview(data)) {
            binding.skinDetailReviewPrompt.setVisibility(View.GONE);
            binding.skinDetailReviewSubmitted.setVisibility(View.VISIBLE);

            int starCount = Math.max(1, Math.min(5, Math.round(data.getUserRating())));
            binding.skinDetailUserRatingNum.setText(String.format(Locale.US, "%.1f", data.getUserRating()));
            binding.skinDetailUserReviewText.setText("“" + data.getUserReview() + "”");
            String reviewDate = data.getReviewDate();
            binding.skinDetailReviewDate.setText(
                    reviewDate != null && !reviewDate.isEmpty()
                            ? "Đánh giá ngày " + reviewDate
                            : "");
            binding.skinDetailRatingLabel.setText(getRatingLabel(starCount));

            ImageView[] stars = new ImageView[] {
                    binding.skinDetailStar1,
                    binding.skinDetailStar2,
                    binding.skinDetailStar3,
                    binding.skinDetailStar4,
                    binding.skinDetailStar5
            };
            for (int i = 0; i < stars.length; i++) {
                boolean filled = i < starCount;
                stars[i].setImageResource(filled ? R.drawable.ic_star : R.drawable.ic_star_outline);
                stars[i].setColorFilter(filled ? Color.parseColor("#FFC107") : Color.parseColor("#D0D0D0"));
            }
        } else {
            binding.skinDetailReviewPrompt.setVisibility(View.VISIBLE);
            binding.skinDetailReviewSubmitted.setVisibility(View.GONE);
        }
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
        TextView tvTitle = dialogView.findViewById(R.id.dialog_review_title);
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

        boolean isFirstReview = !hasUserReview(bookingData);
        tvTitle.setText(isFirstReview ? "Đánh giá dịch vụ" : "Chỉnh sửa đánh giá");
        btnSubmit.setText(isFirstReview ? "Gửi đánh giá" : "Cập nhật đánh giá");

        float initialRating = bookingData.getUserRating() > 0f ? bookingData.getUserRating() : 5f;
        int initialStars = Math.max(1, Math.min(5, Math.round(initialRating)));
        String initialComment = bookingData.getUserReview() != null ? bookingData.getUserReview() : "";
        final int[] selectedStars = {initialStars};

        etComment.setText(initialComment);
        tvCount.setText(initialComment.length() + "/500");
        tvDate.setText(bookingData.getReviewDate() != null && !bookingData.getReviewDate().isEmpty()
                ? bookingData.getReviewDate()
                : new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        updateReviewRatingUI(stars, selectedStars[0], tvRating, tvRatingLabel);
        setupStarClickListeners(stars, selectedStars, tvRating, tvRatingLabel);

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
            float finalRating = selectedStars[0];
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

    private void setupStarClickListeners(ImageView[] stars, int[] selectedStars,
                                         TextView tvRating, TextView tvRatingLabel) {
        for (int i = 0; i < stars.length; i++) {
            final int starValue = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedStars[0] = starValue;
                updateReviewRatingUI(stars, starValue, tvRating, tvRatingLabel);
            });
        }
    }

    private void updateReviewRatingUI(ImageView[] stars, int starCount, TextView tvRating, TextView tvLabel) {
        for (int i = 0; i < stars.length; i++) {
            boolean filled = i < starCount;
            stars[i].setImageResource(filled ? R.drawable.ic_star : R.drawable.ic_star_outline);
            stars[i].setColorFilter(filled ? Color.parseColor("#FFC107") : Color.parseColor("#D0D0D0"));
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

        boolean isFirstReview = !hasUserReview(bookingData);
        int earnedPoints = bookingData.getEarnedPoints();
        int totalPoints = bookingData.getTotalPoints();
        if (isFirstReview) {
            earnedPoints += REVIEW_BONUS_POINTS;
            totalPoints += REVIEW_BONUS_POINTS;
        }

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
                earnedPoints,
                totalPoints,
                bookingData.getNextAppointmentDate(),
                bookingData.getNextAppointmentText(),
                bookingData.getCancelledAt(),
                bookingData.getCancelReason(),
                new ArrayList<>(feedbackImages)
        );
        bookingData = updated;

        updateReviewSection(updated);
        binding.skinDetailEarnedPoints.setText("+" + earnedPoints);
        binding.skinDetailTotalPoints.setText("Tổng điểm hiện tại: " + totalPoints + " điểm");

        final int finalEarnedPoints = earnedPoints;
        final int finalTotalPoints = totalPoints;
        if (isFirstReview) {
            final String bookingId = updated.getId();
            if (!isAdded()) {
                return;
            }
            final androidx.fragment.app.FragmentActivity hostActivity = getActivity();
            final Context appCtx = requireContext().getApplicationContext();
            ioExecutor.execute(() -> {
                LocalJsonReader localJsonReader = new LocalJsonReader(appCtx);
                localJsonReader.updateBookingReview(
                        updated.getId(), rating, review, reviewDate, finalEarnedPoints, finalTotalPoints);
                new FirestoreService().updateBookingReview(
                        updated.getId(), rating, review, reviewDate, finalEarnedPoints, finalTotalPoints);
                int totalBalance = com.veganbeauty.app.utils.RewardPointsHelper.awardPoints(
                        appCtx,
                        "BOOKING_REVIEW_" + bookingId,
                        REVIEW_BONUS_POINTS,
                        "Đánh giá lịch hẹn " + bookingId,
                        "từ đánh giá lịch hẹn",
                        false
                );
                if (hostActivity != null) {
                    hostActivity.runOnUiThread(() -> {
                        if (!isAdded()) {
                            return;
                        }
                        CoinRewardDialogHelper.showWithDismissCallback(
                                this,
                                REVIEW_BONUS_POINTS,
                                totalBalance,
                                "từ đánh giá lịch hẹn",
                                null
                        );
                    });
                }
            });
        } else {
            ioExecutor.execute(() -> {
                LocalJsonReader localJsonReader = new LocalJsonReader(requireContext());
                localJsonReader.updateBookingReview(
                        updated.getId(), rating, review, reviewDate, finalEarnedPoints, finalTotalPoints);
                new FirestoreService().updateBookingReview(
                        updated.getId(), rating, review, reviewDate, finalEarnedPoints, finalTotalPoints);
            });
            Toast.makeText(requireContext(), "Đã cập nhật đánh giá", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFeedbackImages() {
        refreshFeedbackImagesUi();
    }

    private void refreshFeedbackImagesFromData(BookingHistoryEntity data) {
        feedbackImages.clear();
        if (data.getUserFeedbackImages() != null) {
            for (String imageUrl : data.getUserFeedbackImages()) {
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    feedbackImages.add(imageUrl.trim());
                }
            }
        }
        refreshFeedbackImagesUi();
    }

    private void refreshFeedbackImagesUi() {
        if (binding == null) {
            return;
        }
        binding.skinDetailFeedbackImagesContainer.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int thumbSize = (int) (72 * density);
        int thumbMargin = (int) (8 * density);

        for (int i = 0; i < feedbackImages.size(); i++) {
            binding.skinDetailFeedbackImagesContainer.addView(
                    createFeedbackThumbnailView(feedbackImages.get(i), i, thumbSize, thumbMargin));
        }
        binding.skinDetailFeedbackImagesContainer.addView(createAddFeedbackImageView(thumbSize, thumbMargin));
    }

    private View createAddFeedbackImageView(int size, int marginEnd) {
        FrameLayout container = new FrameLayout(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMarginEnd(marginEnd);
        container.setLayoutParams(lp);
        container.setBackgroundResource(R.drawable.skin_bg_outline);
        container.setClickable(true);
        container.setFocusable(true);
        container.setOnClickListener(v -> showAddFeedbackImageDialog());

        ImageView icon = new ImageView(requireContext());
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(
                (int) (24 * getResources().getDisplayMetrics().density),
                (int) (24 * getResources().getDisplayMetrics().density),
                Gravity.CENTER
        );
        icon.setLayoutParams(iconLp);
        icon.setImageResource(R.drawable.ic_plus);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));
        container.addView(icon);
        return container;
    }

    private View createFeedbackThumbnailView(String imagePath, int index, int size, int marginEnd) {
        FrameLayout container = new FrameLayout(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMarginEnd(marginEnd);
        container.setLayoutParams(lp);

        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        com.bumptech.glide.Glide.with(requireContext()).load(imagePath).centerCrop().into(imageView);
        imageView.setOnClickListener(v -> openFeedbackGallery(index));
        container.addView(imageView);

        ImageView removeBtn = new ImageView(requireContext());
        int removeSize = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams removeLp = new FrameLayout.LayoutParams(removeSize, removeSize, Gravity.TOP | Gravity.END);
        removeLp.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        removeLp.setMarginEnd((int) (4 * getResources().getDisplayMetrics().density));
        removeBtn.setLayoutParams(removeLp);
        removeBtn.setImageResource(R.drawable.ic_circle_unchecked);
        removeBtn.setBackgroundResource(R.drawable.home_bg_notification_badge);
        removeBtn.setPadding(4, 4, 4, 4);
        removeBtn.setColorFilter(Color.WHITE);
        removeBtn.setOnClickListener(v -> removeFeedbackImage(index));
        container.addView(removeBtn);
        return container;
    }

    private void showAddFeedbackImageDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Thêm hình ảnh feedback")
                .setItems(new CharSequence[]{
                        "Chụp ảnh",
                        "Chọn 1 ảnh",
                        "Chọn nhiều ảnh"
                }, (dialog, which) -> {
                    if (which == 0) {
                        takePhotoLauncher.launch(null);
                    } else if (which == 1) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        pickMultipleImagesLauncher.launch("image/*");
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void addFeedbackImageUri(Uri uri) {
        String path = copyUriToCache(uri);
        if (path != null) {
            addFeedbackImagePath(path);
        } else {
            Toast.makeText(requireContext(), "Không thể đọc ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void addFeedbackImagePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        feedbackImages.add(path.trim());
        saveFeedbackImages();
        refreshFeedbackImagesUi();
        Toast.makeText(requireContext(), "Đã thêm ảnh feedback", Toast.LENGTH_SHORT).show();
    }

    private void removeFeedbackImage(int index) {
        if (index < 0 || index >= feedbackImages.size()) {
            return;
        }
        feedbackImages.remove(index);
        saveFeedbackImages();
        refreshFeedbackImagesUi();
    }

    private void saveFeedbackImages() {
        if (bookingData == null) {
            return;
        }
        bookingData = copyBookingWithFeedbackImages(feedbackImages);
        final List<String> imagesToSave = new ArrayList<>(feedbackImages);
        ioExecutor.execute(() -> {
            try {
                List<String> remoteImages = FeedbackImageUploadHelper.ensureRemoteUrls(
                        requireContext().getApplicationContext(),
                        imagesToSave,
                        "feedback/bookings/" + bookingData.getId()
                );
                LocalJsonReader localJsonReader = new LocalJsonReader(requireContext());
                localJsonReader.updateBookingFeedbackImages(bookingData.getId(), remoteImages);
                new FirestoreService().updateBookingFeedbackImages(bookingData.getId(), remoteImages);
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Không thể tải ảnh feedback lên cloud", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private BookingHistoryEntity copyBookingWithFeedbackImages(List<String> images) {
        return new BookingHistoryEntity(
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
                bookingData.getUserRating(),
                bookingData.getUserReview(),
                bookingData.getReviewDate(),
                bookingData.getBeforeImage(),
                bookingData.getAfterImage(),
                bookingData.getEarnedPoints(),
                bookingData.getTotalPoints(),
                bookingData.getNextAppointmentDate(),
                bookingData.getNextAppointmentText(),
                bookingData.getCancelledAt(),
                bookingData.getCancelReason(),
                new ArrayList<>(images)
        );
    }

    private String copyUriToCache(Uri uri) {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            File file = new File(requireContext().getCacheDir(),
                    "booking_feedback_" + System.currentTimeMillis() + ".jpg");
            try (OutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String saveBitmapToCache(Bitmap bitmap) {
        try {
            File file = new File(requireContext().getCacheDir(),
                    "booking_feedback_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
                out.flush();
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openFeedbackGallery(int startIndex) {
        if (feedbackImages.isEmpty()) {
            return;
        }
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < feedbackImages.size(); i++) {
            labels.add("Ảnh " + (i + 1));
        }
        BeforeAfterGalleryBottomSheet.newInstance(new ArrayList<>(feedbackImages), labels)
                .show(getParentFragmentManager(), "FeedbackGalleryBottomSheet");
    }

    private void openSkinScanReport() {
        if (bookingData == null) {
            return;
        }

        Toast.makeText(requireContext(), "Đang tải báo cáo soi da...", Toast.LENGTH_SHORT).show();
        ioExecutor.execute(() -> {
            JSONObject scanResult = BookingSkinScanResultHelper.resolveScanResult(
                    requireContext().getApplicationContext(), bookingData);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (scanResult == null) {
                    Toast.makeText(requireContext(), "Chưa có báo cáo soi da cho lịch hẹn này", Toast.LENGTH_SHORT).show();
                    return;
                }
                SkinScanResultDialogFragment dialog = SkinScanResultDialogFragment.newInstance(scanResult.toString());
                dialog.show(getParentFragmentManager(), "BookingSkinScanResultDialog");
            });
        });
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
