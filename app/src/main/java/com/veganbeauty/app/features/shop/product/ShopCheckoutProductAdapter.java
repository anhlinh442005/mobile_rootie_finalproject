package com.veganbeauty.app.features.shop.product;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.databinding.ShopItemCheckoutProductBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class ShopCheckoutProductAdapter extends ListAdapter<CartItemEntity, ShopCheckoutProductAdapter.CheckoutProductViewHolder> {

    public ShopCheckoutProductAdapter() {
        super(new CheckoutProductDiffCallback());
    }

    @NonNull
    @Override
    public CheckoutProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopItemCheckoutProductBinding binding = ShopItemCheckoutProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CheckoutProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckoutProductViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public static class CheckoutProductViewHolder extends RecyclerView.ViewHolder {
        private final ShopItemCheckoutProductBinding binding;

        public CheckoutProductViewHolder(@NonNull ShopItemCheckoutProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CartItemEntity item) {
            binding.tvProductName.setText(item.getName());
            binding.tvQuantity.setText("x" + item.getQuantity());

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvPrice.setText(formatter.format(item.getPrice()));

            long originalPrice = (long) (item.getPrice() * 1.2);
            binding.tvOriginalPrice.setText(formatter.format(originalPrice));
            binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            if (item.getImage() != null && !item.getImage().isEmpty()) {
                ImageRequest request = new ImageRequest.Builder(binding.getRoot().getContext())
                        .data(item.getImage())
                        .target(binding.ivProduct)
                        .crossfade(true)
                        .placeholder(android.R.color.darker_gray)
                        .build();
                Coil.imageLoader(binding.getRoot().getContext()).enqueue(request);
            }
        }
    }

    public static class CheckoutProductDiffCallback extends DiffUtil.ItemCallback<CartItemEntity> {
        @Override
        public boolean areItemsTheSame(@NonNull CartItemEntity oldItem, @NonNull CartItemEntity newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull CartItemEntity oldItem, @NonNull CartItemEntity newItem) {
            return oldItem.equals(newItem);
        }
    }
}
