package com.example.AsmGD1.dto.BanHang;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class DonHangDTO {
    private UUID khachHangDaChon;
    private BigDecimal phiVanChuyen;
    private UUID phuongThucThanhToan;
    private String phuongThucBanHang;
    private BigDecimal soTienKhachDua;
    private List<GioHangItemDTO> danhSachSanPham;
    private UUID idPhieuGiamGia;
    private String tong;
    private String soDienThoaiKhachHang;
    private String tabId;
    private String diaChiGiaoHang;
}
