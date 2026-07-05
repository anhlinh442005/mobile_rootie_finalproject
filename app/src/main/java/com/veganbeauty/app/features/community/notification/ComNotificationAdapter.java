package com.veganbeauty.app.features.community.notification;

import android.animation.ObjectAnimator;
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
    private final OnNotificationClickListener onMarkReadClick;

    // Track currently open swipe item
    private NotificationViewHolder currentOpenHolder = null;
    private static final int ACTION_WIDTH_DP = 128; // 2 buttons × 64dp each

    public ComNotificationAdapter(OnNotificationClickListener onItemClick,
                                  OnNotificationClickListener onDeleteClick,
                                  OnNotificationClickListener onMarkReadClick) {
        super(new DiffCallback());
        this.onItemClick = onItemClick;
        this.onDeleteClick = onDeleteClick;
        this.onMarkReadClick = onMarkReadClick;
    }

    /** Snap the currently-open item back to closed position */
    public void closeOpenedItem() {
        if (currentOpenHolder != null) {
            currentOpenHolder.animateForeground(0f);
            currentOpenHolder = null;
        }
    }

    /** Called by the Fragment's touch listener to open/close the swiped item */
    public void openItem(NotificationViewHolder holder, float actionWidthPx) {
        if (currentOpenHolder != null && currentOpenHolder != holder) {
            currentOpenHolder.animateForeground(0f);
        }
        holder.animateForeground(-actionWidthPx);
        currentOpenHolder = holder;
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
        private ComNotificationItem currentItem;

        public NotificationViewHolder(ComItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ComNotificationItem getBoundItem() { return currentItem; }

        public View getForeground() { return binding.cardNotification; }

        /** Animate the foreground card to a target X translation */
        public void animateForeground(float targetX) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(binding.cardNotification, "translationX", binding.cardNotification.getTranslationX(), targetX);
            anim.setDuration(180);
            anim.start();
        }

        public void bind(ComNotificationItem item) {
            this.currentItem = item;
            // Reset swipe state on re-bind
            binding.cardNotification.setTranslationX(0f);
            Context context = binding.getRoot().getContext();

            String htmlContent = "<b>" + item.getUserName() + "</b> " + item.getContent();
            binding.tvContent.setText(HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.tvTime.setText(item.getTime() + " • " + item.getDate());

            binding.viewUnreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

            binding.cardNotification.setCardBackgroundColor(
                    item.isRead()
                            ? ContextCompat.getColor(context, R.color.white)
                            : android.graphics.Color.parseColor("#E8F5E9")
            );

            String avatarUrl = item.getUserAvatar();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                com.bumptech.glide.Glide.with(binding.ivAvatar.getContext()).load(avatarUrl)
                        .placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar)
                        .circleCrop().into(binding.ivAvatar);
            } else {
                binding.ivAvatar.setImageResource(R.drawable.img_avatar);
            }

            int badgeRes;
            String type = item.getActionType() != null ? item.getActionType() : "";
            switch (type) {
                case "COMMENT": case "REPLY": case "REPOST": badgeRes = R.drawable.ic_chat; break;
                case "LIKE":    badgeRes = R.drawable.ic_heart; break;
                case "SHARE":   badgeRes = R.drawable.ic_plane; break;
                case "ORDER_PLACED":   badgeRes = R.drawable.ic_shipping; break;
                case "ORDER_COMPLETED": badgeRes = R.drawable.ic_circle_checked; break;
                case "WITHDRAW":        badgeRes = R.drawable.ic_cash; break;
                default:                badgeRes = R.drawable.ic_bell; break;
            }

            int badgeColor, badgeBgColor;
            switch (type) {
                case "COMMENT": case "REPLY":
                    badgeColor   = ContextCompat.getColor(context, R.color.status_processing_text);
                    badgeBgColor = ContextCompat.getColor(context, R.color.status_processing_bg); break;
                case "LIKE":
                    badgeColor   = ContextCompat.getColor(context, R.color.status_pending_text);
                    badgeBgColor = ContextCompat.getColor(context, R.color.status_pending_bg); break;
                case "ORDER_PLACED":
                    badgeColor   = ContextCompat.getColor(context, R.color.status_delivering_text);
                    badgeBgColor = ContextCompat.getColor(context, R.color.status_delivering_bg); break;
                case "ORDER_COMPLETED": case "WITHDRAW":
                    badgeColor   = ContextCompat.getColor(context, R.color.status_success_text);
                    badgeBgColor = ContextCompat.getColor(context, R.color.status_success_bg); break;
                default:
                    badgeColor   = ContextCompat.getColor(context, R.color.primary);
                    badgeBgColor = ContextCompat.getColor(context, R.color.gray_light); break;
            }

            binding.ivTypeBadge.setImageResource(badgeRes);
            binding.ivTypeBadge.setColorFilter(badgeColor);
            binding.ivTypeBadge.setBackgroundTintList(ColorStateList.valueOf(badgeBgColor));

            // Foreground tap → open notification
            binding.cardNotification.setOnClickListener(v -> {
                if (binding.cardNotification.getTranslationX() != 0) {
                    closeOpenedItem();
                    return;
                }
                if (onItemClick != null) onItemClick.onClick(item);
            });

            // Action button clicks
            binding.btnMarkReadAction.setOnClickListener(v -> {
                closeOpenedItem();
                if (onMarkReadClick != null) onMarkReadClick.onClick(item);
            });
            binding.btnDeleteAction.setOnClickListener(v -> {
                closeOpenedItem();
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
