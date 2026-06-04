package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

data class OrderItem(
    val productId: String,
    val productName: String,
    val productImage: String,
    val quantity: Int,
    val price: Long
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val orderId: String,
    val orderDate: String,
    val orderTime: String,
    val status: String,
    val totalAmount: Long,
    val items: List<OrderItem>,
    val shippingName: String = "Nguyễn Văn A",
    val shippingPhone: String = "090 123 4567",
    val shippingAddress: String = "123 Đường Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh",
    val shippingCost: Long = 30000L,
    val voucherDiscount: Long = 50000L,
    val paymentMethod: String = "Thanh toán qua Ví MoMo",
    val expectedDeliveryTime: String? = null,
    val hasReview: Boolean = false,
    val reviewStars: Int = 0,
    val reviewText: String? = null,
    val reviewImage: String? = null,
    val isAnonymous: Boolean = false,
    val recommendToFriends: Boolean = false
)
