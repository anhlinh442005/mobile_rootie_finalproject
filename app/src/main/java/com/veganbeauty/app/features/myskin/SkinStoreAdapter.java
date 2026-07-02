package com.veganbeauty.app.features.myskin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.StoreEntity;

import java.util.List;

public class SkinStoreAdapter extends RecyclerView.Adapter<SkinStoreAdapter.ViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(StoreEntity store);
    }

    private final List<StoreEntity> stores;
    private final OnBookClickListener onBookClick;

    public SkinStoreAdapter(List<StoreEntity> stores, OnBookClickListener onBookClick) {
        this.stores = stores;
        this.onBookClick = onBookClick;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView storeImage;
        public final TextView storeName;
        public final TextView storeAddress;
        public final TextView storeHours;
        public final TextView btnBook;

        public ViewHolder(View view) {
            super(view);
            storeImage = view.findViewById(R.id.skin_store_image);
            storeName = view.findViewById(R.id.skin_store_name);
            storeAddress = view.findViewById(R.id.skin_store_address);
            storeHours = view.findViewById(R.id.skin_store_hours);
            btnBook = view.findViewById(R.id.skin_store_btn_book);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.skin_item_store_home, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StoreEntity store = stores.get(position);
        holder.storeName.setText(store.getTenCuaHang() != null ? store.getTenCuaHang() : "");
        holder.storeAddress.setText(store.getDiaChiDayDu() != null ? store.getDiaChiDayDu() : "");
        String open = store.getMoCua() != null ? store.getMoCua() : "";
        String close = store.getDongCua() != null ? store.getDongCua() : "";
        holder.storeHours.setText(open + " - " + close);
        
        String imageUrl = store.getImageUrl() != null ? store.getImageUrl() : "";
        if (!imageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(holder.storeImage.getContext()).load(imageUrl).placeholder(R.drawable.skin_store).error(R.drawable.skin_store).into(holder.storeImage);
        } else {
            holder.storeImage.setImageResource(R.drawable.skin_store);
        }

        holder.btnBook.setOnClickListener(v -> onBookClick.onBookClick(store));
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }
}
