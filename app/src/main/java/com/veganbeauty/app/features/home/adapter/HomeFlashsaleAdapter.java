package com.veganbeauty.app.features.home.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.ItemHomeFlashsaleProductBinding;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class HomeFlashsaleAdapter extends ListAdapter<ProductEntity, HomeFlashsaleAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ProductEntity product);
    }

    public interface OnAddToCartClickListener {
        void onAddToCart(ProductEntity product);
    }

    private final OnItemClickListener onItemClick;
    private final OnAddToCartClickListener onAddToCart;
    private final NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public HomeFlashsaleAdapter(OnItemClickListener onItemClick, OnAddToCartClickListener onAddToCart) {
        super(new DiffCallback());
        this.onItemClick = onItemClick != null ? onItemClick : p -> {};
        this.onAddToCart = onAddToCart != null ? onAddToCart : p -> {};
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
            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(product));
            binding.btnFlashBuy.setOnClickListener(v -> onAddToCart.onAddToCart(product));
            binding.tvFlashProductName.setText(product.getName());

            Random random = new Random((long) product.getId().hashCode());
            int discount = random.nextInt(31) + 20; // 20% to 50%
            binding.tvFlashDiscountBadge.setText("-" + discount + "%");

            long originalPrice = product.getPrice();
            double salePrice = originalPrice * (100 - discount) / 100.0;

            binding.tvFlashPrice.setText(priceFormatter.format(salePrice));

            binding.tvFlashOriginalPrice.setText(priceFormatter.format(originalPrice));
            binding.tvFlashOriginalPrice.setPaintFlags(binding.tvFlashOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            ImageRequest request = new ImageRequest.Builder(binding.getRoot().getContext())
                    .data(product.getMainImage())
                    .target(binding.ivFlashProduct)
                    .crossfade(true)
                    .placeholder(android.R.color.darker_gray)
                    .build();
            Coil.imageLoader(binding.getRoot().getContext()).enqueue(request);
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
