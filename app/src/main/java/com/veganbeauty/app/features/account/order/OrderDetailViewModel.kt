package com.veganbeauty.app.features.account.order

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.core.base.RootieViewModel
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.repository.OrderRepository
import kotlinx.coroutines.launch

class OrderDetailViewModel(
    private val repository: OrderRepository,
    private val orderId: String
) : RootieViewModel() {

    // Reactive observe a single order from the database Flow
    val order: LiveData<OrderEntity?> = repository.getOrderById(orderId).asLiveData()

    fun cancelOrder() {
        viewModelScope.launch {
            try {
                repository.updateOrderStatus(orderId, "Đã hủy")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun confirmReceived() {
        viewModelScope.launch {
            try {
                repository.updateOrderStatus(orderId, "Thành công")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reorder() {
        viewModelScope.launch {
            try {
                // In actual shop flow, this would add products to the cart.
                // Since this is mock UI, it can be handled by displaying standard android toast inside the Fragment.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
