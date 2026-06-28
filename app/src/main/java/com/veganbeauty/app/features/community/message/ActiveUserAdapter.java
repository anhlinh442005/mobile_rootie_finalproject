package com.veganbeauty.app.features.community.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.data.local.entities.MemberInfoEntity;
import com.veganbeauty.app.utils.RootieBrandHelper;

import java.util.ArrayList;
import java.util.List;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

public class ActiveUserAdapter extends RecyclerView.Adapter<ActiveUserAdapter.ViewHolder> {

    public interface OnActiveUserClickListener {
        void onClick(ConversationEntity conversation);
    }

    private final List<ConversationEntity> list;
    private final String currentUserId;
    private final OnActiveUserClickListener listener;

    public ActiveUserAdapter(List<ConversationEntity> list, String currentUserId, OnActiveUserClickListener listener) {
        this.list = new ArrayList<>(list);
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateList(List<ConversationEntity> newList) {
        list.clear();
        if (newList != null) {
            list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_active_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationEntity item = list.get(position);
        
        String partnerId = "";
        if (item.getMembers() != null) {
            for (String memberId : item.getMembers()) {
                if (!memberId.equals(currentUserId)) {
                    partnerId = memberId;
                    break;
                }
            }
        }

        MemberInfoEntity partnerInfo = null;
        if (item.getMemberInfo() != null) {
            partnerInfo = item.getMemberInfo().get(partnerId);
        }

        if (holder.tvName != null) {
            holder.tvName.setText(partnerInfo != null ? partnerInfo.getName() : "Người dùng");
        }

        if (partnerInfo != null && holder.ivAvatar != null) {
            String avatarUrl = RootieBrandHelper.resolveAvatar(partnerId, partnerInfo.getAvatar());
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(avatarUrl)
                    .crossfade(true)
                    .transformations(new CircleCropTransformation())
                    .placeholder(R.drawable.img_avatar)
                    .error(R.drawable.img_avatar)
                    .target(holder.ivAvatar)
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        } else if (holder.ivAvatar != null) {
            holder.ivAvatar.setImageResource(R.drawable.img_avatar);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
        }
    }
}
