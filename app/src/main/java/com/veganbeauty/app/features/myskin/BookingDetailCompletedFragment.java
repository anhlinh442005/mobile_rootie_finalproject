package com.veganbeauty.app.features.myskin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import coil.Coil;
import coil.request.ImageRequest;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.databinding.SkinFragmentBookingDetailCompletedBinding;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;

public class BookingDetailCompletedFragment extends RootieFragment {

    private SkinFragmentBookingDetailCompletedBinding binding;
    private static BookingHistoryEntity bookingData = null;

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
            ImageRequest request = new ImageRequest.Builder(requireContext())
                    .data(data.getStoreImage())
                    .placeholder(R.drawable.imv_logo)
                    .error(R.drawable.imv_logo)
                    .crossfade(true)
                    .target(binding.skinDetailStoreImage)
                    .build();
            Coil.imageLoader(requireContext()).enqueue(request);
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
            ImageRequest request = new ImageRequest.Builder(requireContext())
                    .data(data.getConsultantAvatar())
                    .placeholder(R.drawable.imv_logo)
                    .error(R.drawable.imv_logo)
                    .crossfade(true)
                    .target(binding.skinDetailConsultantAvatar)
                    .build();
            Coil.imageLoader(requireContext()).enqueue(request);
        }

        binding.skinDetailUserRatingNum.setText(String.format(java.util.Locale.US, "%.1f", data.getUserRating()));
        binding.skinDetailUserReviewText.setText("“" + data.getUserReview() + "”");
        binding.skinDetailReviewDate.setText(data.getReviewDate());

        if (data.getBeforeImage() != null && !data.getBeforeImage().isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(requireContext())
                    .data(data.getBeforeImage())
                    .placeholder(R.drawable.imv_logo)
                    .error(R.drawable.imv_logo)
                    .target(binding.skinDetailBeforeImg)
                    .build();
            Coil.imageLoader(requireContext()).enqueue(request);
        }

        if (data.getAfterImage() != null && !data.getAfterImage().isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(requireContext())
                    .data(data.getAfterImage())
                    .placeholder(R.drawable.imv_logo)
                    .error(R.drawable.imv_logo)
                    .target(binding.skinDetailAfterImg)
                    .build();
            Coil.imageLoader(requireContext()).enqueue(request);
        }

        binding.skinDetailEarnedPoints.setText("+" + data.getEarnedPoints());
        binding.skinDetailTotalPoints.setText("Tổng điểm hiện tại: " + data.getTotalPoints() + " điểm");

        binding.skinDetailNextApptText.setText(data.getNextAppointmentText());
        binding.skinDetailNextApptDate.setText(data.getNextAppointmentDate());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
