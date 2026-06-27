package com.veganbeauty.app.features.shop.search;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;
import com.veganbeauty.app.databinding.ShopSearchVideoItemBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchVideoAdapter extends RecyclerView.Adapter<SearchVideoAdapter.ViewHolder> {

    private final List<YtVideoEntity> items = new ArrayList<>();

    public void submitList(List<YtVideoEntity> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopSearchVideoItemBinding binding = ShopSearchVideoItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String extractYouTubeVideoId(String url) {
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|shorts\\/)[^#\\&\\?\\n]*";
        Matcher matcher = Pattern.compile(pattern).matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ShopSearchVideoItemBinding binding;

        public ViewHolder(ShopSearchVideoItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(YtVideoEntity video) {
            binding.tvTitle.setText(video.getTitle());
            String desc = (video.getDescription() != null && !video.getDescription().trim().isEmpty()) 
                          ? video.getDescription() : video.getUsername();
            binding.tvDescription.setText(desc);

            String videoId = extractYouTubeVideoId(video.getUrl());
            String thumbnailUrl = video.getUrl();

            if (videoId != null) {
                thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
            } else if (video.getUrl() != null && video.getUrl().contains("cloudinary.com") && video.getUrl().toLowerCase().endsWith(".mp4")) {
                int lastDotIndex = video.getUrl().lastIndexOf('.');
                if (lastDotIndex != -1) {
                    thumbnailUrl = video.getUrl().substring(0, lastDotIndex) + ".jpg";
                }
            }

            if (thumbnailUrl != null) {
                ImageRequest request = new ImageRequest.Builder(binding.getRoot().getContext())
                        .data(thumbnailUrl)
                        .target(binding.ivThumbnail)
                        .crossfade(true)
                        .placeholder(android.R.color.darker_gray)
                        .build();
                Coil.imageLoader(binding.getRoot().getContext()).enqueue(request);
            }

            binding.getRoot().setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getUrl()));
                    binding.getRoot().getContext().startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
