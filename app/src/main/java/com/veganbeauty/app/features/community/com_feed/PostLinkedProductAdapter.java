package com.veganbeauty.app.features.community.com_feed;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.CommunityProduct;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PostLinkedProductAdapter extends RecyclerView.Adapter<PostLinkedProductAdapter.ProductViewHolder> {

    private final List<CommunityProduct> products;
    private final String postId;
    private final String authorId;

    public PostLinkedProductAdapter(List<CommunityProduct> products, String postId, String authorId) {
        this.products = products != null ? products : new ArrayList<>();
        this.postId = postId != null ? postId : "";
        this.authorId = authorId != null ? authorId : "";
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivProductImage;
        public final TextView tvProductName;

        public ProductViewHolder(View view) {
            super(view);
            ivProductImage = view.findViewById(R.id.ivProductImage);
            tvProductName = view.findViewById(R.id.tvProductName);
        }
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.com_item_post_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        CommunityProduct product = products.get(position);
        holder.tvProductName.setText(product.getName());
        
        com.bumptech.glide.Glide.with(holder.ivProductImage.getContext()).load(product.getMainImage()).placeholder(android.R.color.darker_gray).into(holder.ivProductImage);
        
        holder.itemView.setOnClickListener(v -> {
            Context context = holder.itemView.getContext();
            
            try {
                String affiliateCode = "AFF_" + authorId.toUpperCase() + "_" + product.getId().toUpperCase();
                JSONObject trackingJson = new JSONObject();
                trackingJson.put("referrerUserId", authorId);
                trackingJson.put("sourcePostId", postId);
                trackingJson.put("affiliateCode", affiliateCode);
                trackingJson.put("timestamp", System.currentTimeMillis());
                
                SharedPreferences prefs = context.getSharedPreferences("affiliate_prefs", Context.MODE_PRIVATE);
                prefs.edit().putString(product.getId(), trackingJson.toString()).apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if (context instanceof FragmentActivity) {
                ProductEntity productEntity = new ProductEntity();
                productEntity.setId(product.getId());
                productEntity.setName(product.getName());
                productEntity.setSku(product.getId());
                productEntity.setPrice((long) product.getPrice());
                productEntity.setCategory("Affiliate");
                productEntity.setStock(100);
                productEntity.setMainImage(product.getMainImage());
                
                ProductDetailLauncher.open((androidx.fragment.app.FragmentActivity) context, product.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }
}
