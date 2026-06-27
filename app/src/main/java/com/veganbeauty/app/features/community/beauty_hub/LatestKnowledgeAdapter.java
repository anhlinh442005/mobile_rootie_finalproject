package com.veganbeauty.app.features.community.beauty_hub;

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
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LatestKnowledgeAdapter extends RecyclerView.Adapter<LatestKnowledgeAdapter.ViewHolder> {

    private List<CommunityBlogEntity> blogs;

    public LatestKnowledgeAdapter(List<CommunityBlogEntity> blogs) {
        this.blogs = blogs;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivBlog;
        public final TextView tvTitle;
        public final TextView tvDesc;
        public final TextView tvDate;

        public ViewHolder(View view) {
            super(view);
            ivBlog = view.findViewById(R.id.ivBlog);
            tvTitle = view.findViewById(R.id.tvBlogTitle);
            tvDesc = view.findViewById(R.id.tvBlogDesc);
            tvDate = view.findViewById(R.id.tvBlogDate);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.com_item_latest_knowledge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityBlogEntity item = blogs.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDesc.setText(item.getShortDescription());
        
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            Date date = parser.parse(item.getPublishedAt());
            holder.tvDate.setText(date != null ? formatter.format(date) : item.getPublishedAt());
        } catch (Exception e) {
            holder.tvDate.setText(item.getPublishedAt());
        }
        
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(item.getImageUrl())
                    .target(holder.ivBlog)
                    .crossfade(true)
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_placeholder)
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        } else {
            holder.ivBlog.setImageResource(R.drawable.img_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return blogs.size();
    }

    public void updateData(List<CommunityBlogEntity> newBlogs) {
        this.blogs = newBlogs;
        notifyDataSetChanged();
    }
}
