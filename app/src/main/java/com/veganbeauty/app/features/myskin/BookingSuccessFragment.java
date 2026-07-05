package com.veganbeauty.app.features.myskin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.features.home.HomeFragment;

public class BookingSuccessFragment extends RootieFragment {

    private String bookingId;
    private String storeName;
    private String dateTime;
    private String specialist;
    private String serviceName;

    public static BookingSuccessFragment newInstance(String bookingId, String storeName, String dateTime, String specialist, String serviceName) {
        Bundle args = new Bundle();
        args.putString("BOOKING_ID", bookingId);
        args.putString("STORE_NAME", storeName);
        args.putString("DATE_TIME", dateTime);
        args.putString("SPECIALIST", specialist);
        args.putString("SERVICE_NAME", serviceName);
        BookingSuccessFragment fragment = new BookingSuccessFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.skin_fragment_booking_success, container, false);
    }

    @Override
    public void setupUI(@NonNull View view) {
        if (getArguments() != null) {
            bookingId = getArguments().getString("BOOKING_ID", "");
            storeName = getArguments().getString("STORE_NAME", "Rootie Gò Vấp");
            dateTime = getArguments().getString("DATE_TIME", "");
            specialist = getArguments().getString("SPECIALIST", "Nguyễn Khánh Xuân");
            serviceName = getArguments().getString("SERVICE_NAME", "");
        } else {
            bookingId = "";
            storeName = "Rootie Gò Vấp";
            dateTime = "";
            specialist = "Nguyễn Khánh Xuân";
            serviceName = "";
        }

        TextView tvBookingId = view.findViewById(R.id.info_booking_id);
        TextView tvStoreName = view.findViewById(R.id.info_store_name);
        TextView tvDateTime = view.findViewById(R.id.info_date_time);
        TextView tvSpecialist = view.findViewById(R.id.info_specialist);
        TextView tvService = view.findViewById(R.id.info_service);
        
        ImageView btnBack = view.findViewById(R.id.btn_back);
        TextView btnHome = view.findViewById(R.id.btn_home);

        tvBookingId.setText(bookingId);
        tvStoreName.setText(storeName);
        tvDateTime.setText(dateTime);
        tvSpecialist.setText(specialist);
        tvService.setText(serviceName);

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnHome.setOnClickListener(v -> {
            FragmentManager fm = getParentFragmentManager();
            for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
                fm.popBackStack();
            }
            fm.beginTransaction()
                .replace(R.id.main_container, new HomeFragment())
                .commit();
        });
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }
}
