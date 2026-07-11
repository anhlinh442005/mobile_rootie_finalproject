package com.veganbeauty.app.features.shop.product;

import android.content.Context;
import android.widget.Toast;

import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;

public class CartHelper {
    public static boolean addToCart(Context context, Object coroutineScope, ProductEntity product, int quantity) {
        return addToCart(context, coroutineScope, product, quantity, true);
    }

    public static boolean addToCart(Context context, Object coroutineScope, ProductEntity product, int quantity, boolean showToast) {
        if (!com.veganbeauty.app.data.local.ProfileSession.isLoggedIn(context)) {
            com.veganbeauty.app.features.home.BottomNavHelper.showLoginRequiredDialog(context);
            return false;
        }

        if (product.getStock() <= 0) {
            Toast.makeText(context, "Sản phẩm hiện đã hết hàng", Toast.LENGTH_SHORT).show();
            return false;
        }

        new Thread(() -> {
            try {
                RootieDatabase db = RootieDatabase.getDatabase(context);
                CartItemEntity existingItem = db.cartDao().getCartItemById(product.getId());
                if (existingItem != null) {
                    CartItemEntity updated = new CartItemEntity(
                            existingItem.getId(),
                            existingItem.getName(),
                            existingItem.getImage(),
                            existingItem.getPrice(),
                            existingItem.getQuantity() + quantity,
                            existingItem.isSelected()
                    );
                    db.cartDao().updateCartItem(updated);
                } else {
                    db.cartDao().insertCartItem(new CartItemEntity(
                            product.getId(),
                            product.getName(),
                            product.getMainImage(),
                            product.getPrice(),
                            quantity,
                            true
                    ));
                }
                if (showToast) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        return true;
    }

    public static void clearCart(Context context) {
        new Thread(() -> {
            try {
                RootieDatabase.getDatabase(context).cartDao().clearCart();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
