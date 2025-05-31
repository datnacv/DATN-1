package com.example.AsmGD1.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "don_hang")
@Data
public class DonHang {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_nguoi_dung")
    private NguoiDung nguoiDung;

    @ManyToOne
    @JoinColumn(name = "id_phuong_thuc_ban_hang")
    private PhuongThucBanHang phuongThucBanHang;

    @Column(name = "ma_don_hang")
    private String maDonHang;

    @Column(name = "trang_thai_thanh_toan")
    private Boolean trangThaiThanhToan;

    @Column(name = "phi_van_chuyen")
    private BigDecimal phiVanChuyen;

    @Column(name = "phuong_thuc_thanh_toan")
    private String phuongThucThanhToan;

    @Column(name = "so_lien_lac_dua")
    private String soLienLacDua;

    @Column(name = "ghi_chu_thanh_toan")
    private String ghiChuThanhToan;

    @Column(name = "tien_giam")
    private BigDecimal tienGiam;

    @Column(name = "tong_tien")
    private BigDecimal tongTien;
}