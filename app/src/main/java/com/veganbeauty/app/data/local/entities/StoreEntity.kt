package com.veganbeauty.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val maCuaHang: String,
    val tenCuaHang: String,
    val loaiHinh: String,
    val soNha: String = "",
    val duong: String = "",
    val phuongXa: String = "",
    val quanHuyen: String = "",
    val tinhThanh: String = "",
    val diaChiDayDu: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val soDienThoai: String = "",
    val email: String = "",
    val moCua: String = "07:00",
    val dongCua: String = "21:00",
    val trangThai: String = "Đang hoạt động",
    val isActive: Boolean = true,
    val tienNghi: String = "",
    val imageUrl: String = "",
    val distance: Double = 0.0
) : Serializable {
    val storeName: String get() = tenCuaHang
    val address: String get() = diaChiDayDu
    val province: String get() = tinhThanh
    val openHours: String get() = "$moCua - $dongCua"
}
