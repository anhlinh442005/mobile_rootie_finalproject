package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.dao.OrderDao
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.local.LocalJsonReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class OrderRepository(
    private val orderDao: OrderDao,
    private val localJsonReader: LocalJsonReader
) {
    // Flow of all orders
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()

    // Get orders by status
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByStatus(status)
    }

    // Refresh orders from assets (Force synchronization from orders.json on launch)
    suspend fun refreshOrders() {
        try {
            val mockOrders = localJsonReader.getAllOrders()
            if (mockOrders.isNotEmpty()) {
                orderDao.deleteAllOrders()
                orderDao.insertOrders(mockOrders)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
