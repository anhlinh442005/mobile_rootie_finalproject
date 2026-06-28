package com.veganbeauty.app.features.home.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.HomeItemTopSearchBinding;

public class HomeTopSearchAdapter extends ListAdapter<ProductEntity, HomeTopSearchAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ProductEntity product);
    }

    private final OnItemClickListener onItemClick;

    public HomeTopSearchAdapter() {
        this(product -> {});
    }

    public HomeTopSearchAdapter(OnItemClickListener onItemClick) {
        super(new DiffCallback());
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HomeItemTopSearchBinding binding = HomeItemTopSearchBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), position + 1);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final HomeItemTopSearchBinding binding;

        public ViewHolder(HomeItemTopSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProductEntity product, int rank) {
            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(product));
            binding.tvRank.setText(String.valueOf(rank));
            binding.tvProductName.setText(product.getName());
            
            com.veganbeauty.app.utils.ProductImageHelper.loadProductImage(binding.ivProduct, product);
        }
    }

    private static class DiffCallback extends DiffUtil.ItemCallback<ProductEntity> {
        @Override
        public boolean areItemsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.equals(newItem);
        }
    }
}
