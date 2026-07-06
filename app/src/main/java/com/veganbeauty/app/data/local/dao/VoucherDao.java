package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veganbeauty.app.data.local.entities.VoucherEntity;

import java.util.List;

@Dao
public interface VoucherDao {
    @Query("SELECT * FROM vouchers")
    List<VoucherEntity> getAllVouchers();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<VoucherEntity> vouchers);

    @Query("DELETE FROM vouchers")
    void clearAll();
}
