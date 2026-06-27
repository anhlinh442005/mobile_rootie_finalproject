package com.veganbeauty.app.features.shop.product.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.databinding.ShopItemReviewBinding;

import java.util.List;

public class ShopReviewAdapter extends RecyclerView.Adapter<ShopReviewAdapter.ViewHolder> {

    private List<ProductReview> items;

    public ShopReviewAdapter(List<ProductReview> items) {
        this.items = items;
    }

    public void updateData(List<ProductReview> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopItemReviewBinding binding = ShopItemReviewBinding.inflate(
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ShopItemReviewBinding binding;

        public ViewHolder(ShopItemReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProductReview item) {
            binding.tvReviewerName.setText(item.getReviewerName());
            
            String avatarChar = "U";
            if (item.getReviewerName() != null && !item.getReviewerName().isEmpty()) {
                avatarChar = item.getReviewerName().substring(0, 1).toUpperCase();
            }
            binding.tvAvatarChar.setText(avatarChar);
            binding.tvReviewComment.setText("\"" + item.getComment() + "\"");

            ImageView[] starImageViews = {
                binding.ivStar1,
                binding.ivStar2,
                binding.ivStar3,
                binding.ivStar4,
                binding.ivStar5
            };

            for (int i = 0; i < starImageViews.length; i++) {
                if (i < item.getRating()) {
                    starImageViews[i].setVisibility(View.VISIBLE);
                } else {
                    starImageViews[i].setVisibility(View.GONE);
                }
            }
        }
    }
}
