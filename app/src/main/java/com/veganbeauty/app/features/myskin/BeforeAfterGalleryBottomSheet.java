package com.veganbeauty.app.features.myskin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.veganbeauty.app.R;
import com.veganbeauty.app.utils.ImageSaveHelper;

import java.util.ArrayList;
import java.util.List;

public class BeforeAfterGalleryBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_IMAGES = "arg_images";
    private static final String ARG_LABELS = "arg_labels";

    private ActivityResultLauncher<String> storagePermissionLauncher;
    private String pendingImageUrl;
    private String pendingLabel;
    private boolean isSavingImage;

    public static BeforeAfterGalleryBottomSheet newInstance(ArrayList<String> imageUrls, ArrayList<String> labels) {
        BeforeAfterGalleryBottomSheet sheet = new BeforeAfterGalleryBottomSheet();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_IMAGES, imageUrls);
        args.putStringArrayList(ARG_LABELS, labels);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted && pendingImageUrl != null) {
                        saveCurrentImage(pendingImageUrl, pendingLabel);
                    } else if (!granted) {
                        Toast.makeText(requireContext(), "Cần quyền lưu ảnh vào thư viện", Toast.LENGTH_SHORT).show();
                    }
                    pendingImageUrl = null;
                    pendingLabel = null;
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.skin_bottom_sheet_before_after_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayList<String> images = getArguments() != null ? getArguments().getStringArrayList(ARG_IMAGES) : null;
        if (images == null) images = new ArrayList<>();

        ArrayList<String> textLabels = getArguments() != null ? getArguments().getStringArrayList(ARG_LABELS) : null;
        if (textLabels == null || textLabels.size() != images.size()) {
            textLabels = new ArrayList<>();
            for (int i = 0; i < images.size(); i++) {
                textLabels.add(i == 0 ? "Trước" : "Sau");
            }
        }

        final ArrayList<String> imageUrls = images;
        final ArrayList<String> labels = textLabels;

        if (imageUrls.isEmpty()) {
            Toast.makeText(requireContext(), "Không có ảnh để hiển thị", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        ImageView btnClose = view.findViewById(R.id.gallery_btn_close);
        ImageView btnDownload = view.findViewById(R.id.gallery_btn_download);
        TextView pageIndicator = view.findViewById(R.id.gallery_page_indicator);
        ViewPager2 viewPager = view.findViewById(R.id.gallery_view_pager);
        RecyclerView thumbs = view.findViewById(R.id.gallery_thumb_list);

        GalleryPagerAdapter pagerAdapter = new GalleryPagerAdapter(imageUrls, labels);
        GalleryThumbAdapter thumbAdapter = new GalleryThumbAdapter(imageUrls, labels, position -> viewPager.setCurrentItem(position, true));

        viewPager.setAdapter(pagerAdapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                pageIndicator.setText((position + 1) + "/" + imageUrls.size());
                thumbAdapter.setSelected(position);
                thumbs.smoothScrollToPosition(position);
            }
        });

        thumbs.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        thumbs.setAdapter(thumbAdapter);
        thumbAdapter.setSelected(0);
        pageIndicator.setText("1/" + imageUrls.size());

        btnClose.setOnClickListener(v -> dismiss());
        btnDownload.setOnClickListener(v -> {
            if (isSavingImage) return;
            int current = viewPager.getCurrentItem();
            String url = imageUrls.get(current);
            String label = labels.get(current);
            if (needsStoragePermission()) {
                pendingImageUrl = url;
                pendingLabel = label;
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
            saveCurrentImage(url, label);
        });
    }

    private boolean needsStoragePermission() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED;
    }

    private void saveCurrentImage(String imageUrl, String label) {
        isSavingImage = true;
        Toast.makeText(requireContext(), "Đang lưu ảnh...", Toast.LENGTH_SHORT).show();
        ImageSaveHelper.saveImageFromUrl(requireContext(), imageUrl, label, new ImageSaveHelper.Callback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                isSavingImage = false;
                Toast.makeText(requireContext(), "Đã lưu ảnh vào thư viện", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                isSavingImage = false;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class GalleryPagerAdapter extends RecyclerView.Adapter<GalleryPagerAdapter.PageVH> {
        private final List<String> images;
        private final List<String> labels;

        GalleryPagerAdapter(List<String> images, List<String> labels) {
            this.images = images;
            this.labels = labels;
        }

        @NonNull
        @Override
        public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.skin_item_gallery_page, parent, false);
            return new PageVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageVH holder, int position) {
            String image = images.get(position);
            Glide.with(holder.image.getContext())
                    .load(image)
                    .placeholder(R.drawable.imv_logo)
                    .error(R.drawable.imv_logo)
                    .into(holder.image);
            holder.label.setText(labels.get(position));
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        static class PageVH extends RecyclerView.ViewHolder {
            final ImageView image;
            final TextView label;

            PageVH(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.gallery_page_image);
                label = itemView.findViewById(R.id.gallery_page_label);
            }
        }
    }

    private static class GalleryThumbAdapter extends RecyclerView.Adapter<GalleryThumbAdapter.ThumbVH> {
        interface OnThumbClickListener { void onClick(int position); }

        private final List<String> images;
        private final List<String> labels;
        private final OnThumbClickListener onThumbClick;
        private int selected = 0;

        GalleryThumbAdapter(List<String> images, List<String> labels, OnThumbClickListener onThumbClick) {
            this.images = images;
            this.labels = labels;
            this.onThumbClick = onThumbClick;
        }

        void setSelected(int position) {
            int old = selected;
            selected = position;
            notifyItemChanged(old);
            notifyItemChanged(selected);
        }

        @NonNull
        @Override
        public ThumbVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.skin_item_gallery_thumb, parent, false);
            return new ThumbVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ThumbVH holder, int position) {
            Glide.with(holder.image.getContext())
                    .load(images.get(position))
                    .placeholder(R.drawable.imv_logo)
                    .error(R.drawable.imv_logo)
                    .into(holder.image);
            holder.label.setText(labels.get(position));
            holder.container.setAlpha(position == selected ? 1f : 0.55f);
            holder.container.setOnClickListener(v -> {
                if (onThumbClick != null) onThumbClick.onClick(position);
            });
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        static class ThumbVH extends RecyclerView.ViewHolder {
            final View container;
            final ImageView image;
            final TextView label;

            ThumbVH(View itemView) {
                super(itemView);
                container = itemView.findViewById(R.id.gallery_thumb_container);
                image = itemView.findViewById(R.id.gallery_thumb_image);
                label = itemView.findViewById(R.id.gallery_thumb_label);
            }
        }
    }
}
