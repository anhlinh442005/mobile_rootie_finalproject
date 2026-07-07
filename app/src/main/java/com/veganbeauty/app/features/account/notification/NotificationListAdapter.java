package com.veganbeauty.app.features.account.notification;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.databinding.AccountNotificationHeaderBinding;
import com.veganbeauty.app.databinding.AccountNotificationItemBinding;

public class NotificationListAdapter extends ListAdapter<NotificationListItem, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public interface OnNotificationClickListener {
        void onClick(NotificationItem item);
    }

    private static final int MAX_CONTENT_WORDS = 200;

    private final OnNotificationClickListener onItemClick;
    private final OnNotificationClickListener onMarkReadClick;
    private final OnNotificationClickListener onDeleteClick;

    // Track currently open swipe item
    private NotificationViewHolder currentOpenHolder = null;

    public NotificationListAdapter(
            OnNotificationClickListener onItemClick,
            OnNotificationClickListener onMarkReadClick,
            OnNotificationClickListener onDeleteClick
    ) {
        super(new DiffCallback());
        this.onItemClick = onItemClick;
        this.onMarkReadClick = onMarkReadClick;
        this.onDeleteClick = onDeleteClick;
    }

    private static String truncateWords(String text, int maxWords) {
        if (text == null) return "";
        String[] words = text.trim().split("\\s+");
        if (words.length <= maxWords) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) sb.append(' ');
            sb.append(words[i]);
        }
        sb.append("...");
        return sb.toString();
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
        NotificationListItem item = getItem(position);
        if (item instanceof NotificationListItem.Header) {
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
            AccountNotificationHeaderBinding binding = AccountNotificationHeaderBinding.inflate(inflater, parent, false);
            return new HeaderViewHolder(binding);
        } else {
            AccountNotificationItemBinding binding = AccountNotificationItemBinding.inflate(inflater, parent, false);
            return new NotificationViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NotificationListItem item = getItem(position);
        if (item instanceof NotificationListItem.Header) {
            ((HeaderViewHolder) holder).bind((NotificationListItem.Header) item);
        } else if (item instanceof NotificationListItem.Notification) {
            ((NotificationViewHolder) holder).bind(((NotificationListItem.Notification) item).getItem());
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final AccountNotificationHeaderBinding binding;

        public HeaderViewHolder(AccountNotificationHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(NotificationListItem.Header header) {
            binding.tvHeaderTitle.setText(header.getTitle());
        }
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final AccountNotificationItemBinding binding;

        public NotificationViewHolder(AccountNotificationItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public View getForeground() { return binding.cardNotification; }

        /** Animate the foreground card to a target X translation */
        public void animateForeground(float targetX) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(binding.cardNotification, "translationX", binding.cardNotification.getTranslationX(), targetX);
            anim.setDuration(180);
            anim.start();
        }

        public void bind(NotificationItem item) {
            Context context = binding.getRoot().getContext();

            // Reset swipe state on re-bind
            binding.cardNotification.setTranslationX(0f);

            binding.tvTitle.setText(item.getTitle());
            binding.tvContent.setText(truncateWords(item.getContent(), MAX_CONTENT_WORDS));
            binding.tvTime.setText(NotificationDateHelper.getDisplayTime(item.getTime()));

            binding.viewUnreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
            binding.cardNotification.setCardBackgroundColor(
                    item.isRead()
                            ? ContextCompat.getColor(context, R.color.white)
                            : android.graphics.Color.parseColor("#E8F5E9")
            );

            int resId = resolveNotificationIconRes(context, item.getIconResName());
            if (resId != 0) {
                binding.ivIcon.setImageResource(resId);
            } else {
                binding.ivIcon.setImageResource(R.drawable.ic_voucher);
            }

            int iconTint;
            int bgTint;

            if ("Khuyến mãi".equals(item.getCategory())) {
                iconTint = ContextCompat.getColor(context, R.color.status_pending_text);
                bgTint = ContextCompat.getColor(context, R.color.status_pending_bg);
            } else if ("Đơn hàng".equals(item.getCategory())) {
                iconTint = ContextCompat.getColor(context, R.color.status_delivering_text);
                bgTint = ContextCompat.getColor(context, R.color.status_delivering_bg);
            } else if ("Khác".equals(item.getCategory())) {
                iconTint = ContextCompat.getColor(context, R.color.status_processing_text);
                bgTint = ContextCompat.getColor(context, R.color.status_processing_bg);
            } else {
                iconTint = ContextCompat.getColor(context, R.color.primary);
                bgTint = ContextCompat.getColor(context, R.color.gray_light);
            }

            binding.ivIcon.setColorFilter(iconTint);
            binding.ivIcon.setBackgroundTintList(ColorStateList.valueOf(bgTint));

            // Foreground tap → open notification
            binding.cardNotification.setOnClickListener(v -> {
                if (binding.cardNotification.getTranslationX() != 0) {
                    closeOpenedItem();
                    return;
                }
                if (onItemClick != null) onItemClick.onClick(item);
            });

            // Action button clicks (revealed on swipe left)
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

    private static int resolveNotificationIconRes(Context context, String iconResName) {
        if (iconResName == null || iconResName.isEmpty()
                || "ic_bell".equals(iconResName)
                || "home_ic_bell".equals(iconResName)) {
            return R.drawable.ic_bell;
        }
        int resId = context.getResources().getIdentifier(iconResName, "drawable", context.getPackageName());
        return resId != 0 ? resId : R.drawable.ic_bell;
    }

    private static class DiffCallback extends DiffUtil.ItemCallback<NotificationListItem> {
        @Override
        public boolean areItemsTheSame(@NonNull NotificationListItem oldItem, @NonNull NotificationListItem newItem) {
            if (oldItem instanceof NotificationListItem.Header && newItem instanceof NotificationListItem.Header) {
                return ((NotificationListItem.Header) oldItem).getTitle().equals(((NotificationListItem.Header) newItem).getTitle());
            } else if (oldItem instanceof NotificationListItem.Notification && newItem instanceof NotificationListItem.Notification) {
                return ((NotificationListItem.Notification) oldItem).getItem().getId().equals(((NotificationListItem.Notification) newItem).getItem().getId());
            }
            return false;
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull NotificationListItem oldItem, @NonNull NotificationListItem newItem) {
            return oldItem.equals(newItem);
        }
    }
}
