package com.veganbeauty.app.data.local.entities

data class NotificationItem(
    val id: String,
    val title: String,
    val content: String,
    val time: String,
    val category: String, // "Khuyến mãi", "Đơn hàng", "Tin nhắn"
    val tag: String? = null,
    val voucherCode: String? = null,
    val actionText: String? = null,
    var isRead: Boolean = false,
    val section: String, // "Hôm nay", "Hôm qua", "Cũ hơn"
    val iconResName: String
)
