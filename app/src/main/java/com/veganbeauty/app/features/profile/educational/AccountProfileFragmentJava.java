package com.veganbeauty.app.features.profile.educational;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.databinding.AccountProfileBinding;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

/**
 * FILE TÀI LIỆU HỌC TẬP - PHIÊN BẢN JAVA ĐỂ ĐỐI CHIẾU
 * Giúp bạn hiểu cách code giao diện này bằng ngôn ngữ Java tương tự như thầy dạy trên lớp.
 */
public class AccountProfileFragmentJava extends Fragment {

    private AccountProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Sử dụng ViewBinding để ánh xạ view trong Java
        binding = AccountProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
    }

    private void setupUI() {
        // Load ảnh đại diện từ mạng bằng thư viện Coil trong Java
        ImageRequest request = new ImageRequest.Builder(requireContext())
                .data("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80")
                .target(binding.ivAvatar)
                .transformations(new CircleCropTransformation())
                .crossfade(true)
                .placeholder(android.R.color.darker_gray)
                .build();
        Coil.imageLoader(requireContext()).enqueue(request);

        // Đăng ký sự kiện click cho các nút bấm trong Java (Sử dụng biểu thức Lambda rút gọn)
        binding.llSearch.setOnClickListener(v -> 
            Toast.makeText(getContext(), "Tính năng tìm kiếm đang phát triển", Toast.LENGTH_SHORT).show()
        );

        binding.btnQrScan.setOnClickListener(v -> 
            Toast.makeText(getContext(), "Mở trình quét mã QR", Toast.LENGTH_SHORT).show()
        );

        binding.btnNotification.setOnClickListener(v -> 
            Toast.makeText(getContext(), "Không có thông báo mới", Toast.LENGTH_SHORT).show()
        );

        // Sự kiện khi click vào dòng Địa chỉ giao hàng
        binding.getRoot().findViewById(com.veganbeauty.app.R.id.iv_chevron).setOnClickListener(v ->
            Toast.makeText(getContext(), "Chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Tránh rò rỉ bộ nhớ
    }
}
