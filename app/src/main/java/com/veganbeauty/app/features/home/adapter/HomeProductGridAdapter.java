package com.veganbeauty.app.features.home.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.HomeItemProductGridBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeProductGridAdapter extends ListAdapter<ProductEntity, HomeProductGridAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ProductEntity product);
    }

    public interface OnAddToCartClickListener {
        void onAddToCart(ProductEntity product);
    }

    private final OnItemClickListener onItemClick;
    private final OnAddToCartClickListener onAddToCart;
    private final NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public HomeProductGridAdapter(OnItemClickListener onItemClick, OnAddToCartClickListener onAddToCart) {
        super(new DiffCallback());
        this.onItemClick = onItemClick != null ? onItemClick : p -> {};
        this.onAddToCart = onAddToCart != null ? onAddToCart : p -> {};
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HomeItemProductGridBinding binding = HomeItemProductGridBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final HomeItemProductGridBinding binding;

        public ViewHolder(HomeItemProductGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProductEntity product) {
            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(product));
            binding.btnAddToCart.setOnClickListener(v -> onAddToCart.onAddToCart(product));
            binding.tvProductName.setText(product.getName());
            binding.tvPrice.setText(priceFormatter.format(product.getPrice()));

            long originalPrice = (long) (product.getPrice() / 0.65);
            binding.tvOriginalPrice.setText(priceFormatter.format(originalPrice));
            binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            com.veganbeauty.app.utils.ProductImageHelper.loadProductImage(binding.ivProduct, product);

            binding.tvSaleBadge.setText("-35%");
            binding.tvSaleBadge.setVisibility(View.VISIBLE);
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
