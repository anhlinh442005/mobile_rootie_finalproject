package com.veganbeauty.app.features.shop.product

import android.content.Context
import android.widget.Toast
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.CartItemEntity
import com.veganbeauty.app.data.local.entities.ProductEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object CartHelper {
    fun addToCart(context: Context, coroutineScope: CoroutineScope, product: ProductEntity, quantity: Int) {
        coroutineScope.launch {
            val db = RootieDatabase.getDatabase(context)
            val existingItem = db.cartDao().getCartItemById(product.id)
            if (existingItem != null) {
                db.cartDao().updateCartItem(existingItem.copy(quantity = existingItem.quantity + quantity))
            } else {
                db.cartDao().insertCartItem(
                    CartItemEntity(
                        id = product.id,
                        name = product.name,
                        image = product.mainImage,
                        price = product.price,
                        quantity = quantity,
                        isSelected = true
                    )
                )
            }
            Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
        }
    }
}
