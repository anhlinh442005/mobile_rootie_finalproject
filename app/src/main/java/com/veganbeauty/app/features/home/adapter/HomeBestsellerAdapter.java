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
import com.veganbeauty.app.databinding.HomeItemBestsellerBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeBestsellerAdapter extends ListAdapter<ProductEntity, HomeBestsellerAdapter.ViewHolder> {

    private final HomeProductCartListener listener;
    private final NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public HomeBestsellerAdapter(HomeProductCartListener listener) {
        super(new DiffCallback());
        this.listener = listener != null ? listener : new HomeProductCartListener() {
            @Override public void onProductClick(ProductEntity product) {}
            @Override public void onQuickAddToCart(ProductEntity product, View cartButton, android.widget.ImageView productImage) {}
            @Override public void onCartLongPress(ProductEntity product) {}
            @Override public void onBuyNow(ProductEntity product) {}
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HomeItemBestsellerBinding binding = HomeItemBestsellerBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), position + 1);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final HomeItemBestsellerBinding binding;

        public ViewHolder(HomeItemBestsellerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProductEntity product, int rank) {
            View.OnClickListener openDetail = v -> listener.onProductClick(product);
            binding.ivProduct.setOnClickListener(openDetail);
            binding.tvProductName.setOnClickListener(openDetail);
            binding.tvPrice.setOnClickListener(openDetail);
            binding.tvOriginalPrice.setOnClickListener(openDetail);
            binding.tvRank.setOnClickListener(openDetail);

            binding.btnAction.setOnClickListener(v ->
                    listener.onQuickAddToCart(product, binding.btnAction, binding.ivProduct));
            binding.btnAction.setOnLongClickListener(v -> {
                listener.onCartLongPress(product);
                return true;
            });

            binding.tvRank.setText(String.valueOf(rank));
            binding.tvProductName.setText(product.getName());
            binding.tvPrice.setText(priceFormatter.format(product.getPrice()));

            double originalPrice = product.getPrice() / 0.75;
            binding.tvOriginalPrice.setText(priceFormatter.format(originalPrice));
            binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            com.veganbeauty.app.utils.ProductImageHelper.loadProductImage(binding.ivProduct, product);
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
