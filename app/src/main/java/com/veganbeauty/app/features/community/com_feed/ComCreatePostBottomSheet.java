package com.veganbeauty.app.features.community.com_feed;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;

public class ComCreatePostBottomSheet extends BottomSheetDialogFragment {
    
    public static final String TAG = "ComCreatePostBottomSheet";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_bottom_sheet_create_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        view.findViewById(R.id.llCreateReel).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Thước phim đang được phát triển", Toast.LENGTH_SHORT).show();
            dismiss();
        });
        
        view.findViewById(R.id.llCreateArticle).setOnClickListener(v -> {
            dismiss();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    .replace(R.id.main_container, new CommunityCreatePostFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });
        
        view.findViewById(R.id.llCreateStory).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tin đang được phát triển", Toast.LENGTH_SHORT).show();
            dismiss();
        });
        
        view.findViewById(R.id.llCreateHighlight).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tin nổi bật đang được phát triển", Toast.LENGTH_SHORT).show();
            dismiss();
        });
        
        view.findViewById(R.id.llCreateLive).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Phát trực tiếp đang được phát triển", Toast.LENGTH_SHORT).show();
            dismiss();
        });
        
        view.findViewById(R.id.llCreateAi).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tính năng AI đang được phát triển", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }
}
