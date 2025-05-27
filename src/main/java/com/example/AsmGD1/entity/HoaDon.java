package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "hoa_don")
@Data
public class HoaDon {
    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_nguoi_dung")
    private NguoiDung nguoiDung;

    @ManyToOne
    @JoinColumn(name = "id_don_hang")
    private DonHang donHang;

    @ManyToOne
    @JoinColumn(name = "id_ma_giam_gia")
    private ChienDichGiamGia chienDichGiamGia;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_hoan_tien")
    private LocalDateTime ngayHoanTien;

    @Column(name = "tong_tien")
    private BigDecimal tongTien;

    @Column(name = "tien_giam")
    private BigDecimal tienGiam;

    @Column(name = "hinh_thuc_thanh_toan")
    private String hinhThucThanhToan;

    @Column(name = "trang_thai")
    private Boolean trangThai;

    @Column(name = "ghi_chu")
    private String ghiChu;
}
