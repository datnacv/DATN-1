package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "chi_tiet_don_hang")
@Data
public class ChiTietDonHang {
    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_don_hang")
    private DonHang donHang;

    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_san_pham")
    private ChiTietSanPham chiTietSanPham;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "gia")
    private BigDecimal gia;

    @Column(name = "ten_san_pham")
    private String tenSanPham;

    @Column(name = "thanh_tien")
    private BigDecimal thanhTien;

    @Column(name = "ghi_chu")
    private String ghiChu;

    @Column(name = "trang_thai_hoan_tra")
    private Boolean trangThaiHoanTra;
}
