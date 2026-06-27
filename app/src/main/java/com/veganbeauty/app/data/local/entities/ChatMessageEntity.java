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

    public ChatMessageEntity(@NonNull String id, @NonNull String senderId, @NonNull String text, @NonNull String sentAt, @Nullable String deliveredAt, @Nullable String seenAt) {
        this.id = id; this.senderId = senderId; this.text = text; this.sentAt = sentAt; this.deliveredAt = deliveredAt; this.seenAt = seenAt;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getSenderId() { return senderId; }
    @NonNull public String getText() { return text; }
    @NonNull public String getSentAt() { return sentAt; }
    @Nullable public String getDeliveredAt() { return deliveredAt; }
    @Nullable public String getSeenAt() { return seenAt; }
}
