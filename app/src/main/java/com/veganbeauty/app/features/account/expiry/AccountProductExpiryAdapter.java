package com.veganbeauty.app.features.account.expiry;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;

public class AccountProductExpiryAdapter extends ListAdapter<ExpiryProductUiModel, AccountProductExpiryAdapter.ViewHolder> {

    public enum ExpiryLayoutMode {
        GRID,
        HORIZONTAL,
        LIST
    }

    public interface OnItemClickListener {
        void onItemClick(ExpiryProductUiModel uiModel);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(ExpiryProductUiModel uiModel);
    }

    private final ExpiryLayoutMode layoutMode;
    private final OnItemClickListener onItemClick;
    private final OnItemLongClickListener onItemLongClick;

    public AccountProductExpiryAdapter(ExpiryLayoutMode layoutMode, OnItemClickListener onItemClick, OnItemLongClickListener onItemLongClick) {
        super(new DiffCallback());
        this.layoutMode = layoutMode;
        this.onItemClick = onItemClick;
        this.onItemLongClick = onItemLongClick;
    }

    public AccountProductExpiryAdapter(ExpiryLayoutMode layoutMode, OnItemClickListener onItemClick) {
        this(layoutMode, onItemClick, null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        switch (layoutMode) {
            case GRID:
                layoutId = R.layout.account_product_expiry_item_grid;
                break;
            case HORIZONTAL:
                layoutId = R.layout.account_product_expiry_item_horizontal;
                break;
            case LIST:
            default:
                layoutId = R.layout.account_product_expiry_item_list;
                break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view, onItemClick, onItemLongClick);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProductImage;
        private final TextView tvProductName;
        private final TextView tvExpiryDuration;
        private final ProgressBar pbExpiry;
        private final OnItemClickListener onItemClick;
        private final OnItemLongClickListener onItemLongClick;

        public ViewHolder(@NonNull View itemView, OnItemClickListener onItemClick, OnItemLongClickListener onItemLongClick) {
            super(itemView);
            this.onItemClick = onItemClick;
            this.onItemLongClick = onItemLongClick;
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvExpiryDuration = itemView.findViewById(R.id.tvExpiryDuration);
            pbExpiry = itemView.findViewById(R.id.pbExpiry);
        }

        public void bind(ExpiryProductUiModel uiModel) {
            tvProductName.setText(uiModel.getProduct().getName());
            tvExpiryDuration.setText(uiModel.getDurationText());

            com.bumptech.glide.Glide.with(ivProductImage.getContext()).load(uiModel.getProduct().getMainImage()).placeholder(android.R.color.darker_gray).into(ivProductImage);

            pbExpiry.setProgress(uiModel.getProgressPercent());

            if (uiModel.getRemainingDays() <= 0) {
                tvExpiryDuration.setTextColor(Color.parseColor("#8E8E8E")); // Grey
                pbExpiry.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#8E8E8E")));
            } else if (uiModel.isUrgent()) {
                tvExpiryDuration.setTextColor(Color.parseColor("#C62828")); // Red
                pbExpiry.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#C62828")));
            } else {
                tvExpiryDuration.setTextColor(Color.parseColor("#677559")); // Green
                pbExpiry.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#677559")));
            }

            itemView.setOnClickListener(v -> {
                if (onItemClick != null) onItemClick.onItemClick(uiModel);
            });
            
            itemView.setOnLongClickListener(v -> {
                if (onItemLongClick != null) {
                    onItemLongClick.onItemLongClick(uiModel);
                    return true;
                }
                return false;
            });
        }
    }

    public static class DiffCallback extends DiffUtil.ItemCallback<ExpiryProductUiModel> {
        @Override
        public boolean areItemsTheSame(@NonNull ExpiryProductUiModel oldItem, @NonNull ExpiryProductUiModel newItem) {
            return oldItem.getProduct().getId().equals(newItem.getProduct().getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ExpiryProductUiModel oldItem, @NonNull ExpiryProductUiModel newItem) {
            return oldItem.equals(newItem);
        }
    }
}
