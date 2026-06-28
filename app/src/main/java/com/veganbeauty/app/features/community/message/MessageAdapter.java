package com.veganbeauty.app.features.community.message;

import android.graphics.Color;
import android.graphics.Typeface;
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
import com.veganbeauty.app.utils.MessageTimeFormatter;
import com.veganbeauty.app.utils.RootieBrandHelper;

import java.util.ArrayList;
import java.util.List;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public interface OnMessageClickListener {
        void onClick(ConversationEntity conversation);
    }

    private final List<ConversationEntity> list;
    private final String currentUserId;
    private final OnMessageClickListener listener;

    public MessageAdapter(List<ConversationEntity> list, String currentUserId, OnMessageClickListener listener) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_message, parent, false);
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

        if (holder.tvLastMessage != null) {
            holder.tvLastMessage.setText(item.getLastMessage() != null ? item.getLastMessage() : "");
        }
        if (holder.tvTime != null) {
            String timeRaw = item.getLastMessageAt();
            if (timeRaw == null || timeRaw.trim().isEmpty()) {
                timeRaw = item.getUpdatedAt();
            }
            holder.tvTime.setText(MessageTimeFormatter.formatConversationTime(timeRaw));
        }

        boolean isUnread = item.getUnreadBy() != null && item.getUnreadBy().contains(currentUserId);
        if (holder.tvLastMessage != null) {
            if (isUnread) {
                holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
                holder.tvLastMessage.setTextColor(Color.BLACK);
            } else {
                holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
                holder.tvLastMessage.setTextColor(Color.GRAY);
            }
        }
        if (holder.viewUnread != null) {
            holder.viewUnread.setVisibility(isUnread ? View.VISIBLE : View.GONE);
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
        TextView tvLastMessage;
        TextView tvTime;
        View viewUnread;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            viewUnread = itemView.findViewById(R.id.vUnreadDot);
        }
    }
}
