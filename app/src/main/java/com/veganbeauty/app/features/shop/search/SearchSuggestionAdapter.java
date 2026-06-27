package com.veganbeauty.app.features.shop.search;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.databinding.ShopSearchBarBinding;

import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionAdapter extends RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(String text);
    }

    private final int iconRes;
    private final OnItemClickListener onItemClick;
    private final List<String> items = new ArrayList<>();

    public SearchSuggestionAdapter(int iconRes, OnItemClickListener onItemClick) {
        this.iconRes = iconRes;
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
        ShopSearchBarBinding binding = ShopSearchBarBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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
        private final ShopSearchBarBinding binding;

        public ViewHolder(ShopSearchBarBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String text) {
            binding.tvText.setText(text);
            binding.ivIcon.setImageResource(iconRes);
            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(text));
        }
    }
}
