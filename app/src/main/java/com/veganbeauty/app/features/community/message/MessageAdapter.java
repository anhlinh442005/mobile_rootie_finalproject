package com.veganbeauty.app.features.community.message;

import android.graphics.Typeface;
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
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.ConversationEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ConversationEntity conversation);
    }

    private List<ConversationEntity> items;
    private final OnItemClickListener onItemClick;

    public MessageAdapter(List<ConversationEntity> items, OnItemClickListener onItemClick) {
        this.items = items;
        this.onItemClick = onItemClick;
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivAvatar;
        public final View vActiveDot;
        public final TextView tvName;
        public final TextView tvLastMessage;
        public final TextView tvTime;
        public final View vUnreadDot;

        public MessageViewHolder(View view) {
            super(view);
            ivAvatar = view.findViewById(R.id.ivAvatar);
            vActiveDot = view.findViewById(R.id.vActiveDot);
            tvName = view.findViewById(R.id.tvName);
            tvLastMessage = view.findViewById(R.id.tvLastMessage);
            tvTime = view.findViewById(R.id.tvTime);
            vUnreadDot = view.findViewById(R.id.vUnreadDot);
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.com_item_message, parent, false);
        return new MessageViewHolder(view);
    }

    private String formatTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) return "";
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = format.parse(timestampStr);
            if (date == null) return "";

            Calendar calendar = Calendar.getInstance();
            Calendar now = Calendar.getInstance();
            calendar.setTime(date);

            SimpleDateFormat formatTime = new SimpleDateFormat("HH:mm", new Locale("vi", "VN"));
            SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));

            String timeStr = formatTime.format(calendar.getTime());

            if (calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                return timeStr;
            } else {
                String dateStr = formatDate.format(calendar.getTime());
                return timeStr + " " + dateStr;
            }
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ConversationEntity item = items.get(position);

        String currentUserId = ProfileSession.getCurrentUserId(holder.itemView.getContext());
        String partnerId = "";
        if (item.getMembers() != null) {
            for (String member : item.getMembers()) {
                if (!member.equals(currentUserId)) {
                    partnerId = member;
                    break;
                }
            }
        }

        ConversationEntity.MemberInfo partnerInfo = null;
        if (item.getMemberInfo() != null) {
            partnerInfo = item.getMemberInfo().get(partnerId);
        }

        String partnerName = partnerInfo != null ? partnerInfo.getName() : "Unknown";
        String partnerAvatar = partnerInfo != null ? partnerInfo.getAvatar() : "";

        if (partnerName.equals("You") || partnerName.equals("Unknown") || partnerAvatar.isEmpty()) {
            List<UserEntity> users = new LocalJsonReader(holder.itemView.getContext()).getUsers();
            UserEntity foundUser = null;
            if (users != null) {
                for (UserEntity u : users) {
                    if (u.getUser_id() != null && u.getUser_id().equals(partnerId)) {
                        foundUser = u;
                        break;
                    }
                }
            }
            if (foundUser != null) {
                if (partnerName.equals("You") || partnerName.equals("Unknown")) {
                    partnerName = foundUser.getFull_name() != null ? foundUser.getFull_name() : partnerName;
                }
                if (partnerAvatar.isEmpty() && foundUser.getAvatar() != null) {
                    partnerAvatar = foundUser.getAvatar();
                }
            } else if ("test_001".equals(partnerId)) {
                if (partnerName.equals("You") || partnerName.equals("Unknown")) {
                    partnerName = "Test User";
                }
            }
        }

        String lastMsgText = item.getLastMessage() != null ? item.getLastMessage() : "";
        String lastTime = formatTimestamp(item.getLastMessageAt() != null ? item.getLastMessageAt() : "");

        holder.tvName.setText(partnerName);
        holder.tvLastMessage.setText(lastMsgText);
        holder.tvTime.setText(lastTime);

        if (!partnerAvatar.isEmpty()) {
            int errorRes = partnerId.equals("rootie_vn") ? R.drawable.ic_logo_rootie : R.drawable.mascot_message;
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(partnerAvatar)
                    .target(holder.ivAvatar)
                    .crossfade(true)
                    .placeholder(R.color.gray_light)
                    .error(errorRes)
                    .transformations(new CircleCropTransformation())
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        } else {
            if (partnerId.equals("rootie_vn")) {
                holder.ivAvatar.setImageResource(R.drawable.ic_logo_rootie);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.mascot_message);
            }
        }

        boolean isActive = false;
        if (item.getActiveBy() != null) {
            isActive = item.getActiveBy().contains(partnerId);
        }
        holder.vActiveDot.setVisibility(isActive ? View.VISIBLE : View.GONE);

        boolean isUnread = false;
        if (item.getUnreadBy() != null) {
            isUnread = item.getUnreadBy().contains(currentUserId);
        }
        
        if (isUnread) {
            holder.vUnreadDot.setVisibility(View.VISIBLE);
            holder.tvName.setTypeface(null, Typeface.BOLD);
            holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
            holder.tvLastMessage.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.black, null));
        } else {
            holder.vUnreadDot.setVisibility(View.GONE);
            holder.tvName.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMessage.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.gray_dark, null));
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClick != null) {
                onItemClick.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<ConversationEntity> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }
}
