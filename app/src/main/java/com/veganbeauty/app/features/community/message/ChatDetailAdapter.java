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
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.CircleCropTransformation;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.ChatMessageEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatDetailAdapter extends RecyclerView.Adapter<ChatDetailAdapter.ChatViewHolder> {

    private List<ChatMessageEntity> items;
    private final OnMessageLongClickListener onMessageLongClick;
    private String partnerAvatar = "";
    private int selectedItemPosition = -1;

    public interface OnMessageLongClickListener {
        void onLongClick(ChatMessageEntity message);
    }

    public ChatDetailAdapter(List<ChatMessageEntity> items, OnMessageLongClickListener onMessageLongClick) {
        this.items = items;
        this.onMessageLongClick = onMessageLongClick;
    }

    public void setPartnerAvatar(String url) {
        partnerAvatar = url != null ? url : "";
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvTimestamp;
        public final ImageView ivPartnerAvatar;
        public final View vLeftSpacer;
        public final LinearLayout llBubble;
        public final TextView tvMessage;
        public final View vRightSpacer;
        public final TextView tvStatus;

        public ChatViewHolder(View view) {
            super(view);
            tvTimestamp = view.findViewById(R.id.tvTimestamp);
            ivPartnerAvatar = view.findViewById(R.id.ivPartnerAvatar);
            vLeftSpacer = view.findViewById(R.id.vLeftSpacer);
            llBubble = view.findViewById(R.id.llBubble);
            tvMessage = view.findViewById(R.id.tvMessage);
            vRightSpacer = view.findViewById(R.id.vRightSpacer);
            tvStatus = view.findViewById(R.id.tvStatus);
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_chat_bubble, parent, false);
        return new ChatViewHolder(view);
    }

    private long parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return 0L;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(dateStr).getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp == 0L) return "";
        Calendar calendar = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm", new Locale("vi", "VN"));
        SimpleDateFormat formatDate = new SimpleDateFormat("dd 'th' MM", new Locale("vi", "VN"));

        String timeStr = formatTime.format(calendar.getTime());

        if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
            return timeStr;
        } else {
            String dateStr = formatDate.format(calendar.getTime());
            return timeStr + " ng " + dateStr;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessageEntity item = items.get(position);
        long itemTime = parseDate(item.getSentAt());

        boolean showTimestamp;
        if (position == 0) {
            showTimestamp = true;
        } else {
            ChatMessageEntity prevItem = items.get(position - 1);
            long prevTime = parseDate(prevItem.getSentAt());
            long diff = itemTime - prevTime;
            showTimestamp = diff > 30 * 60 * 1000; // 30 minutes
        }

        if (showTimestamp || position == selectedItemPosition) {
            holder.tvTimestamp.setVisibility(View.VISIBLE);
            holder.tvTimestamp.setText(formatTimestamp(itemTime));
        } else {
            holder.tvTimestamp.setVisibility(View.GONE);
        }

        holder.llBubble.setOnClickListener(v -> {
            int prevSelected = selectedItemPosition;
            selectedItemPosition = (selectedItemPosition == position) ? -1 : position;
            if (prevSelected != -1) notifyItemChanged(prevSelected);
            if (selectedItemPosition != -1) notifyItemChanged(selectedItemPosition);
        });

        holder.tvMessage.setText(item.getText());

        String currentUserId = ProfileSession.getCurrentUserId(holder.itemView.getContext());
        boolean isMine = item.getSenderId() != null && item.getSenderId().equals(currentUserId);

        if (isMine) {
            holder.ivPartnerAvatar.setVisibility(View.GONE);
            holder.vLeftSpacer.setVisibility(View.VISIBLE);
            holder.vRightSpacer.setVisibility(View.GONE);
            holder.llBubble.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#758864")));
            holder.tvMessage.setTextColor(Color.WHITE);

            if (item.getSeenAt() != null && !item.getSeenAt().isEmpty() && position == selectedItemPosition) {
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvStatus.setText("Đã xem");
            } else if (item.getDeliveredAt() != null && !item.getDeliveredAt().isEmpty() && position == selectedItemPosition) {
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvStatus.setText("Đã nhận");
            } else {
                holder.tvStatus.setVisibility(View.GONE);
            }

            holder.llBubble.setOnLongClickListener(v -> {
                if (onMessageLongClick != null) {
                    onMessageLongClick.onLongClick(item);
                }
                return true;
            });
        } else {
            holder.ivPartnerAvatar.setVisibility(View.VISIBLE);
            holder.vLeftSpacer.setVisibility(View.GONE);
            holder.vRightSpacer.setVisibility(View.VISIBLE);
            holder.llBubble.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DEE2D3")));
            holder.tvMessage.setTextColor(Color.BLACK);
            holder.tvStatus.setVisibility(View.GONE);

            holder.llBubble.setOnLongClickListener(null);

            ChatMessageEntity nextItem = (position < items.size() - 1) ? items.get(position + 1) : null;
            boolean nextIsMine = nextItem != null && nextItem.getSenderId() != null && nextItem.getSenderId().equals(currentUserId);

            long nextTime = nextItem != null ? parseDate(nextItem.getSentAt()) : 0L;
            boolean nextIsClose = nextItem != null && (nextTime - itemTime <= 30 * 60 * 1000);

            if (nextItem != null && !nextIsMine) {
                holder.ivPartnerAvatar.setVisibility(View.INVISIBLE);
            } else {
                holder.ivPartnerAvatar.setVisibility(View.VISIBLE);
                if (!partnerAvatar.isEmpty()) {
                    ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                            .data(partnerAvatar)
                            .crossfade(true)
                            .placeholder(R.color.gray_light)
                            .error(R.drawable.mascot_message)
                            .transformations(new CircleCropTransformation())
                            .target(holder.ivPartnerAvatar)
                            .build();
                    Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
                } else {
                    ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                            .data(R.drawable.mascot_message)
                            .transformations(new CircleCropTransformation())
                            .target(holder.ivPartnerAvatar)
                            .build();
                    Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<ChatMessageEntity> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }
}
