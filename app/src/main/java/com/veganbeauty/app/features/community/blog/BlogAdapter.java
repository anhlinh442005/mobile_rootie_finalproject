package com.veganbeauty.app.features.community.blog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;

import java.util.List;

public class BlogAdapter extends RecyclerView.Adapter<BlogAdapter.BlogViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(BlogPost post);
    }

    private List<BlogPost> posts;
    private final OnItemClickListener onItemClick;

    public BlogAdapter(List<BlogPost> posts, OnItemClickListener onItemClick) {
        this.posts = posts;
        this.onItemClick = onItemClick;
    }

    public static class BlogViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivPostImage;
        public final TextView tvPostCategory;
        public final TextView tvPostTitle;
        public final TextView tvPostDesc;

        public BlogViewHolder(View view) {
            super(view);
            ivPostImage = view.findViewById(R.id.ivPostImage);
            tvPostCategory = view.findViewById(R.id.tvPostCategory);
            tvPostTitle = view.findViewById(R.id.tvPostTitle);
            tvPostDesc = view.findViewById(R.id.tvPostDesc);
        }
    }

    @NonNull
    @Override
    public BlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_blog_post, parent, false);
        return new BlogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlogViewHolder holder, int position) {
        BlogPost post = posts.get(position);
        holder.tvPostTitle.setText(post.getTitle());
        
        String category = (post.getCategory() != null && !post.getCategory().isEmpty()) ? post.getCategory() : "Dưỡng da";
        holder.tvPostCategory.setText(category);
        
        holder.tvPostDesc.setText("🕒 " + post.getDate() + " " + post.getDescription());
        
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(holder.ivPostImage.getContext()).load(post.getImageUrl()).error(R.color.gray_light).into(holder.ivPostImage);
        } else {
            holder.ivPostImage.setImageResource(R.color.gray_light);
        }
        
        holder.itemView.setOnClickListener(v -> onItemClick.onItemClick(post));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updateData(List<BlogPost> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }
}
