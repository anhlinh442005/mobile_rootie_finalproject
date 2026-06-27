package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NotificationItem {
    @NonNull private String id;
    @NonNull private String title;
    @NonNull private String content;
    @NonNull private String time;
    @NonNull private String category;
    @Nullable private String tag;
    @Nullable private String voucherCode;
    @Nullable private String actionText;
    private boolean isRead;
    @NonNull private String section;
    @NonNull private String iconResName;
    @Nullable private String notificationType;
    @Nullable private String orderId;
    @Nullable private String scheduleId;

    public NotificationItem(@NonNull String id, @NonNull String title, @NonNull String content, @NonNull String time, @NonNull String category, @Nullable String tag, @Nullable String voucherCode, @Nullable String actionText, boolean isRead, @NonNull String section, @NonNull String iconResName, @Nullable String notificationType, @Nullable String orderId, @Nullable String scheduleId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.time = time;
        this.category = category;
        this.tag = tag;
        this.voucherCode = voucherCode;
        this.actionText = actionText;
        this.isRead = isRead;
        this.section = section;
        this.iconResName = iconResName;
        this.notificationType = notificationType;
        this.orderId = orderId;
        this.scheduleId = scheduleId;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    @NonNull public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }
    @NonNull public String getContent() { return content; }
    public void setContent(@NonNull String content) { this.content = content; }
    @NonNull public String getTime() { return time; }
    public void setTime(@NonNull String time) { this.time = time; }
    @NonNull public String getCategory() { return category; }
    public void setCategory(@NonNull String category) { this.category = category; }
    @Nullable public String getTag() { return tag; }
    public void setTag(@Nullable String tag) { this.tag = tag; }
    @Nullable public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(@Nullable String voucherCode) { this.voucherCode = voucherCode; }
    @Nullable public String getActionText() { return actionText; }
    public void setActionText(@Nullable String actionText) { this.actionText = actionText; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    @NonNull public String getSection() { return section; }
    public void setSection(@NonNull String section) { this.section = section; }
    @NonNull public String getIconResName() { return iconResName; }
    public void setIconResName(@NonNull String iconResName) { this.iconResName = iconResName; }
    @Nullable public String getNotificationType() { return notificationType; }
    public void setNotificationType(@Nullable String notificationType) { this.notificationType = notificationType; }
    @Nullable public String getOrderId() { return orderId; }
    public void setOrderId(@Nullable String orderId) { this.orderId = orderId; }
    @Nullable public String getScheduleId() { return scheduleId; }
    public void setScheduleId(@Nullable String scheduleId) { this.scheduleId = scheduleId; }
}
