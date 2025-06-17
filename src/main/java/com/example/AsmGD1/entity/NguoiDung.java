package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nguoi_dung")
@Data
public class NguoiDung {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "ten_dang_nhap", nullable = false, length = 50, unique = true)
    private String tenDangNhap;

    @Column(name = "mat_khau", nullable = false, length = 100)
    private String matKhau;

    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "so_dien_thoai", nullable = false, length = 20, unique = true)
    private String soDienThoai;

    @Column(name = "dia_chi", columnDefinition = "NVARCHAR(MAX)")
    private String diaChi;

    @Column(name = "vai_tro", length = 50)
    private String vaiTro;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "gioi_tinh")
    private Boolean gioiTinh;

    @Column(name = "tinh_thanh_pho", length = 100)
    private String tinhThanhPho;

    @Column(name = "quan_huyen", length = 100)
    private String quanHuyen;

    @Column(name = "phuong_xa", length = 100)
    private String phuongXa;

    @Column(name = "chi_tiet_dia_chi", columnDefinition = "NVARCHAR(MAX)")
    private String chiTietDiaChi;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime thoiGianTao;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}