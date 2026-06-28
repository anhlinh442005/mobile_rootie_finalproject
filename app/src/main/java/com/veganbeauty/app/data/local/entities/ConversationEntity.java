package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class ConversationEntity {
    @NonNull private String id;
    @Nullable @SerializedName("chat_type") private String chatType;
    @Nullable private List<String> members;
    @Nullable @SerializedName("member_info") private Map<String, MemberInfoEntity> memberInfo;
    @Nullable @SerializedName("active_by") private List<String> activeBy;
    @Nullable @SerializedName("typing_by") private List<String> typingBy;
    @Nullable @SerializedName("unread_by") private List<String> unreadBy;
    @Nullable @SerializedName("created_at") private String createdAt;
    @Nullable @SerializedName("updated_at") private String updatedAt;
    @Nullable @SerializedName("last_message") private String lastMessage;
    @Nullable @SerializedName("last_message_at") private String lastMessageAt;
    @Nullable private List<ChatMessageEntity> messages;

    public ConversationEntity(@NonNull String id, @Nullable String chatType, @Nullable List<String> members, @Nullable Map<String, MemberInfoEntity> memberInfo, @Nullable List<String> activeBy, @Nullable List<String> typingBy, @Nullable List<String> unreadBy, @Nullable String createdAt, @Nullable String updatedAt, @Nullable String lastMessage, @Nullable String lastMessageAt, @Nullable List<ChatMessageEntity> messages) {
        this.id = id; this.chatType = chatType; this.members = members; this.memberInfo = memberInfo; this.activeBy = activeBy; this.typingBy = typingBy; this.unreadBy = unreadBy; this.createdAt = createdAt; this.updatedAt = updatedAt; this.lastMessage = lastMessage; this.lastMessageAt = lastMessageAt; this.messages = messages;
    }

    public ConversationEntity() {
        this.id = "";
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @Nullable public String getChatType() { return chatType; }
    public void setChatType(@Nullable String chatType) { this.chatType = chatType; }

    @Nullable public List<String> getMembers() { return members; }
    public void setMembers(@Nullable List<String> members) { this.members = members; }

    @Nullable public Map<String, MemberInfoEntity> getMemberInfo() { return memberInfo; }
    public void setMemberInfo(@Nullable Map<String, MemberInfoEntity> memberInfo) { this.memberInfo = memberInfo; }

    @Nullable public List<String> getActiveBy() { return activeBy; }
    public void setActiveBy(@Nullable List<String> activeBy) { this.activeBy = activeBy; }

    @Nullable public List<String> getTypingBy() { return typingBy; }
    public void setTypingBy(@Nullable List<String> typingBy) { this.typingBy = typingBy; }

    @Nullable public List<String> getUnreadBy() { return unreadBy; }
    public void setUnreadBy(@Nullable List<String> unreadBy) { this.unreadBy = unreadBy; }

    @Nullable public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(@Nullable String createdAt) { this.createdAt = createdAt; }

    @Nullable public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(@Nullable String updatedAt) { this.updatedAt = updatedAt; }

    @Nullable public String getLastMessage() { return lastMessage; }
    public void setLastMessage(@Nullable String lastMessage) { this.lastMessage = lastMessage; }

    @Nullable public String getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(@Nullable String lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    @Nullable public List<ChatMessageEntity> getMessages() { return messages; }
    public void setMessages(@Nullable List<ChatMessageEntity> messages) { this.messages = messages; }
}
