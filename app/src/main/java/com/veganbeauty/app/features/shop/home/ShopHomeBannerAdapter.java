package com.veganbeauty.app.features.shop.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.databinding.ShopHomeBannerBinding;
import com.veganbeauty.app.features.shop.home.models.BannerUiModel;

public class ShopHomeBannerAdapter extends ListAdapter<BannerUiModel, ShopHomeBannerAdapter.BannerViewHolder> {

    public ShopHomeBannerAdapter() {
        super(new DiffCallback());
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopHomeBannerBinding binding = ShopHomeBannerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new BannerViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        private final ShopHomeBannerBinding binding;

        public BannerViewHolder(@NonNull ShopHomeBannerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(BannerUiModel banner) {
            binding.ivBanner.setImageResource(banner.getImageRes());
        }
    }

    public static class DiffCallback extends DiffUtil.ItemCallback<BannerUiModel> {
        @Override
        public boolean areItemsTheSame(@NonNull BannerUiModel oldItem, @NonNull BannerUiModel newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BannerUiModel oldItem, @NonNull BannerUiModel newItem) {
            return oldItem.equals(newItem);
        }
    }
}
