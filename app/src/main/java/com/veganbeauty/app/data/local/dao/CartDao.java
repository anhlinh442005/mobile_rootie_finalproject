package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.veganbeauty.app.data.local.entities.CartItemEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface CartDao {
    @Query("SELECT * FROM cart_items")
    Flow<List<CartItemEntity>> getAllCartItems();

    @androidx.annotation.Nullable
    @Query("SELECT * FROM cart_items WHERE id = :itemId LIMIT 1")
    CartItemEntity getCartItemById(String itemId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertCartItem(CartItemEntity item);

    @Update
    int updateCartItem(CartItemEntity item);

    @Delete
    int deleteCartItem(CartItemEntity item);

    @Query("DELETE FROM cart_items")
    int clearCart();

    @Query("SELECT COUNT(*) FROM cart_items")
    int getCartItemCount();

    @Query("SELECT * FROM cart_items LIMIT 6")
    List<CartItemEntity> getCartItemsSync();
}
