package com.veganbeauty.app.features.home.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.databinding.HomeItemCategoryBinding;

public class HomeCategoryAdapter extends ListAdapter<HomeCategoryItem, HomeCategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(HomeCategoryItem category);
    }

    private final OnCategoryClickListener listener;

    public HomeCategoryAdapter(OnCategoryClickListener listener) {
        super(new DiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HomeItemCategoryBinding binding = HomeItemCategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final HomeItemCategoryBinding binding;

        public ViewHolder(@NonNull HomeItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(HomeCategoryItem item) {
            binding.tvCategoryName.setText(item.getName());
            binding.ivCategory.setImageResource(item.getIconResId());
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onCategoryClick(item);
            });
        }
    }

    private static class DiffCallback extends DiffUtil.ItemCallback<HomeCategoryItem> {
        @Override
        public boolean areItemsTheSame(@NonNull HomeCategoryItem oldItem, @NonNull HomeCategoryItem newItem) {
            return oldItem.getName().equals(newItem.getName());
        }

        @Override
        public boolean areContentsTheSame(@NonNull HomeCategoryItem oldItem, @NonNull HomeCategoryItem newItem) {
            return oldItem.equals(newItem);
        }
    }
}
