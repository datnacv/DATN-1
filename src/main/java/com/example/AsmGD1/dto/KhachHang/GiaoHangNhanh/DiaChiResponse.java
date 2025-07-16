package com.example.AsmGD1.dto.KhachHang.GiaoHangNhanh;

import lombok.Data;

@Data
public class DiaChiResponse {
    private String tinhThanhPho;
    private String quanHuyen;
    private String phuongXa;
    private String chiTietDiaChi;

    public String getFullAddress() {
        return chiTietDiaChi + ", " + phuongXa + ", " + quanHuyen + ", " + tinhThanhPho;
    }
}