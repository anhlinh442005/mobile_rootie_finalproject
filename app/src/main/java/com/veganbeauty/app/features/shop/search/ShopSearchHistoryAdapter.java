package com.veganbeauty.app.features.shop.search;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.databinding.ShopItemSearchHistoryBinding;

import java.util.ArrayList;
import java.util.List;

public class ShopSearchHistoryAdapter extends RecyclerView.Adapter<ShopSearchHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(String query);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(String query);
    }

    private final OnItemClickListener onItemClick;
    private final OnDeleteClickListener onDeleteClick;
    private final List<String> items = new ArrayList<>();

    public ShopSearchHistoryAdapter(OnItemClickListener onItemClick, OnDeleteClickListener onDeleteClick) {
        this.onItemClick = onItemClick;
        this.onDeleteClick = onDeleteClick;
    }

    public void submitList(List<String> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopItemSearchHistoryBinding binding = ShopItemSearchHistoryBinding.inflate(
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ShopItemSearchHistoryBinding binding;

        public ViewHolder(ShopItemSearchHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String query) {
            binding.tvSearchTerm.setText(query);
            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(query));
            binding.btnDelete.setOnClickListener(v -> onDeleteClick.onDeleteClick(query));
        }
    }
}
