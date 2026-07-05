package com.veganbeauty.app.features.shop.product.detail;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.ShopProductCardBinding;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ShopHorizontalProductAdapter extends RecyclerView.Adapter<ShopHorizontalProductAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ProductEntity product);
    }

    public interface OnAddToCartClickListener {
        void onAddToCartClick(ProductEntity product);
    }

    private List<ProductEntity> items;
    private final OnItemClickListener onItemClick;
    private final OnAddToCartClickListener onAddToCartClick;

    public ShopHorizontalProductAdapter(List<ProductEntity> items, OnItemClickListener onItemClick, OnAddToCartClickListener onAddToCartClick) {
        this.items = items != null ? items : new java.util.ArrayList<>();
        this.onItemClick = onItemClick;
        this.onAddToCartClick = onAddToCartClick;
    }

    public void updateData(List<ProductEntity> newItems) {
        this.items = newItems != null ? newItems : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopProductCardBinding binding = ShopProductCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        
        float density = parent.getResources().getDisplayMetrics().density;
        int widthPx = (int) (160 * density);
        ViewGroup.LayoutParams layoutParams = binding.getRoot().getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = widthPx;
            binding.getRoot().setLayoutParams(layoutParams);
        } else {
            binding.getRoot().setLayoutParams(new ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), onItemClick, onAddToCartClick);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ShopProductCardBinding binding;

        public ViewHolder(ShopProductCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProductEntity product, OnItemClickListener onItemClick, OnAddToCartClickListener onAddToCartClick) {
            binding.tvProductName.setText(product.getName());

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvPrice.setText(formatter.format(product.getPrice()));

            long originalPrice = (long) (product.getPrice() * 1.2);
            binding.tvOriginalPrice.setText(formatter.format(originalPrice));
            binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            if (product.isNew()) {
                binding.tvBadgeNew.setVisibility(View.VISIBLE);
            } else {
                binding.tvBadgeNew.setVisibility(View.GONE);
            }

            com.veganbeauty.app.utils.ProductImageHelper.loadProductImage(binding.ivProduct, product);

            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(product));
            binding.btnAddToCart.setOnClickListener(v -> onAddToCartClick.onAddToCartClick(product));

            if (product.getStock() <= 0) {
                binding.btnAddToCart.setText("Hết hàng");
                binding.btnAddToCart.setEnabled(false);
                binding.btnAddToCart.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        binding.getRoot().getContext().getResources().getColor(R.color.gray_dark)));
                binding.layoutStockWarning.setVisibility(View.GONE);
            } else {
                binding.btnAddToCart.setText("Chọn mua");
                binding.btnAddToCart.setEnabled(true);
                binding.btnAddToCart.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        binding.getRoot().getContext().getResources().getColor(R.color.primary)));
                if (product.getStock() < 20) {
                    binding.layoutStockWarning.setVisibility(View.VISIBLE);
                    binding.tvStockWarning.setText("Chỉ còn " + product.getStock());
                } else {
                    binding.layoutStockWarning.setVisibility(View.GONE);
                }
            }
        }
    }
}
