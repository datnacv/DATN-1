package com.example.AsmGD1.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class SanPhamTonKhoThapDTO {
    private UUID idChiTietSanPham;
    private String tenSanPham;
    private String mauSac;
    private String kichCo;
    private BigDecimal gia;
    private Integer soLuongTonKho;
    private String imageUrl;

    public SanPhamTonKhoThapDTO(UUID idChiTietSanPham, String tenSanPham, String mauSac,
                                String kichCo, BigDecimal gia, Integer soLuongTonKho, String imageUrl) {
        this.idChiTietSanPham = idChiTietSanPham;
        this.tenSanPham = tenSanPham;
        this.mauSac = mauSac;
        this.kichCo = kichCo;
        this.gia = gia;
        this.soLuongTonKho = soLuongTonKho;
        this.imageUrl = imageUrl;
    }
}
