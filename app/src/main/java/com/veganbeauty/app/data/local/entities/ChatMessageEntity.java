package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class ChatMessageEntity {
    @NonNull private String id;
    @NonNull @SerializedName("sender_id") private String senderId;
    @NonNull private String text;
    @NonNull @SerializedName("sent_at") private String sentAt;
    @Nullable @SerializedName("delivered_at") private String deliveredAt;
    @Nullable @SerializedName("seen_at") private String seenAt;

    public ChatMessageEntity() {
    }

    public ChatMessageEntity(@NonNull String id, @NonNull String senderId, @NonNull String text, @NonNull String sentAt, @Nullable String deliveredAt, @Nullable String seenAt) {
        this.id = id; this.senderId = senderId; this.text = text; this.sentAt = sentAt; this.deliveredAt = deliveredAt; this.seenAt = seenAt;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    @NonNull public String getSenderId() { return senderId; }
    public void setSenderId(@NonNull String senderId) { this.senderId = senderId; }
    @NonNull public String getText() { return text; }
    public void setText(@NonNull String text) { this.text = text; }
    @NonNull public String getSentAt() { return sentAt; }
    public void setSentAt(@NonNull String sentAt) { this.sentAt = sentAt; }
    @Nullable public String getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(@Nullable String deliveredAt) { this.deliveredAt = deliveredAt; }
    @Nullable public String getSeenAt() { return seenAt; }
    public void setSeenAt(@Nullable String seenAt) { this.seenAt = seenAt; }
}
