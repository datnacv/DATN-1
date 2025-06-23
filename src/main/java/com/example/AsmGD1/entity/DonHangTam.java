package com.example.AsmGD1.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "don_hang_tam")
@Data
public class DonHangTam {
    @Id
    @Column(name = "id", columnDefinition = "UNIQUEIDENTIFIER", updatable = false)
    private UUID id;

    @Column(name = "tabId")
    private String tabId; // Thêm trường này

    @Column(name = "id_khach_hang")
    private UUID khachHang;

    @Column(name = "ma_don_hang_tam")
    private String maDonHangTam;

    @Column(name = "so_dien_thoai_khach_hang")
    private String soDienThoaiKhachHang;

    @Column(name = "tong_tien")
    private BigDecimal tong;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime thoiGianTao;

    @Column(name = "danh_sach_item", columnDefinition = "NVARCHAR(MAX)")
    private String danhSachSanPham;

    @Column(name = "phuong_thuc_thanh_toan")
    private String phuongThucThanhToan;

    @Column(name = "phuong_thuc_ban_hang")
    private String phuongThucBanHang;

    @Column(name = "phi_van_chuyen")
    private BigDecimal phiVanChuyen;

    @Column(name = "id_phieu_giam_gia")
    private UUID phieuGiamGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang", referencedColumnName = "id", insertable = false, updatable = false)
    private NguoiDung nguoiDung;
}