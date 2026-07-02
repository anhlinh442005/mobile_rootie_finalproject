package com.veganbeauty.app.features.community.profile;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;

public class CommunityProfileOptionsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "CommunityProfileOptionsBottomSheet";
    private static final String ARG_USER_NAME = "ARG_USER_NAME";
    private static final String ARG_USER_ID = "ARG_USER_ID";

    private String userName = "";
    private String userId = "";

    public static CommunityProfileOptionsBottomSheet newInstance(String userName, String userId) {
        CommunityProfileOptionsBottomSheet sheet = new CommunityProfileOptionsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_USER_NAME, userName);
        args.putString(ARG_USER_ID, userId);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString(ARG_USER_NAME, "");
            userId = getArguments().getString(ARG_USER_ID, "");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetDialog sheetDialog = (BottomSheetDialog) dialog;
            sheetDialog.setOnShowListener(d -> {
                View bottomSheet = sheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    bottomSheet.setBackgroundResource(android.R.color.transparent);
                    BottomSheetBehavior.from(bottomSheet).setSkipCollapsed(true);
                }
            });
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(0.55f);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        if (dialog instanceof BottomSheetDialog) {
            View bottomSheet = ((BottomSheetDialog) dialog).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.com_bottom_sheet_profile_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.tvCancel).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.tvReport).setOnClickListener(v -> {
            showComingSoon();
            dismiss();
        });
        view.findViewById(R.id.tvRestrict).setOnClickListener(v -> {
            showComingSoon();
            dismiss();
        });
        view.findViewById(R.id.tvBlock).setOnClickListener(v -> {
            showComingSoon();
            dismiss();
        });
        view.findViewById(R.id.tvAboutAccount).setOnClickListener(v -> {
            showComingSoon();
            dismiss();
        });
        view.findViewById(R.id.tvSharedActivity).setOnClickListener(v -> {
            showComingSoon();
            dismiss();
        });
        view.findViewById(R.id.tvHideStory).setOnClickListener(v -> {
            showComingSoon();
            dismiss();
        });
        view.findViewById(R.id.tvCopyUrl).setOnClickListener(v -> {
            copyProfileUrl();
            dismiss();
        });
        view.findViewById(R.id.tvShareProfile).setOnClickListener(v -> {
            shareProfile();
            dismiss();
        });
        view.findViewById(R.id.tvQrCode).setOnClickListener(v -> {
            showComingSoon();
            dismiss();
        });
    }

    private void showComingSoon() {
        Toast.makeText(requireContext(), "Tính năng sẽ sớm ra mắt", Toast.LENGTH_SHORT).show();
    }

    private void copyProfileUrl() {
        String profileUrl = "https://rootie.app/profile/" + userId;
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("profile_url", profileUrl));
            Toast.makeText(requireContext(), "Đã sao chép URL trang cá nhân", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareProfile() {
        String profileUrl = "https://rootie.app/profile/" + userId;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, userName);
        shareIntent.putExtra(Intent.EXTRA_TEXT, userName + "\n" + profileUrl);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ trang cá nhân"));
    }
}
