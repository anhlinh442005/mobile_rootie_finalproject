package com.veganbeauty.app.features.shop.home;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.veganbeauty.app.R;
import com.veganbeauty.app.databinding.ShopHomeCategoryBinding;
import com.veganbeauty.app.features.shop.home.models.CategoryUiModel;

public class ShopHomeCategoryAdapter extends ListAdapter<CategoryUiModel, ShopHomeCategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryUiModel category);
    }

    private final OnCategoryClickListener onCategoryClick;

    public ShopHomeCategoryAdapter(OnCategoryClickListener onCategoryClick) {
        super(new DiffCallback());
        this.onCategoryClick = onCategoryClick;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopHomeCategoryBinding binding = ShopHomeCategoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final ShopHomeCategoryBinding binding;

        public CategoryViewHolder(ShopHomeCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @SuppressLint("ClickableViewAccessibility")
        public void bind(CategoryUiModel category) {
            binding.tvCategoryName.setText(category.getName());
            binding.tvProductCount.setText(category.getProductCount() + " sản phẩm");
            binding.ivCategoryIcon.setImageResource(category.getIconRes());

            binding.getRoot().setOnClickListener(v -> {
                if (onCategoryClick != null) onCategoryClick.onCategoryClick(category);
            });

            float density = binding.getRoot().getContext().getResources().getDisplayMetrics().density;
            binding.getRoot().setOnTouchListener((view, event) -> {
                MaterialCardView card = (MaterialCardView) view;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        card.animate()
                                .scaleX(1.05f)
                                .scaleY(1.05f)
                                .translationZ(6f * density)
                                .setDuration(120)
                                .start();
                        card.setStrokeColor(ContextCompat.getColor(card.getContext(), R.color.primary));
                        card.setStrokeWidth((int) (2.5f * density));
                        break;
                    case MotionEvent.ACTION_UP:
                        card.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .translationZ(0f)
                                .setDuration(150)
                                .start();
                        card.setStrokeColor(Color.parseColor("#EAEAEA"));
                        card.setStrokeWidth((int) (1f * density));
                        card.performClick();
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        card.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .translationZ(0f)
                                .setDuration(150)
                                .start();
                        card.setStrokeColor(Color.parseColor("#EAEAEA"));
                        card.setStrokeWidth((int) (1f * density));
                        break;
                }
                return true;
            });
        }
    }

    public static class DiffCallback extends DiffUtil.ItemCallback<CategoryUiModel> {
        @Override
        public boolean areItemsTheSame(@NonNull CategoryUiModel oldItem, @NonNull CategoryUiModel newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull CategoryUiModel oldItem, @NonNull CategoryUiModel newItem) {
            return oldItem.equals(newItem);
        }
    }
}
