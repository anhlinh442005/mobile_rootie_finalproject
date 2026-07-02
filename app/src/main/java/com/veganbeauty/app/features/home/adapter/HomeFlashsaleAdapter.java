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
import com.veganbeauty.app.databinding.ItemHomeFlashsaleProductBinding;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class HomeFlashsaleAdapter extends ListAdapter<ProductEntity, HomeFlashsaleAdapter.ViewHolder> {

    private final HomeProductCartListener listener;
    private final NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public HomeFlashsaleAdapter(HomeProductCartListener listener) {
        super(new DiffCallback());
        this.listener = listener != null ? listener : new HomeProductCartListener() {
            @Override public void onProductClick(ProductEntity product) {}
            @Override public void onQuickAddToCart(ProductEntity product, View cartButton, android.widget.ImageView productImage) {}
            @Override public void onCartLongPress(ProductEntity product) {}
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHomeFlashsaleProductBinding binding = ItemHomeFlashsaleProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHomeFlashsaleProductBinding binding;

        public ViewHolder(ItemHomeFlashsaleProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProductEntity product) {
            View.OnClickListener openDetail = v -> listener.onProductClick(product);
            binding.ivFlashProduct.setOnClickListener(openDetail);
            binding.tvFlashProductName.setOnClickListener(openDetail);
            binding.tvFlashPrice.setOnClickListener(openDetail);
            binding.tvFlashOriginalPrice.setOnClickListener(openDetail);

            binding.btnFlashBuy.setOnClickListener(v ->
                    listener.onQuickAddToCart(product, binding.btnFlashBuy, binding.ivFlashProduct));
            binding.btnFlashBuy.setOnLongClickListener(v -> {
                listener.onCartLongPress(product);
                return true;
            });

            binding.tvFlashProductName.setText(product.getName());

            Random random = new Random((long) product.getId().hashCode());
            int discount = random.nextInt(31) + 20;
            binding.tvFlashDiscountBadge.setText("-" + discount + "%");

            long originalPrice = product.getPrice();
            double salePrice = originalPrice * (100 - discount) / 100.0;

            binding.tvFlashPrice.setText(priceFormatter.format(salePrice));
            binding.tvFlashOriginalPrice.setText(priceFormatter.format(originalPrice));
            binding.tvFlashOriginalPrice.setPaintFlags(binding.tvFlashOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            com.veganbeauty.app.utils.ProductImageHelper.loadProductImage(binding.ivFlashProduct, product);
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
