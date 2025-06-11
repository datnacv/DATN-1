package com.example.AsmGD1.dto.BanHang;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class GioHangDTO {
    private List<GioHangItemDTO> danhSachSanPham;
    private String tongTienHang;
    private String giamGia;
    private String tong;
    private boolean daXoaPhieuGiamGia;
    private String giamGiaDaXoa;
    private boolean daApDungPhieuGiamGia;
    private BigDecimal phiVanChuyen;
    private UUID khachHangDaChon;
    private UUID idPhieuGiamGia;
    private String phuongThucBanHang;
    private String phuongThucThanhToan;
}