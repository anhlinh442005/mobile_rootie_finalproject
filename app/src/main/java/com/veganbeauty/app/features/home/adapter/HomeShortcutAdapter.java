package com.veganbeauty.app.features.home.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.databinding.HomeItemShortcutBinding;

import java.util.ArrayList;
import java.util.List;

public class HomeShortcutAdapter extends RecyclerView.Adapter<HomeShortcutAdapter.ViewHolder> {

    private final List<HomeShortcutItem> items = new ArrayList<>();

    public void submitList(List<HomeShortcutItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HomeItemShortcutBinding binding = HomeItemShortcutBinding.inflate(
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
        private final HomeItemShortcutBinding binding;

        public ViewHolder(HomeItemShortcutBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(HomeShortcutItem item) {
            binding.tvShortcutName.setText(item.getTitle());
            binding.ivShortcutIcon.setImageResource(item.getIconResId());
            binding.getRoot().setOnClickListener(v -> item.getAction().run());
        }
    }
}
