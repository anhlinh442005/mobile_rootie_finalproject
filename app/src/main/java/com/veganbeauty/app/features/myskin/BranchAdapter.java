package com.veganbeauty.app.features.myskin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.StoreEntity;

import java.util.List;

public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.ViewHolder> {

    public interface OnBranchSelectedListener {
        void onBranchSelected(StoreEntity store);
    }

    private List<StoreEntity> stores;
    private final OnBranchSelectedListener onBranchSelected;
    private String selectedStoreId = null;

    public BranchAdapter(List<StoreEntity> stores, OnBranchSelectedListener onBranchSelected) {
        this.stores = stores;
        this.onBranchSelected = onBranchSelected;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View container;
        public final ImageView image;
        public final TextView name;
        public final TextView address;
        public final TextView distance;
        public final ImageView checkIcon;

        public ViewHolder(View view) {
            super(view);
            container = view.findViewById(R.id.store_branch_container);
            image = view.findViewById(R.id.store_branch_image);
            name = view.findViewById(R.id.store_branch_name);
            address = view.findViewById(R.id.store_branch_address);
            distance = view.findViewById(R.id.store_branch_distance);
            checkIcon = view.findViewById(R.id.store_branch_check);
        }
    }

    public void updateData(List<StoreEntity> newStores) {
        this.stores = newStores;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.skin_item_store_branch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StoreEntity store = stores.get(position);
        holder.name.setText(store.getTenCuaHang());
        holder.address.setText(store.getDiaChiDayDu());
        
        int randomSeed = store.getId().hashCode();
        double mockDistance = 1.0 + (Math.abs(randomSeed) % 140) / 10.0;
        holder.distance.setText(String.format(java.util.Locale.US, "%.1fkm", mockDistance));
        
        String imageUrl = "";
        if (!imageUrl.isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(imageUrl)
                    .target(holder.image)
                    .crossfade(true)
                    .placeholder(R.drawable.imv_logo)
                    .error(R.drawable.imv_logo)
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        } else {
            holder.image.setImageResource(R.drawable.imv_logo);
        }

        boolean isSelected = store.getId().equals(selectedStoreId);
        holder.checkIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        
        if (isSelected) {
            holder.container.setBackgroundResource(R.drawable.skin_bg_store_card);
        } else {
            holder.container.setBackgroundResource(R.drawable.skin_bg_store_card);
        }

        holder.container.setOnClickListener(v -> {
            selectedStoreId = store.getId();
            notifyDataSetChanged();
            onBranchSelected.onBranchSelected(store);
        });
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }
}
