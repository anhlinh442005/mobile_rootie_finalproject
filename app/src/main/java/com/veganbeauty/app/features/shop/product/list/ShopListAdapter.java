package com.veganbeauty.app.features.shop.product.list;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShopListAdapter extends RecyclerView.Adapter<ShopListAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ProductEntity product);
    }

    public interface OnAddToCartClickListener {
        void onAddToCartClick(ProductEntity product);
    }

    private final OnItemClickListener onItemClick;
    private final OnAddToCartClickListener onAddToCartClick;
    private final boolean isInfinite;
    private final List<ProductEntity> originalItems = new ArrayList<>();
    private final List<ProductEntity> items = new ArrayList<>();

    public ShopListAdapter(OnItemClickListener onItemClick, OnAddToCartClickListener onAddToCartClick, boolean isInfinite) {
        this.onItemClick = onItemClick;
        this.onAddToCartClick = onAddToCartClick;
        this.isInfinite = isInfinite;
    }

    public ShopListAdapter(OnItemClickListener onItemClick, OnAddToCartClickListener onAddToCartClick) {
        this(onItemClick, onAddToCartClick, false);
    }

    public void submitList(List<ProductEntity> newItems) {
        originalItems.clear();
        originalItems.addAll(newItems);
        items.clear();
        items.addAll(newItems);
        if (isInfinite && !newItems.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                items.addAll(newItems);
            }
        }
        notifyDataSetChanged();
    }

    public void duplicateItems() {
        if (isInfinite && !originalItems.isEmpty()) {
            int startPosition = items.size();
            items.addAll(originalItems);
            notifyItemRangeInserted(startPosition, originalItems.size());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopProductCardBinding binding = ShopProductCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ShopProductCardBinding binding;

        public ViewHolder(ShopProductCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProductEntity product) {
            binding.tvProductName.setText(product.getName());

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvPrice.setText(formatter.format(product.getPrice()));

            long originalPrice = (long) (product.getPrice() * 1.2);
            binding.tvOriginalPrice.setText(formatter.format(originalPrice));
            binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            com.veganbeauty.app.utils.ProductImageHelper.loadProductImage(binding.ivProduct, product);

            if (product.isNew()) {
                binding.tvBadgeNew.setVisibility(View.VISIBLE);
                binding.tvBadgeNew.setText("Mới");
                binding.tvBadgeNew.setBackgroundResource(R.drawable.bg_badge_new);
            } else if (product.getPrice() >= 500000 || (product.getCategory() != null && product.getCategory().toLowerCase().contains("combo"))) {
                binding.tvBadgeNew.setVisibility(View.VISIBLE);
                binding.tvBadgeNew.setText("Hot");
                binding.tvBadgeNew.setBackgroundResource(R.drawable.bg_badge_hot);
            } else {
                binding.tvBadgeNew.setVisibility(View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(product));
            binding.btnAddToCart.setOnClickListener(v -> onAddToCartClick.onAddToCartClick(product));
        }
    }
}
