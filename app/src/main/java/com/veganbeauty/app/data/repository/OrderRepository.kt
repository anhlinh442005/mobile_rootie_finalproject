package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.dao.OrderDao
import com.veganbeauty.app.data.local.dao.RewardPointDao
import com.veganbeauty.app.data.local.dao.UserGiftDao
import com.veganbeauty.app.data.local.entities.OrderEntity
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

    // Get orders by status
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByStatus(status)
    }

    // Refresh orders from assets (Seed only if database is empty)
    suspend fun refreshOrders() {
        try {
            if (orderDao.getOrderCount() == 0) {
                val mockOrders = localJsonReader.getAllOrders()
                if (mockOrders.isNotEmpty()) {
                    orderDao.insertOrders(mockOrders)
                }
            }
            
            // Seed initial reward points (8,500 net coins) and transaction history if empty
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
                            expiryDate = "30/12/2026",
                            status = "Còn hạn",
                            giftType = "voucher",
                            code = "SAVE50K",
                            acquiredTimestamp = 1775831400000L
                        ),
                        UserGiftEntity(
                            giftId = "gift_cleanser",
                            title = "Quà tặng: Sữa rửa mặt",
                            description = "Nhận miễn phí 1 tuýp sữa rửa mặt bí đao mini 15ml.",
                            cost = 1000,
                            expiryDate = "15/12/2026",
                            status = "Còn hạn",
                            giftType = "product",
                            code = "FREECLN",
                            acquiredTimestamp = 1772043600000L
                        ),
                        UserGiftEntity(
                            giftId = "gift_freeship",
                            title = "Freeship Đơn 0Đ",
                            description = "Miễn phí vận chuyển toàn quốc cho mọi đơn hàng.",
                            cost = 200,
                            expiryDate = "Hôm nay",
                            status = "Hôm nay",
                            giftType = "freeship",
                            code = "FREESHIP",
                            acquiredTimestamp = 1772698500000L
                        ),
                        UserGiftEntity(
                            giftId = "voucher_10_percent",
                            title = "Giảm 10% Cho Sản Phẩm Bưởi",
                            description = "Áp dụng cho dòng sản phẩm tinh chất vỏ bưởi dưỡng tóc.",
                            cost = 300,
                            expiryDate = "31/10/2026",
                            status = "Hết hạn",
                            giftType = "voucher",
                            code = "GRAPE10",
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
        giftType: String
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
        orderDao.updateOrderReview(
            orderId = orderId,
            hasReview = true,
            stars = stars,
            text = text,
            image = image,
            isAnonymous = isAnonymous,
            recommend = recommend
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
                return true // Points awarded
            }
        }
        return false // Points not awarded
    }

    // Cancel order (locally)
    suspend fun cancelOrder(orderId: String) {
        orderDao.updateOrderStatus(orderId, "Đã hủy")
    }

    // Watch a specific order reactively
    fun getOrderById(orderId: String): Flow<OrderEntity?> {
        return orderDao.getOrderByIdFlow(orderId)
    }

    // Update order status
    suspend fun updateOrderStatus(orderId: String, status: String) {
        orderDao.updateOrderStatus(orderId, status)
    }
}
