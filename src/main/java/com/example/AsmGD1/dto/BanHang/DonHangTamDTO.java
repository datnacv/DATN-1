package com.example.AsmGD1.dto.BanHang;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class DonHangTamDTO {
    private UUID id;
    private String tenKhachHang;
    private BigDecimal tong;
    private String thoiGianTao;
    private List<GioHangItemDTO> danhSachSanPham;
}
