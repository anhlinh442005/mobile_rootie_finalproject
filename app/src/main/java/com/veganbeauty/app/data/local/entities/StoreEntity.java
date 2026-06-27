package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "stores")
public class StoreEntity implements Serializable {
    @PrimaryKey
    @NonNull
    private String id;
    @NonNull private String maCuaHang;
    @NonNull private String tenCuaHang;
    @NonNull private String loaiHinh;
    @NonNull private String soNha;
    @NonNull private String duong;
    @NonNull private String phuongXa;
    @NonNull private String quanHuyen;
    @NonNull private String tinhThanh;
    @NonNull private String diaChiDayDu;
    private double lat;
    private double lng;
    @NonNull private String soDienThoai;
    @NonNull private String email;
    @NonNull private String moCua;
    @NonNull private String dongCua;
    @NonNull private String trangThai;
    private boolean isActive;
    @NonNull private String tienNghi;
    @NonNull private String imageUrl;
    private double distance;

    public StoreEntity(@NonNull String id, @NonNull String maCuaHang, @NonNull String tenCuaHang, @NonNull String loaiHinh, @NonNull String soNha, @NonNull String duong, @NonNull String phuongXa, @NonNull String quanHuyen, @NonNull String tinhThanh, @NonNull String diaChiDayDu, double lat, double lng, @NonNull String soDienThoai, @NonNull String email, @NonNull String moCua, @NonNull String dongCua, @NonNull String trangThai, boolean isActive, @NonNull String tienNghi, @NonNull String imageUrl, double distance) {
        this.id = id; this.maCuaHang = maCuaHang; this.tenCuaHang = tenCuaHang; this.loaiHinh = loaiHinh; this.soNha = soNha; this.duong = duong; this.phuongXa = phuongXa; this.quanHuyen = quanHuyen; this.tinhThanh = tinhThanh; this.diaChiDayDu = diaChiDayDu; this.lat = lat; this.lng = lng; this.soDienThoai = soDienThoai; this.email = email; this.moCua = moCua; this.dongCua = dongCua; this.trangThai = trangThai; this.isActive = isActive; this.tienNghi = tienNghi; this.imageUrl = imageUrl; this.distance = distance;
    }

    public String getStoreName() { return tenCuaHang; }
    public String getAddress() { return diaChiDayDu; }
    public String getProvince() { return tinhThanh; }
    public String getOpenHours() { return moCua + " - " + dongCua; }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    @NonNull public String getMaCuaHang() { return maCuaHang; }
    public void setMaCuaHang(@NonNull String maCuaHang) { this.maCuaHang = maCuaHang; }
    @NonNull public String getTenCuaHang() { return tenCuaHang; }
    public void setTenCuaHang(@NonNull String tenCuaHang) { this.tenCuaHang = tenCuaHang; }
    @NonNull public String getLoaiHinh() { return loaiHinh; }
    public void setLoaiHinh(@NonNull String loaiHinh) { this.loaiHinh = loaiHinh; }
    @NonNull public String getSoNha() { return soNha; }
    public void setSoNha(@NonNull String soNha) { this.soNha = soNha; }
    @NonNull public String getDuong() { return duong; }
    public void setDuong(@NonNull String duong) { this.duong = duong; }
    @NonNull public String getPhuongXa() { return phuongXa; }
    public void setPhuongXa(@NonNull String phuongXa) { this.phuongXa = phuongXa; }
    @NonNull public String getQuanHuyen() { return quanHuyen; }
    public void setQuanHuyen(@NonNull String quanHuyen) { this.quanHuyen = quanHuyen; }
    @NonNull public String getTinhThanh() { return tinhThanh; }
    public void setTinhThanh(@NonNull String tinhThanh) { this.tinhThanh = tinhThanh; }
    @NonNull public String getDiaChiDayDu() { return diaChiDayDu; }
    public void setDiaChiDayDu(@NonNull String diaChiDayDu) { this.diaChiDayDu = diaChiDayDu; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
    @NonNull public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(@NonNull String soDienThoai) { this.soDienThoai = soDienThoai; }
    @NonNull public String getEmail() { return email; }
    public void setEmail(@NonNull String email) { this.email = email; }
    @NonNull public String getMoCua() { return moCua; }
    public void setMoCua(@NonNull String moCua) { this.moCua = moCua; }
    @NonNull public String getDongCua() { return dongCua; }
    public void setDongCua(@NonNull String dongCua) { this.dongCua = dongCua; }
    @NonNull public String getTrangThai() { return trangThai; }
    public void setTrangThai(@NonNull String trangThai) { this.trangThai = trangThai; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    @NonNull public String getTienNghi() { return tienNghi; }
    public void setTienNghi(@NonNull String tienNghi) { this.tienNghi = tienNghi; }
    @NonNull public String getImageUrl() { return imageUrl; }
    public void setImageUrl(@NonNull String imageUrl) { this.imageUrl = imageUrl; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
}
