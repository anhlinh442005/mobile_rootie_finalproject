package com.veganbeauty.app.data.repository;

import com.veganbeauty.app.data.local.dao.CartDao;
import com.veganbeauty.app.data.local.entities.CartItemEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

public class CartRepository {
    private final CartDao cartDao;
    private final Flow<List<CartItemEntity>> allCartItems;

    public CartRepository(CartDao cartDao) {
        this.cartDao = cartDao;
        this.allCartItems = cartDao.getAllCartItems();
    }

    public Flow<List<CartItemEntity>> getAllCartItems() {
        return allCartItems;
    }

    public void insertOrUpdate(CartItemEntity item) {
        cartDao.insertCartItem(item);
    }

    public void update(CartItemEntity item) {
        cartDao.updateCartItem(item);
    }

    public void delete(CartItemEntity item) {
        cartDao.deleteCartItem(item);
    }

    public void clear() {
        cartDao.clearCart();
    }
}
