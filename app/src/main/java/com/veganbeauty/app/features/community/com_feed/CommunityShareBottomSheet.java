package com.veganbeauty.app.features.community.com_feed;

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

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;

public class CommunityShareBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "CommunityShareBottomSheet";
    private String videoId;
    private String videoUrl;

    public static CommunityShareBottomSheet newInstance(String videoId, String videoUrl) {
        CommunityShareBottomSheet fragment = new CommunityShareBottomSheet();
        Bundle args = new Bundle();
        args.putString("video_id", videoId);
        args.putString("video_url", videoUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoId = getArguments().getString("video_id");
            videoUrl = getArguments().getString("video_url");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_bottom_sheet_share, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.llRepost).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Đã đăng lại video", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        view.findViewById(R.id.llCopy).setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                String linkToCopy = videoUrl != null ? videoUrl : "https://rootie.com/video/" + videoId;
                ClipData clip = ClipData.newPlainText("Copied Link", linkToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "Đã sao chép liên kết", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });

        view.findViewById(R.id.llReport).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét.", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        view.findViewById(R.id.llSave).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Đang tải video xuống...", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        view.findViewById(R.id.llNotInterested).setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chúng tôi sẽ ẩn bớt các video tương tự.", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }
}
