package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.dao.OrderDao
import com.veganbeauty.app.data.local.dao.RewardPointDao
import com.veganbeauty.app.data.local.dao.UserGiftDao
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.local.entities.OrderItem
import com.veganbeauty.app.data.local.entities.RewardPointEntity
import com.veganbeauty.app.data.local.entities.UserGiftEntity
import com.veganbeauty.app.data.local.LocalJsonReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class OrderRepository(
    private val orderDao: OrderDao,
    private val rewardPointDao: RewardPointDao,
    private val userGiftDao: UserGiftDao,
    private val localJsonReader: LocalJsonReader
) {
    // Flow of all orders
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()

    fun getBuyerOrders(userId: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByUserId(userId)
    }

    fun getAffiliateOrders(referrerUserId: String): Flow<List<OrderEntity>> {
        return orderDao.getAffiliateOrdersByReferrer(referrerUserId)
    }

    /**
     * Stream of orders scoped to the current buyer.
     *
     *  - Logged-in members see only their own orders
     *    (`userId == currentUserId`).
     *  - Guests see only the orders that were placed with their
     *    billing phone, so the same phone = same cart. This prevents
     *    the previous bug where a guest's order list was empty even
     *    after a successful checkout, or where a test-user account
     *    could see guest orders that did not belong to them.
     *
     *  - When [userId] is blank/empty and [phone] is blank/empty the
     *    function falls back to the unfiltered [allOrders] flow so
     *    legacy / pre-fix installs do not regress to an empty list.
     */
    fun getOrdersForBuyer(userId: String?, phone: String?): Flow<List<OrderEntity>> {
        val safeUserId = userId?.trim().orEmpty()
        val safePhone = phone?.trim().orEmpty()
        return when {
            safeUserId.isNotEmpty() -> orderDao.getOrdersForUser(safeUserId)
            safePhone.isNotEmpty() -> orderDao.getOrdersForGuestPhone(safePhone)
            else -> orderDao.getAllOrders()
        }
    }
    suspend fun refreshOrders() {
        try {
            // Clear all old orders first as requested
            orderDao.deleteAllOrders()
            
            val mockOrders = localJsonReader.getAllOrders()
            if (mockOrders.isNotEmpty()) {
                orderDao.insertOrders(mockOrders)
            }
            
            // Seed initial reward points (Reset if needed, but keeping for now as it's separate from orders collection)
            val totalPoints = rewardPointDao.getTotalPointsFlow().first()
            if (totalPoints == null) {
                // To end up with exactly 8,500 net, we insert 11,200 initial and the 3 history deductions:
                // 11,200 - 500 - 200 - 2,000 = 8,500 xu.
                rewardPointDao.insertRewardPoints(
                    RewardPointEntity(
                        orderId = "SYSTEM_INITIAL",
                        points = 11200,
                        reason = "Tích điểm mua sắm",
                        timestamp = 1767261600000L // 01/01/2026
                    )
                )
                rewardPointDao.insertRewardPoints(
                    RewardPointEntity(
                        orderId = "REDEEM_HIST_1",
                        points = -2000,
                        reason = "Đổi quà: Sữa rửa mặt mini",
                        timestamp = 1772043600000L // 25/02/2026 18:20
                    )
                )
                rewardPointDao.insertRewardPoints(
                    RewardPointEntity(
                        orderId = "REDEEM_HIST_2",
                        points = -200,
                        reason = "Đổi quà: Freeship Đơn 0Đ",
                        timestamp = 1772698500000L // 05/03/2026 09:15
                    )
                )
                rewardPointDao.insertRewardPoints(
                    RewardPointEntity(
                        orderId = "REDEEM_HIST_3",
                        points = -500,
                        reason = "Đổi quà: Voucher Giảm 50K",
                        timestamp = 1775831400000L // 10/04/2026 14:30
                    )
                )
            }

            // Seed initial owned user gifts if empty
            if (userGiftDao.getUserGiftCount() == 0) {
                userGiftDao.insertUserGifts(
                    listOf(
                        UserGiftEntity(
                            giftId = "voucher_50k",
                            title = "Voucher Giảm 50K",
                            description = "Áp dụng cho đơn hàng từ 300K, sản phẩm nguyên giá.",
                            cost = 500,
                            expiryDate = "2026-12-30 23:59:59",
                            status = "Còn hạn",
                            giftType = "voucher_discount",
                            code = "SAVE50K",
                            minOrderValue = 300000,
                            applicableProducts = "Chăm Sóc Da Mặt",
                            offerType = "fixed_amount",
                            discountValue = 50000,
                            acquiredTimestamp = 1775831400000L
                        ),
                        UserGiftEntity(
                            giftId = "gift_cleanser",
                            title = "Quà tặng: Sữa rửa mặt",
                            description = "Nhận miễn phí 1 tuýp sữa rửa mặt bí đao mini 15ml.",
                            cost = 1000,
                            expiryDate = "2026-12-15 23:59:59",
                            status = "Còn hạn",
                            giftType = "product",
                            code = "FREECLN",
                            minOrderValue = 0,
                            applicableProducts = "Sữa rửa mặt",
                            offerType = "product_gift",
                            productId = "p003",
                            discountValue = 0,
                            acquiredTimestamp = 1772043600000L
                        ),
                        UserGiftEntity(
                            giftId = "gift_freeship",
                            title = "Freeship Đơn 0Đ",
                            description = "Miễn phí vận chuyển toàn quốc cho mọi đơn hàng.",
                            cost = 200,
                            expiryDate = "2026-06-11 23:59:59",
                            status = "Hôm nay",
                            giftType = "voucher_freeship",
                            code = "FREESHIP",
                            minOrderValue = 150000,
                            applicableProducts = "Tất cả sản phẩm",
                            offerType = "percentage",
                            discountValue = 100,
                            acquiredTimestamp = 1772698500000L
                        ),
                        UserGiftEntity(
                            giftId = "voucher_10_percent",
                            title = "Giảm 10% Cho Sản Phẩm Bưởi",
                            description = "Áp dụng cho dòng sản phẩm tinh chất vỏ bưởi dưỡng tóc.",
                            cost = 300,
                            expiryDate = "2026-10-31 23:59:59",
                            status = "Hết hạn",
                            giftType = "voucher_discount",
                            code = "GRAPE10",
                            minOrderValue = 0,
                            applicableProducts = "Tinh chất bưởi",
                            offerType = "percentage",
                            discountValue = 10,
                            acquiredTimestamp = 1767261600000L
                        )
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Redeem gift and log point deduction
    suspend fun redeemGift(
        giftId: String,
        title: String,
        description: String,
        cost: Int,
        expiryDate: String,
        code: String,
        giftType: String,
        minOrderValue: Int = 0,
        applicableProducts: String = "Tất cả sản phẩm",
        offerType: String = "fixed_amount",
        productId: String? = null,
        discountValue: Int = 0
    ): Boolean {
        val total = rewardPointDao.getTotalPointsFlow().first() ?: 0
        if (total >= cost) {
            // Deduct points
            rewardPointDao.insertRewardPoints(
                RewardPointEntity(
                    orderId = "REDEEM_$giftId",
                    points = -cost,
                    reason = "Đổi quà: $title",
                    timestamp = System.currentTimeMillis()
                )
            )
            com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsToFirestore(localJsonReader.getContext())
            // Add user gift
            userGiftDao.insertUserGift(
                UserGiftEntity(
                    giftId = giftId,
                    title = title,
                    description = description,
                    cost = cost,
                    expiryDate = expiryDate,
                    status = "Còn hạn",
                    giftType = giftType,
                    code = code,
                    minOrderValue = minOrderValue,
                    applicableProducts = applicableProducts,
                    offerType = offerType,
                    productId = productId,
                    discountValue = discountValue,
                    acquiredTimestamp = System.currentTimeMillis()
                )
            )
            return true
        }
        return false
    }

    // Watch all user gifts
    fun getAllUserGifts(): Flow<List<UserGiftEntity>> {
        return userGiftDao.getAllUserGiftsFlow()
    }

    // Delete a user gift by DB id (Xoá khỏi danh sách)
    suspend fun deleteUserGiftById(id: Int): Boolean {
        return userGiftDao.deleteUserGiftById(id) > 0
    }

    // Update order review and award points if criteria met
    suspend fun updateOrderReview(
        orderId: String,
        stars: Int,
        text: String?,
        image: String?,
        isAnonymous: Boolean,
        recommend: Boolean
    ): Boolean {
        // Validate reward criteria: stars > 0, non-empty review text, and has image
        val wordCount = text?.trim()?.split("\\s+".toRegex())?.filter { it.isNotEmpty() }?.size ?: 0
        val hasText = text != null && text.trim().isNotEmpty() && wordCount <= 200
        val hasImage = image != null && image.isNotEmpty()
        val qualifiesForCoins = stars > 0 && hasText && hasImage

        // Update the review details in database
        orderDao.updateOrderReviewStatus(
            orderId = orderId,
            hasReview = true
        )

        // Award points if qualified and hasn't already been awarded
        if (qualifiesForCoins) {
            val alreadyAwarded = rewardPointDao.hasReceivedPointsForOrder(orderId)
            if (!alreadyAwarded) {
                rewardPointDao.insertRewardPoints(
                    RewardPointEntity(
                        orderId = orderId,
                        points = 200,
                        reason = "Đánh giá đơn hàng $orderId kèm hình ảnh"
                    )
                )
                com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsToFirestore(localJsonReader.getContext())
                return true // Points awarded
            }
        }
        return false // Points not awarded
    }

    // Cancel order (locally)
    suspend fun cancelOrder(orderId: String) {
        orderDao.updateOrderStatus(orderId, "Đã hủy")
        OrderStatusNotifier.simulateOnly(orderDao, orderId, "Đã hủy")
    }

    // Watch a specific order reactively
    fun getOrderById(orderId: String): Flow<OrderEntity?> {
        return orderDao.getOrderByIdFlow(orderId)
    }

    // Update order status. The repository delegates the actual SQL update
    // to the DAO and then asks [OrderStatusNotifier] to dispatch the
    // mock SMS / Email notification per the hybrid checkout plan.
    suspend fun updateOrderStatus(orderId: String, status: String) {
        orderDao.updateOrderStatus(orderId, status)
        OrderStatusNotifier.simulateOnly(orderDao, orderId, status)
    }

    // Ensure order exists in local DB. If not, seed a realistic mock fallback.
    suspend fun ensureOrderExists(orderId: String) {
        val existing = orderDao.getOrderById(orderId)
        if (existing == null) {
            val mockOrder = OrderEntity(
                id = orderId,
                orderDate = "16/06/2026",
                orderTime = "15:30",
                status = "Đang giao",
                totalAmount = 202000L,
                subTotal = 222000L,
                items = listOf(
                    OrderItem(
                        productId = "product_rose_cream",
                        productName = "Kem dưỡng hoa hồng Cocoon 50ml",
                        productImage = "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1781257994/rose_cream_u2kgwf.png",
                        quantity = 1,
                        price = 222000L
                    )
                ),
                userId = "test_001",
                isGuest = false,
                shippingName = "Nguyễn Khánh Xuân",
                shippingPhone = "090 123 4567",
                shippingAddress = "123 Đường Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh",
                shippingCost = 30000L,
                voucherDiscount = 50000L,
                paymentMethod = "Thanh toán khi nhận hàng (COD)"
            )
            orderDao.insertOrder(mockOrder)
        }
    }
}
