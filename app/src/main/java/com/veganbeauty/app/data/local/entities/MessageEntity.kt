package com.veganbeauty.app.data.local.entities

import com.google.gson.annotations.SerializedName

data class ConversationEntity(
    val id: String,
    @SerializedName("chat_type")
    val chatType: String? = null,
    val members: List<String>? = emptyList(),
    @SerializedName("member_info")
    val memberInfo: Map<String, MemberInfoEntity>? = emptyMap(),
    @SerializedName("active_by")
    val activeBy: List<String>? = emptyList(),
    @SerializedName("typing_by")
    val typingBy: List<String>? = emptyList(),
    @SerializedName("unread_by")
    val unreadBy: List<String>? = emptyList(),
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("last_message")
    val lastMessage: String? = null,
    @SerializedName("last_message_at")
    val lastMessageAt: String? = null,
    val messages: List<ChatMessageEntity>? = emptyList()
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
