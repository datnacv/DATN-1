package com.example.AsmGD1.dto.BanHang;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class GioHangDTO {
    private List<GioHangItemDTO> danhSachSanPham;
    private BigDecimal tongTienHangValue; // Thêm giá trị số
    private String tongTienHang; // Giữ định dạng
    private BigDecimal giamGiaValue; // Thêm giá trị số
    private String giamGia; // Giữ định dạng
    private BigDecimal tongValue; // Thêm giá trị số
    private String tong; // Giữ định dạng
    private boolean daXoaPhieuGiamGia;
    private String giamGiaDaXoa;
    private boolean daApDungPhieuGiamGia;
    private BigDecimal phiVanChuyen;
    private UUID khachHangDaChon;
    private UUID idPhieuGiamGia;
    private String phuongThucBanHang;
    private String phuongThucThanhToan;
    private String soDienThoaiKhachHang;
    private String tabId;
    private BigDecimal soTienKhachDua;
    private BigDecimal changeAmount;
}