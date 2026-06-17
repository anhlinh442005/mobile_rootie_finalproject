package com.veganbeauty.app.features.account.order

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.core.base.RootieViewModel
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class OrderListViewModel(private val repository: OrderRepository) : RootieViewModel() {

    private val _selectedStatus = MutableStateFlow("Tất cả")
    val selectedStatus: LiveData<String> = _selectedStatus.asLiveData()

    private val currentUserId = "test_001" // In real app, get from session

    // Combined Flow: filter orders in memory for super-responsive UI updates
    val filteredOrders: LiveData<List<OrderEntity>> = combine(
        repository.getBuyerOrders(currentUserId),
        _selectedStatus
    ) { orders, status ->
        if (status == "Tất cả") {
            orders
        } else {
            orders.filter { it.status.equals(status, ignoreCase = true) }
        }
    }.asLiveData()

    val orderStats: LiveData<String> = repository.getBuyerOrders(currentUserId).map { orders ->
        val total = orders.size
        val pending = orders.count { it.status.equals("Chờ xác nhận", ignoreCase = true) }
        "$total đơn • $pending chờ xác nhận"
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
