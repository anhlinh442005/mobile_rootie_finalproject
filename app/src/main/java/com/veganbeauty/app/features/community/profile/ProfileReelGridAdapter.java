package com.veganbeauty.app.features.community.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.ReelEntity;

import java.util.List;

public class ProfileReelGridAdapter extends RecyclerView.Adapter<ProfileReelGridAdapter.ReelGridViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private final List<ReelEntity> reels;
    private final OnItemClickListener onItemClick;

    public ProfileReelGridAdapter(List<ReelEntity> reels, OnItemClickListener onItemClick) {
        this.reels = reels;
        this.onItemClick = onItemClick;
    }

    static class ReelGridViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final ImageView ivMultipleIcon;

        ReelGridViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.ivGridImage);
            ivMultipleIcon = view.findViewById(R.id.ivMultipleIcon);
        }
    }

    @NonNull
    @Override
    public ReelGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.com_item_profile_grid, parent, false);
        return new ReelGridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReelGridViewHolder holder, int position) {
        ReelEntity reel = reels.get(position);
        holder.imageView.setVisibility(View.VISIBLE);
        View textView = holder.itemView.findViewById(R.id.tvGridText);
        if (textView != null) {
            textView.setVisibility(View.GONE);
        }
        holder.ivMultipleIcon.setVisibility(View.GONE);

        Glide.with(holder.imageView.getContext())
                .load(reel.getThumbnailUrl())
                .centerCrop()
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> onItemClick.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return reels.size();
    }
}
