package com.veganbeauty.app.features.home.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;

import java.util.ArrayList;
import java.util.List;

public class HomeVoucherCategoryAdapter extends RecyclerView.Adapter<HomeVoucherCategoryAdapter.ViewHolder> {

    public interface OnCategorySelectedListener {
        void onCategorySelected(String category);
    }

    private final List<String> categories = new ArrayList<>();
    private int selectedIndex = 0;
    private final OnCategorySelectedListener listener;

    public HomeVoucherCategoryAdapter(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    public void submitCategories(List<String> items) {
        categories.clear();
        categories.addAll(items);
        selectedIndex = 0;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView chip = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.home_item_voucher_chip, parent, false);
        return new ViewHolder(chip);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(categories.get(position), position == selectedIndex);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView chip;

        ViewHolder(TextView chip) {
            super(chip);
            this.chip = chip;
        }

        void bind(String category, boolean selected) {
            chip.setText(category);
            if (selected) {
                chip.setBackgroundResource(R.drawable.bg_home_voucher_chip_selected);
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setBackgroundResource(R.drawable.bg_home_voucher_chip_normal);
                chip.setTextColor(ContextCompat.getColor(chip.getContext(), R.color.primary));
            }
            chip.setOnClickListener(v -> {
                int previous = selectedIndex;
                selectedIndex = getBindingAdapterPosition();
                if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous);
                notifyItemChanged(selectedIndex);
                listener.onCategorySelected(category);
            });
        }
    }
}
