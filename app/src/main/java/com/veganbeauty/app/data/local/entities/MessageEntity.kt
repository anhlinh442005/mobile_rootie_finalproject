package com.veganbeauty.app.data.local.entities

import com.google.gson.annotations.SerializedName

data class ConversationEntity(
    val id: String,
    @SerializedName("chat_type")
    val chatType: String?,
    val members: List<String>,
    @SerializedName("member_info")
    val memberInfo: Map<String, MemberInfoEntity>,
    @SerializedName("active_by")
    val activeBy: List<String>,
    @SerializedName("typing_by")
    val typingBy: List<String>,
    @SerializedName("unread_by")
    val unreadBy: List<String>,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("last_message")
    val lastMessage: String,
    @SerializedName("last_message_at")
    val lastMessageAt: String,
    val messages: List<ChatMessageEntity>
)

data class MemberInfoEntity(
    val name: String,
    val avatar: String
)

data class ChatMessageEntity(
    val id: String,
    @SerializedName("sender_id")
    val senderId: String,
    val text: String,
    @SerializedName("sent_at")
    val sentAt: String,
    @SerializedName("delivered_at")
    val deliveredAt: String?,
    @SerializedName("seen_at")
    val seenAt: String?
)
