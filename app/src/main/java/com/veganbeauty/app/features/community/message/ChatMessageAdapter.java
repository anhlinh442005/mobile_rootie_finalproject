package com.veganbeauty.app.features.community.message;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.ChatMessageEntity;

public class ChatMessageAdapter extends ListAdapter<ChatMessageEntity, ChatMessageAdapter.ViewHolder> {

    private final String currentUserId;

    public ChatMessageAdapter(String currentUserId) {
        super(new DiffCallback());
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_chat_bubble, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessageEntity msg = getItem(position);
        boolean isMine = msg.getSenderId().equals(currentUserId);

        holder.tvMessage.setText(msg.getText());

        if (isMine) {
            holder.vLeftSpacer.setVisibility(View.VISIBLE);
            holder.vRightSpacer.setVisibility(View.GONE);
            holder.ivPartnerAvatar.setVisibility(View.GONE);
            holder.llBubble.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#758864")));
            holder.tvMessage.setTextColor(Color.WHITE);
        } else {
            holder.vLeftSpacer.setVisibility(View.GONE);
            holder.vRightSpacer.setVisibility(View.VISIBLE);
            holder.ivPartnerAvatar.setVisibility(View.VISIBLE);
            holder.llBubble.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3F4F6")));
            holder.tvMessage.setTextColor(Color.BLACK);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        View vLeftSpacer;
        View vRightSpacer;
        ImageView ivPartnerAvatar;
        LinearLayout llBubble;

        ViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            vLeftSpacer = v.findViewById(R.id.vLeftSpacer);
            vRightSpacer = v.findViewById(R.id.vRightSpacer);
            ivPartnerAvatar = v.findViewById(R.id.ivPartnerAvatar);
            llBubble = v.findViewById(R.id.llBubble);
        }
    }

    static class DiffCallback extends DiffUtil.ItemCallback<ChatMessageEntity> {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessageEntity oldItem, @NonNull ChatMessageEntity newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull ChatMessageEntity oldItem, @NonNull ChatMessageEntity newItem) {
            return oldItem.getText().equals(newItem.getText());
        }
    }
}
