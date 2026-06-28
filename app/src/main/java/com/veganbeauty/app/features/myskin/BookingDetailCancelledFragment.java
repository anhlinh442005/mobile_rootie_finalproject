package com.veganbeauty.app.features.myskin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.databinding.SkinFragmentBookingDetailCancelledBinding;

public class BookingDetailCancelledFragment extends RootieFragment {

    private SkinFragmentBookingDetailCancelledBinding binding;
    private static BookingHistoryEntity bookingData = null;

    public static BookingDetailCancelledFragment newInstance(BookingHistoryEntity data) {
        BookingDetailCancelledFragment fragment = new BookingDetailCancelledFragment();
        bookingData = data;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SkinFragmentBookingDetailCancelledBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void setupUI(@NonNull View view) {
        binding.skinDetailBtnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.skinDetailBtnNotification.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
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
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Mã đặt lịch", bookingData != null ? bookingData.getId() : "");
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Đã sao chép mã đặt lịch", Toast.LENGTH_SHORT).show();
        });

        binding.skinDetailBtnContactStore.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Liên hệ chi nhánh clicked", Toast.LENGTH_SHORT).show();
        });

        binding.skinDetailBtnRebookNow.setOnClickListener(v -> navigateToBooking());
        binding.skinDetailBtnRebookBottom.setOnClickListener(v -> navigateToBooking());
        binding.skinDetailBtnBookOtherBottom.setOnClickListener(v -> navigateToBooking());
    }

    private void navigateToBooking() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new BookingFragment())
                .addToBackStack(null)
                .commit();
    }

    private void populateUI(BookingHistoryEntity data) {
        binding.skinDetailServiceName.setText(data.getServiceName());
        binding.skinDetailOrderId.setText("Mã đặt lịch: " + data.getId());
        binding.skinDetailDate.setText(data.getDateDisplay() + " • " + data.getDayOfWeek());
        binding.skinDetailTime.setText(data.getTime() + " (" + data.getDuration() + ")");

        if (data.getCancelledAt() != null && !data.getCancelledAt().isEmpty()) {
            binding.skinDetailCancelledAt.setText("Đã huỷ vào: " + data.getCancelledAt());
        }

        if (data.getStoreImage() != null && !data.getStoreImage().isEmpty()) {
            com.bumptech.glide.Glide.with(binding.skinDetailStoreImage.getContext()).load(data.getStoreImage()).placeholder(R.drawable.imv_logo).error(R.drawable.imv_logo).into(binding.skinDetailStoreImage);
        }

        if (data.getCancelReason() != null && !data.getCancelReason().isEmpty()) {
            binding.skinDetailCancelReason.setText(data.getCancelReason());
        }

        binding.skinDetailStoreName.setText(data.getStoreName());
        binding.skinDetailStoreAddress.setText(data.getStoreAddress());
        binding.skinDetailStorePhone.setText(data.getStorePhone());

        binding.skinDetailUserName.setText(data.getUserName());
        binding.skinDetailUserPhone.setText(data.getUserPhone());
        binding.skinDetailUserEmail.setText(data.getUserEmail());
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
