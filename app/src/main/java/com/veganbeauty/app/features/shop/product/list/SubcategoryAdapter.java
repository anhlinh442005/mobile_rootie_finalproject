package com.veganbeauty.app.features.shop.product.list;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;

public class SubcategoryAdapter extends ListAdapter<String, SubcategoryAdapter.SubcategoryViewHolder> {

    public interface OnSubcategoryClickListener {
        void onSubcategoryClick(String subcategory);
    }

    private String selectedSubcategory = "Tất cả";
    private final OnSubcategoryClickListener onSubcategoryClick;

    public SubcategoryAdapter(OnSubcategoryClickListener onSubcategoryClick) {
        super(new SubcategoryDiffCallback());
        this.onSubcategoryClick = onSubcategoryClick;
    }

    public void setSelectedSubcategory(String selectedSubcategory) {
        this.selectedSubcategory = selectedSubcategory;
        notifyDataSetChanged();
    }

    public String getSelectedSubcategory() {
        return selectedSubcategory;
    }

    @NonNull
    @Override
    public SubcategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shop_category_subcategory, parent, false);
        return new SubcategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubcategoryViewHolder holder, int position) {
        String subcategory = getItem(position);
        holder.bind(subcategory, subcategory.equals(selectedSubcategory), onSubcategoryClick);
    }

    public static class SubcategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvChip;

        public SubcategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChip = itemView.findViewById(R.id.tvChip);
        }

        public void bind(String subcategory, boolean isSelected, OnSubcategoryClickListener listener) {
            tvChip.setText(subcategory);
            
            if (isSelected) {
                tvChip.setBackgroundResource(R.drawable.bg_chip_selected);
                tvChip.setTextColor(Color.parseColor("#3D5A40"));
            } else {
                tvChip.setBackgroundResource(R.drawable.bg_chip_normal);
                tvChip.setTextColor(Color.parseColor("#555555"));
            }

            itemView.setOnClickListener(v -> listener.onSubcategoryClick(subcategory));
        }
    }

    public static class SubcategoryDiffCallback extends DiffUtil.ItemCallback<String> {
        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
    }
}
