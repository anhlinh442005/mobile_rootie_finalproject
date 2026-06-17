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

data class AffiliateInfo(
    val isAffiliateOrder: Boolean = false,
    val affiliate_id: String, // Tracking ID: AFFORD-xxxx
    val affiliateCode: String,
    val referrerUserId: String,
    val referrerName: String,
    val sourceType: String = "community_post",
    val sourcePostId: String? = null,
    val commissionRate: Double,
    val commissionAmount: Long,
    val commissionStatus: String // confirmed, pending, rejected
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String, // Real ID: ORD-xxxx
    val userId: String,
    val status: String, // Chờ xác nhận, Đang xử lý, Đang giao, Hoàn tất, Đã hủy
    val orderDate: String,
    val orderTime: String,
    val totalAmount: Long,
    val subTotal: Long,
    val items: List<OrderItem>,
    val shippingName: String,
    val shippingPhone: String,
    val shippingAddress: String,
    val shippingCost: Long,
    val voucherDiscount: Long,
    val paymentMethod: String,
    val expectedDeliveryTime: String? = null,
    val deliveryDate: String? = null,
    val isAffiliate: Boolean = false,
    @androidx.room.Embedded(prefix = "aff_")
    val affiliate: AffiliateInfo? = null,
    val hasReview: Boolean = false
)
