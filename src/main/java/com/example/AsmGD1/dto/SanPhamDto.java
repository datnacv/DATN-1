package com.example.AsmGD1.dto;

import lombok.Data;

import java.util.UUID;

import java.time.LocalDateTime; // THÊM nếu chưa có

@Data
public class SanPhamDto {
    private UUID id;
    private String tenSanPham;
    private String maSanPham;
    private String moTa;
    private String urlHinhAnh;
    private Boolean trangThai;
    private UUID danhMucId;
    private String tenDanhMuc;
    private LocalDateTime thoiGianTao; // 👈 THÊM dòng này
}


