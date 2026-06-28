package com.veganbeauty.app.features.home.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.databinding.HomeItemBannerBinding;

import java.util.ArrayList;
import java.util.List;

public class HomeBannerAdapter extends RecyclerView.Adapter<HomeBannerAdapter.BannerViewHolder> {

    private final List<HomeBannerItem> items = new ArrayList<>();

    public void submitBanners(List<HomeBannerItem> banners) {
        items.clear();
        items.addAll(banners);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HomeItemBannerBinding binding = HomeItemBannerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new BannerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        private final HomeItemBannerBinding binding;

        public BannerViewHolder(HomeItemBannerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(HomeBannerItem item) {
            boolean hasOverlay = (item.getTitle() != null && !item.getTitle().trim().isEmpty()) ||
                                 (item.getActionText() != null && !item.getActionText().trim().isEmpty());
            binding.llBannerOverlay.setVisibility(hasOverlay ? View.VISIBLE : View.GONE);
            binding.tvBannerTitle.setText(item.getTitle() != null ? item.getTitle() : "");
            binding.btnBannerAction.setText(item.getActionText() != null ? item.getActionText() : "");

            if (item.getImageRes() != null) {
                binding.ivBanner.setImageResource(item.getImageRes());
            } else if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()) {
                com.bumptech.glide.Glide.with(binding.ivBanner.getContext()).load(item.getImageUrl()).into(binding.ivBanner);
            }
        }
    }
}
