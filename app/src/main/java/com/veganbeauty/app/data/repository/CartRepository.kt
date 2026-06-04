package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.dao.CartDao
import com.veganbeauty.app.data.local.entities.CartItemEntity
import kotlinx.coroutines.flow.Flow

class CartRepository(private val cartDao: CartDao) {
    val allCartItems: Flow<List<CartItemEntity>> = cartDao.getAllCartItems()

    suspend fun insertOrUpdate(item: CartItemEntity) {
        cartDao.insertCartItem(item)
    }

    suspend fun update(item: CartItemEntity) {
        cartDao.updateCartItem(item)
    }

    suspend fun delete(item: CartItemEntity) {
        cartDao.deleteCartItem(item)
    }

    suspend fun clear() {
        cartDao.clearCart()
    }
}
