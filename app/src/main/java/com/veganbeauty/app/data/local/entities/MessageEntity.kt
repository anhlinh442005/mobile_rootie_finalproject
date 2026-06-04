package com.veganbeauty.app.data.local.entities

data class MessageEntity(
    val id: String,
    val partnerId: String,
    val partnerName: String,
    val partnerAvatar: String,
    val isActive: Boolean,
    val isUnread: Boolean,
    val isTyping: Boolean,
    val messages: List<ChatItemEntity>
)

data class ChatItemEntity(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: String,
    val isMine: Boolean,
    val status: String
)
