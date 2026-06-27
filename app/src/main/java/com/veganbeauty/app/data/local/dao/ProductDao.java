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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertProducts(List<ProductEntity> products);

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    ProductEntity getProductById(String productId);

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    ProductEntity getProductByBarcode(String barcode);
}
