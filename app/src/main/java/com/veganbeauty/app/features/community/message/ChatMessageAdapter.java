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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.ChatMessageEntity;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

public class ChatMessageAdapter extends ListAdapter<ChatMessageEntity, ChatMessageAdapter.ViewHolder> {

    private static final long GROUP_GAP_MS = 30L * 60L * 1000L;

    private final String currentUserId;
    private String partnerAvatarUrl = "";

    public ChatMessageAdapter(String currentUserId) {
        super(new DiffCallback());
        this.currentUserId = currentUserId;
    }

    public void setPartnerAvatarUrl(String partnerAvatarUrl) {
        this.partnerAvatarUrl = partnerAvatarUrl != null ? partnerAvatarUrl : "";
        notifyDataSetChanged();
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
        boolean isLike = MessageHelper.isLikeMessage(msg.getText());

        if (isLike) {
            holder.tvMessage.setVisibility(View.GONE);
            holder.ivLikeMessage.setVisibility(View.VISIBLE);
            holder.llBubble.setBackground(null);
            holder.llBubble.setPadding(0, 0, 0, 0);

            int likeSize = (int) (36 * holder.itemView.getResources().getDisplayMetrics().density);
            ViewGroup.LayoutParams likeParams = holder.ivLikeMessage.getLayoutParams();
            likeParams.width = likeSize;
            likeParams.height = likeSize;
            holder.ivLikeMessage.setLayoutParams(likeParams);

            int likeTint = ContextCompat.getColor(holder.itemView.getContext(), R.color.basic_green);
            holder.ivLikeMessage.setColorFilter(likeTint);
        } else {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.ivLikeMessage.setVisibility(View.GONE);
            holder.tvMessage.setText(msg.getText());
            holder.llBubble.setBackgroundResource(R.drawable.com_bg_card_rounded);

            int horizontalPadding = (int) (16 * holder.itemView.getResources().getDisplayMetrics().density);
            int verticalPadding = (int) (10 * holder.itemView.getResources().getDisplayMetrics().density);
            holder.llBubble.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        }

        if (isMine) {
            holder.vLeftSpacer.setVisibility(View.VISIBLE);
            holder.vRightSpacer.setVisibility(View.GONE);
            holder.ivPartnerAvatar.setVisibility(View.GONE);
            if (!isLike) {
                holder.llBubble.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#758864")));
                holder.tvMessage.setTextColor(Color.WHITE);
            }
        } else {
            holder.vLeftSpacer.setVisibility(View.GONE);
            holder.vRightSpacer.setVisibility(View.VISIBLE);
            boolean showAvatar = shouldShowPartnerAvatar(position);
            holder.ivPartnerAvatar.setVisibility(showAvatar ? View.VISIBLE : View.INVISIBLE);
            if (!isLike) {
                holder.llBubble.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F3F4F6")));
                holder.tvMessage.setTextColor(Color.BLACK);
            }
            if (showAvatar) {
                if (partnerAvatarUrl.isEmpty()) {
                    holder.ivPartnerAvatar.setImageResource(R.drawable.img_avatar);
                } else {
                    ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                            .data(partnerAvatarUrl)
                            .crossfade(true)
                            .transformations(new CircleCropTransformation())
                            .placeholder(R.drawable.img_avatar)
                            .error(R.drawable.img_avatar)
                            .target(holder.ivPartnerAvatar)
                            .build();
                    Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
                }
            }
        }
    }

    /** Avatar on the last bubble of each consecutive partner group (Messenger-style). */
    private boolean shouldShowPartnerAvatar(int position) {
        ChatMessageEntity current = getItem(position);
        if (position >= getItemCount() - 1) {
            return true;
        }
        ChatMessageEntity next = getItem(position + 1);
        if (!current.getSenderId().equals(next.getSenderId())) {
            return true;
        }
        return !isWithinGroupGap(current.getSentAt(), next.getSentAt());
    }

    private boolean isWithinGroupGap(String earlierSentAt, String laterSentAt) {
        long earlier = parseSentAtMillis(earlierSentAt);
        long later = parseSentAtMillis(laterSentAt);
        if (earlier <= 0 || later <= 0) {
            return true;
        }
        return later - earlier <= GROUP_GAP_MS;
    }

    private long parseSentAtMillis(String sentAt) {
        if (sentAt == null || sentAt.trim().isEmpty()) {
            return 0L;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(sentAt).getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ImageView ivLikeMessage;
        View vLeftSpacer;
        View vRightSpacer;
        ImageView ivPartnerAvatar;
        LinearLayout llBubble;

        ViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            ivLikeMessage = v.findViewById(R.id.ivLikeMessage);
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
