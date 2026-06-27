package com.veganbeauty.app.features.community.com_feed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.UserEntity;

import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(UserEntity user);
    }

    private List<UserEntity> users;
    private final OnUserClickListener onUserClick;

    public UserSearchAdapter(List<UserEntity> users, OnUserClickListener onUserClick) {
        this.users = users;
        this.onUserClick = onUserClick;
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivAvatar;
        public final TextView tvName;
        public final TextView tvUsername;

        public UserViewHolder(View view) {
            super(view);
            ivAvatar = view.findViewById(R.id.ivAvatar);
            tvName = view.findViewById(R.id.tvName);
            tvUsername = view.findViewById(R.id.tvUsername);
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.com_item_search_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserEntity user = users.get(position);
        
        String name = (user.getFull_name() != null && !user.getFull_name().trim().isEmpty()) 
                      ? user.getFull_name() : user.getUsername();
        holder.tvName.setText(name);
        
        String username = user.getUsername() != null ? user.getUsername() : "";
        holder.tvUsername.setText("@" + username.toLowerCase().replace(" ", "_"));
        
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(user.getAvatar())
                    .target(holder.ivAvatar)
                    .crossfade(true)
                    .transformations(new CircleCropTransformation())
                    .placeholder(R.drawable.img_avatar)
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.img_avatar);
        }

        holder.itemView.setOnClickListener(v -> onUserClick.onUserClick(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateData(List<UserEntity> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }
}
