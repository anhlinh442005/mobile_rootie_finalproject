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
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.databinding.ShopItemCartBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class CartAdapter extends ListAdapter<CartItemEntity, CartAdapter.CartViewHolder> {

    public interface OnQuantityChangedListener {
        void onQuantityChanged(CartItemEntity item, int newQuantity);
    }

    public interface OnSelectionToggledListener {
        void onSelectionToggled(CartItemEntity item, boolean isSelected);
    }

    private final OnQuantityChangedListener onQuantityChanged;
    private final OnSelectionToggledListener onSelectionToggled;

    public CartAdapter(OnQuantityChangedListener onQuantityChanged, OnSelectionToggledListener onSelectionToggled) {
        super(new CartDiffCallback());
        this.onQuantityChanged = onQuantityChanged;
        this.onSelectionToggled = onSelectionToggled;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopItemCartBinding binding = ShopItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public class CartViewHolder extends RecyclerView.ViewHolder {
        private final ShopItemCartBinding binding;

        public CartViewHolder(@NonNull ShopItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CartItemEntity item) {
            binding.tvProductName.setText(item.getName());
            binding.tvQuantityValue.setText(String.valueOf(item.getQuantity()));

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvPrice.setText(formatter.format(item.getPrice()));

            long originalPrice = (long) (item.getPrice() * 1.2);
            binding.tvOriginalPrice.setText(formatter.format(originalPrice));
            binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            ImageRequest request = new ImageRequest.Builder(binding.getRoot().getContext())
                    .data(item.getImage())
                    .target(binding.ivProduct)
                    .crossfade(true)
                    .placeholder(android.R.color.darker_gray)
                    .build();
            Coil.imageLoader(binding.getRoot().getContext()).enqueue(request);

            if (item.isSelected()) {
                binding.ivSelect.setImageResource(R.drawable.ic_cart_checked);
            } else {
                binding.ivSelect.setImageResource(R.drawable.ic_cart_unchecked);
            }

            binding.ivSelect.setOnClickListener(v -> onSelectionToggled.onSelectionToggled(item, !item.isSelected()));

            binding.btnPlus.setOnClickListener(v -> onQuantityChanged.onQuantityChanged(item, item.getQuantity() + 1));

            binding.btnMinus.setOnClickListener(v -> onQuantityChanged.onQuantityChanged(item, item.getQuantity() - 1));
        }
    }

    public static class CartDiffCallback extends DiffUtil.ItemCallback<CartItemEntity> {
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
