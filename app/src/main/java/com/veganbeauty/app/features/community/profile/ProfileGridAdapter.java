package com.veganbeauty.app.features.community.profile;

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
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;

import java.util.ArrayList;
import java.util.List;

public class ProfileGridAdapter extends RecyclerView.Adapter<ProfileGridAdapter.GridViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    private final List<CommunityPostEntity> posts;
    private final OnItemClickListener onItemClick;

    public ProfileGridAdapter(List<CommunityPostEntity> posts, OnItemClickListener onItemClick) {
        this.posts = posts;
        this.onItemClick = onItemClick;
    }

    public static class GridViewHolder extends RecyclerView.ViewHolder {
        public final ImageView imageView;
        public final TextView tvGridText;
        public final ImageView ivMultipleIcon;

        public GridViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.ivGridImage);
            tvGridText = view.findViewById(R.id.tvGridText);
            ivMultipleIcon = view.findViewById(R.id.ivMultipleIcon);
        }
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_profile_grid, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        CommunityPostEntity post = posts.get(position);
        
        List<String> mediaUrls = new ArrayList<>();
        if (post.getMediaUrlsString() != null) {
            String[] split = post.getMediaUrlsString().split(",");
            for (String s : split) {
                if (s != null && !s.trim().isEmpty()) {
                    mediaUrls.add(s);
                }
            }
        }
        
        if (!mediaUrls.isEmpty()) {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.tvGridText.setVisibility(View.GONE);
            
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(mediaUrls.get(0))
                    .target(holder.imageView)
                    .crossfade(true)
                    .placeholder(android.R.color.darker_gray)
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        } else {
            holder.imageView.setVisibility(View.GONE);
            holder.tvGridText.setVisibility(View.VISIBLE);
            holder.tvGridText.setText(post.getContent());
        }
        
        if (mediaUrls.size() > 1) {
            holder.ivMultipleIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivMultipleIcon.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> onItemClick.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }
}
