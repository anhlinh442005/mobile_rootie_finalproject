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

/**
 * Order entity. Supports both member and guest checkouts:
 *  - When [userId] is non-null, this order belongs to a logged-in member.
 *  - When [userId] is null, this is a guest (khách vãng lai) checkout.
 *
 * Customer details (name / phone / email / address) are always denormalized onto
 * the order itself rather than read from a user profile so that historical
 * snapshots remain intact even if the user later edits or deletes their profile.
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String, // Real ID: ORD-xxxx
    val userId: String? = null,
    val status: String, // Chờ xác nhận, Đang xử lý, Đang giao, Hoàn tất, Đã hủy
    val orderDate: String,
    val orderTime: String,
    val totalAmount: Long,
    val subTotal: Long,
    val items: List<OrderItem>,

    val isGuest: Boolean = false,
    val shippingName: String = "Nguyễn Văn A",
    val shippingPhone: String = "090 123 4567",
    val shippingAddress: String = "123 Đường Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh",
    val shippingCost: Long = 30000L,
    val voucherDiscount: Long = 0L,
    val paymentMethod: String = "Thanh toán qua Ví MoMo",
    val expectedDeliveryTime: String? = null,
    val deliveryDate: String? = null,
    
    val isAffiliate: Boolean = false,
    @androidx.room.Embedded(prefix = "aff_")
    val affiliate: AffiliateInfo? = null,
    
    val hasReview: Boolean = false,
    val reviewStars: Int = 0,
    val reviewText: String? = null,
    val reviewImage: String? = null,
    val isAnonymous: Boolean = false,
    val recommendToFriends: Boolean = false,
    
    val billingName: String? = null,
    val billingPhone: String? = null,
    val billingEmail: String? = null
)
