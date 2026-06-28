package com.veganbeauty.app.features.shop.search;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.ShopProductHorizontalBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HotDealsAdapter extends RecyclerView.Adapter<HotDealsAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ProductEntity product);
    }

    public interface OnAddToCartClickListener {
        void onAddToCartClick(ProductEntity product);
    }

    private final OnItemClickListener onItemClick;
    private final OnAddToCartClickListener onAddToCartClick;
    private final List<ProductEntity> items = new ArrayList<>();

    public HotDealsAdapter(OnItemClickListener onItemClick, OnAddToCartClickListener onAddToCartClick) {
        this.onItemClick = onItemClick;
        this.onAddToCartClick = onAddToCartClick != null ? onAddToCartClick : p -> {};
    }

    public void submitList(List<ProductEntity> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ShopProductHorizontalBinding binding = ShopProductHorizontalBinding.inflate(
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
        private final ShopProductHorizontalBinding binding;

        public ViewHolder(ShopProductHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProductEntity product) {
            binding.tvProductName.setText(product.getName());

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            binding.tvPrice.setText(formatter.format(product.getPrice()));

            long originalPrice = (long) (product.getPrice() * 1.35);
            binding.tvOriginalPrice.setText(formatter.format(originalPrice));
            binding.tvOriginalPrice.setPaintFlags(binding.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

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

            com.veganbeauty.app.utils.ProductImageHelper.loadProductImage(binding.ivProduct, product);

            binding.getRoot().setOnClickListener(v -> onItemClick.onItemClick(product));
        }
    }
}
