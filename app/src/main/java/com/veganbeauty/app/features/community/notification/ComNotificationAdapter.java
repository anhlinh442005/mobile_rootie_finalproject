package com.veganbeauty.app.features.community.notification;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;


import com.veganbeauty.app.R;
import com.veganbeauty.app.databinding.ComItemNotificationBinding;
import com.veganbeauty.app.databinding.ComItemNotificationHeaderBinding;

public class ComNotificationAdapter extends ListAdapter<ComNotificationListItem, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public interface OnNotificationClickListener {
        void onClick(ComNotificationItem item);
    }

    private final OnNotificationClickListener onItemClick;
    private final OnNotificationClickListener onDeleteClick;

    public ComNotificationAdapter(OnNotificationClickListener onItemClick, OnNotificationClickListener onDeleteClick) {
        super(new DiffCallback());
        this.onItemClick = onItemClick;
        this.onDeleteClick = onDeleteClick;
    }

    @Override
    public int getItemViewType(int position) {
        ComNotificationListItem item = getItem(position);
        if (item instanceof ComNotificationListItem.Header) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            ComItemNotificationHeaderBinding binding = ComItemNotificationHeaderBinding.inflate(inflater, parent, false);
            return new HeaderViewHolder(binding);
        } else {
            ComItemNotificationBinding binding = ComItemNotificationBinding.inflate(inflater, parent, false);
            return new NotificationViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ComNotificationListItem item = getItem(position);
        if (item instanceof ComNotificationListItem.Header) {
            ((HeaderViewHolder) holder).bind((ComNotificationListItem.Header) item);
        } else if (item instanceof ComNotificationListItem.Notification) {
            ((NotificationViewHolder) holder).bind(((ComNotificationListItem.Notification) item).getItem());
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ComItemNotificationHeaderBinding binding;

        public HeaderViewHolder(ComItemNotificationHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ComNotificationListItem.Header header) {
            binding.tvHeaderTitle.setText(header.getTitle());
        }
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ComItemNotificationBinding binding;

        public NotificationViewHolder(ComItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ComNotificationItem item) {
            Context context = binding.getRoot().getContext();

            String htmlContent = "<b>" + item.getUserName() + "</b> " + item.getContent();
            binding.tvContent.setText(HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.tvTime.setText(item.getTime() + " • " + item.getDate());

            binding.viewUnreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

            String avatarUrl = item.getUserAvatar();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                com.bumptech.glide.Glide.with(binding.ivAvatar.getContext()).load(avatarUrl).placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar).circleCrop().into(binding.ivAvatar);
            } else {
                binding.ivAvatar.setImageResource(R.drawable.img_avatar);
            }

            int badgeRes;
            String type = item.getActionType() != null ? item.getActionType() : "";
            switch (type) {
                case "COMMENT":
                case "REPLY":
                case "REPOST":
                    badgeRes = R.drawable.ic_chat;
                    break;
                case "LIKE":
                    badgeRes = R.drawable.ic_heart;
                    break;
                case "SHARE":
                    badgeRes = R.drawable.ic_plane;
                    break;
                case "ORDER_PLACED":
                    badgeRes = R.drawable.ic_truck;
                    break;
                case "ORDER_COMPLETED":
                    badgeRes = R.drawable.ic_check_circle;
                    break;
                case "WITHDRAW":
                    badgeRes = R.drawable.ic_cash;
                    break;
                default:
                    badgeRes = R.drawable.ic_bell;
                    break;
            }

            int badgeColor;
            int badgeBgColor;

            switch (type) {
                case "COMMENT":
                case "REPLY":
                    badgeColor = ContextCompat.getColor(context, R.color.status_processing_text);
                    badgeBgColor = ContextCompat.getColor(context, R.color.status_processing_bg);
                    break;
                case "LIKE":
                    badgeColor = ContextCompat.getColor(context, R.color.status_pending_text);
                    badgeBgColor = ContextCompat.getColor(context, R.color.status_pending_bg);
                    break;
                case "SHARE":
                case "REPOST":
                    badgeColor = ContextCompat.getColor(context, R.color.primary);
                    badgeBgColor = ContextCompat.getColor(context, R.color.gray_light);
                    break;
                case "ORDER_PLACED":
                    badgeColor = ContextCompat.getColor(context, R.color.status_delivering_text);
                    badgeBgColor = ContextCompat.getColor(context, R.color.status_delivering_bg);
                    break;
                case "ORDER_COMPLETED":
                case "WITHDRAW":
                    badgeColor = ContextCompat.getColor(context, R.color.status_success_text);
                    badgeBgColor = ContextCompat.getColor(context, R.color.status_success_bg);
                    break;
                default:
                    badgeColor = ContextCompat.getColor(context, R.color.primary);
                    badgeBgColor = ContextCompat.getColor(context, R.color.gray_light);
                    break;
            }

            binding.ivTypeBadge.setImageResource(badgeRes);
            binding.ivTypeBadge.setColorFilter(badgeColor);
            binding.ivTypeBadge.setBackgroundTintList(ColorStateList.valueOf(badgeBgColor));

            binding.cardNotification.setOnClickListener(v -> {
                if (onItemClick != null) onItemClick.onClick(item);
            });

            binding.btnDeleteNotification.setOnClickListener(v -> {
                if (onDeleteClick != null) onDeleteClick.onClick(item);
            });
        }
    }

    private static class DiffCallback extends DiffUtil.ItemCallback<ComNotificationListItem> {
        @Override
        public boolean areItemsTheSame(@NonNull ComNotificationListItem oldItem, @NonNull ComNotificationListItem newItem) {
            if (oldItem instanceof ComNotificationListItem.Header && newItem instanceof ComNotificationListItem.Header) {
                return ((ComNotificationListItem.Header) oldItem).getTitle().equals(((ComNotificationListItem.Header) newItem).getTitle());
            } else if (oldItem instanceof ComNotificationListItem.Notification && newItem instanceof ComNotificationListItem.Notification) {
                return ((ComNotificationListItem.Notification) oldItem).getItem().getId().equals(((ComNotificationListItem.Notification) newItem).getItem().getId());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ComNotificationListItem oldItem, @NonNull ComNotificationListItem newItem) {
            return oldItem.equals(newItem);
        }
    }
}
