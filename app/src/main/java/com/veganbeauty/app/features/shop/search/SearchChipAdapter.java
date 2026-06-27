package com.veganbeauty.app.features.shop.search;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.databinding.ShopItemSearchChipBinding;

import java.util.ArrayList;
import java.util.List;

public class SearchChipAdapter extends RecyclerView.Adapter<SearchChipAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(String text);
    }

    private final OnItemClickListener onItemClick;
    private final List<String> items = new ArrayList<>();

    public SearchChipAdapter(OnItemClickListener onItemClick) {
        this.onItemClick = onItemClick;
    }

    public void submitList(List<String> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopItemSearchChipBinding binding = ShopItemSearchChipBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ShopItemSearchChipBinding binding;

        public ViewHolder(ShopItemSearchChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String text) {
            binding.tvChip.setText(text);
            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(text));
        }
    }
}
