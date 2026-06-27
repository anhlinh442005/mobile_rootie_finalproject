package com.veganbeauty.app.features.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.utils.NavAppUtils;

public class HomeAboutUsFragment extends RootieFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_about_us_fragment, container, false);
    }

    @Override
    public void setupUI(@NonNull View view) {
        NavAppUtils.setupNavApp(this, view, R.id.nav_home);

        View searchBar = view.findViewById(R.id.home_header_search_bar);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> 
                Toast.makeText(getContext(), "Tính năng tìm kiếm đang phát triển", Toast.LENGTH_SHORT).show()
            );
        }

        View qrBtn = view.findViewById(R.id.home_header_qr_btn);
        if (qrBtn != null) {
            qrBtn.setOnClickListener(v -> 
                Toast.makeText(getContext(), "Mở trình quét mã QR", Toast.LENGTH_SHORT).show()
            );
        }

        View notifBtn = view.findViewById(R.id.home_header_notification_btn);
        if (notifBtn != null) {
            notifBtn.setOnClickListener(v -> 
                Toast.makeText(getContext(), "Không có thông báo mới", Toast.LENGTH_SHORT).show()
            );
        }
    }

    @Override
    protected void observeViewModel() {
        // Not used
    }
}
