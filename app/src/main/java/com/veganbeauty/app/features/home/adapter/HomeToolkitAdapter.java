package com.veganbeauty.app.features.home.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.databinding.HomeItemToolkitBinding;

import java.util.ArrayList;
import java.util.List;

public class HomeToolkitAdapter extends RecyclerView.Adapter<HomeToolkitAdapter.ViewHolder> {

    private final List<HomeShortcutItem> items = new ArrayList<>();

    public void submitList(List<HomeShortcutItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HomeItemToolkitBinding binding = HomeItemToolkitBinding.inflate(
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final HomeItemToolkitBinding binding;

        ViewHolder(HomeItemToolkitBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(HomeShortcutItem item) {
            binding.tvToolkitName.setText(item.getTitle());
            binding.ivToolkitIcon.setImageResource(item.getIconResId());
            binding.getRoot().setOnClickListener(v -> item.getAction().run());
        }
    }
}
