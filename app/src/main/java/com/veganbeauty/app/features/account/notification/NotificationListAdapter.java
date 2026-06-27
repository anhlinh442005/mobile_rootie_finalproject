package com.veganbeauty.app.features.account.notification;

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

    private final OnNotificationClickListener onItemClick;
    private final OnNotificationClickListener onActionClick;
    private final OnNotificationClickListener onMarkReadClick;
    private final OnNotificationClickListener onDeleteClick;

    public NotificationListAdapter(
            OnNotificationClickListener onItemClick,
            OnNotificationClickListener onActionClick,
            OnNotificationClickListener onMarkReadClick,
            OnNotificationClickListener onDeleteClick
    ) {
        super(new DiffCallback());
        this.onItemClick = onItemClick;
        this.onActionClick = onActionClick;
        this.onMarkReadClick = onMarkReadClick;
        this.onDeleteClick = onDeleteClick;
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

        public void bind(NotificationItem item) {
            Context context = binding.getRoot().getContext();

            binding.tvTitle.setText(item.getTitle());
            binding.tvContent.setText(item.getContent());
            binding.tvTime.setText(item.getTime());

            binding.viewUnreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

            int resId = context.getResources().getIdentifier(item.getIconResName(), "drawable", context.getPackageName());
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

            if (item.getActionText() != null && !item.getActionText().isEmpty() &&
                !item.getActionText().equalsIgnoreCase("CHI TIẾT") &&
                !item.getActionText().equalsIgnoreCase("CHI TIẾT ĐƠN")) {
                binding.btnAction.setVisibility(View.VISIBLE);
                binding.btnAction.setText(item.getActionText());
                binding.btnAction.setOnClickListener(v -> {
                    if (onActionClick != null) onActionClick.onClick(item);
                });
            } else {
                binding.btnAction.setVisibility(View.GONE);
            }

            binding.btnMarkRead.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
            binding.btnMarkRead.setOnClickListener(v -> {
                if (onMarkReadClick != null) onMarkReadClick.onClick(item);
            });

            binding.btnDelete.setOnClickListener(v -> {
                if (onDeleteClick != null) onDeleteClick.onClick(item);
            });

            binding.cardNotification.setOnClickListener(v -> {
                if (onItemClick != null) onItemClick.onClick(item);
            });
        }
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

        @Override
        public boolean areContentsTheSame(@NonNull NotificationListItem oldItem, @NonNull NotificationListItem newItem) {
            return oldItem.equals(newItem);
        }
    }
}
