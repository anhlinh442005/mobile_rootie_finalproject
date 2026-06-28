package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veganbeauty.app.data.local.entities.ProductEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface ProductDao {
    @Query("SELECT COUNT(*) FROM products")
    int getProductCount();

    @Query("SELECT * FROM products")
    Flow<List<ProductEntity>> getAllProducts();

    @Query("SELECT * FROM products")
    List<ProductEntity> getAllProductsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertProducts(List<ProductEntity> products);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    ProductEntity getProductById(String productId);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    ProductEntity getProductByBarcode(String barcode);
}
