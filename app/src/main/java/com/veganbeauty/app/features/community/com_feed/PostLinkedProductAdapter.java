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

import coil.Coil;
import coil.request.ImageRequest;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.CommunityProduct;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment;

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
        
        ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                .data(product.getMainImage())
                .target(holder.ivProductImage)
                .crossfade(true)
                .placeholder(android.R.color.darker_gray)
                .error(R.drawable.logo)
                .build();
        Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        
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
                
                ShopDetailFragment detailFragment = new ShopDetailFragment();
                detailFragment.setProduct(productEntity);
                
                ((FragmentActivity) context).getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }
}
