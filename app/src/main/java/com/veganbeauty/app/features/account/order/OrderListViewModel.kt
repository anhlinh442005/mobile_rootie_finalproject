package com.veganbeauty.app.features.account.order

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.core.base.RootieViewModel
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class OrderListViewModel(
    private val repository: OrderRepository,
    private val appContext: Context
) : RootieViewModel() {

    private val _selectedStatus = MutableStateFlow("Tất cả")
    val selectedStatus: LiveData<String> = _selectedStatus.asLiveData()

<<<<<<< HEAD
    private val currentUserId = "test_001" // In real app, get from session

    // Combined Flow: filter orders in memory for super-responsive UI updates
    val filteredOrders: LiveData<List<OrderEntity>> = combine(
        repository.getBuyerOrders(currentUserId),
=======
    /**
     * Stream of orders scoped to the current buyer.
     *
     * Resolves the [ProfileSession] once at construction so the same
     * scope applies to all observers; recomputes when the buyer logs
     * in or out by relying on [refreshOrders] (which the host fragment
     * should call on resume).
     *
     * For logged-in users: uses userId to fetch their orders
     * For guests: uses guestPhone (stored after checkout) to fetch their orders
     */
    private val scopedOrders: kotlinx.coroutines.flow.Flow<List<OrderEntity>> =
        if (ProfileSession.isLoggedIn(appContext)) {
            // Logged-in user: use their userId
            repository.getOrdersForBuyer(
                userId = ProfileSession.getUserId(appContext).takeIf { it.isNotBlank() },
                phone = null
            )
        } else {
            // Guest user: use their stored guest phone for order tracking
            val guestPhone = ProfileSession.getGuestPhone(appContext).takeIf { it.isNotBlank() }
            repository.getOrdersForBuyer(
                userId = null,
                phone = guestPhone
            )
        }

    // Combined Flow: filter orders in memory for super-responsive UI updates
    val filteredOrders: LiveData<List<OrderEntity>> = combine(
        scopedOrders,
>>>>>>> 35f09837414391a9ba011bce61277d4577c69501
        _selectedStatus
    ) { orders, status ->
        if (status == "Tất cả") {
            orders
        } else {
            orders.filter { it.status.equals(status, ignoreCase = true) }
        }
    }.asLiveData()

<<<<<<< HEAD
    val orderStats: LiveData<String> = repository.getBuyerOrders(currentUserId).map { orders ->
=======
    val orderStats: LiveData<String> = scopedOrders.map { orders ->
>>>>>>> 35f09837414391a9ba011bce61277d4577c69501
        val total = orders.size
        val pending = orders.count { it.status.equals("Chờ xử lý", ignoreCase = true) }
        "$total đơn • $pending chờ xử lý"
    }.asLiveData()

    init {
        refreshOrders()
    }

    fun refreshOrders() {
        viewModelScope.launch {
            try {
                repository.refreshOrders()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setFilter(status: String) {
        _selectedStatus.value = status
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                repository.cancelOrder(orderId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
