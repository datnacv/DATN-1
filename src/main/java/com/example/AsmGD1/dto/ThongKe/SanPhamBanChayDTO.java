package com.example.AsmGD1.dto.ThongKe;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SanPhamBanChayDTO {
    private UUID idChiTietSanPham;
    private String tenSanPham;
    private String mauSac;
    private String kichCo;
    private BigDecimal gia;
    private Long soLuongDaBan;
    private String imageUrl;

    public SanPhamBanChayDTO(UUID idChiTietSanPham, String tenSanPham, String mauSac,
                             String kichCo, BigDecimal gia, Long soLuongDaBan, String imageUrl) {
        this.idChiTietSanPham = idChiTietSanPham;
        this.tenSanPham = tenSanPham;
        this.mauSac = mauSac;
        this.kichCo = kichCo;
        this.gia = gia;
        this.soLuongDaBan = soLuongDaBan;
        this.imageUrl = imageUrl;
    }
}
