package com.veganbeauty.app.data.local.entities

data class ConversationEntity(
    val conversationId: String,
    val participants: List<String>,
    val partnerId: String,
    var partnerName: String,
    var partnerAvatar: String,
    val isActive: Boolean,
    val isTyping: Boolean,
    val lastMessage: LastMessageEntity?,
    val unreadCount: Map<String, Int>,
    val updatedAt: Long
)

data class LastMessageEntity(
    val messageId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long
)

data class ChatMessageEntity(
    val messageId: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val type: String,
    val createdAt: Long,
    val status: Map<String, String>,
    val readAt: Map<String, Long?>
)
