package com.example.AsmGD1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi_dung")
@Data
public class NguoiDung {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "ten_dang_nhap")
    private String tenDangNhap;

    @Column(name = "mat_khau")
    private String matKhau;

    @Column(name = "ho_ten")
    private String hoTen;

    @Column(name = "email")
    private String email;

    @Column(name = "so_dien_thoai")
    private String soDienThoai;

    @Column(name = "dia_chi")
    private String diaChi;

    @Column(name = "vai_tro")
    private String vaiTro;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "gioi_tinh")
    private Boolean gioiTinh;

    @Column(name = "tinh_thanh_pho")
    private String tinhThanhPho;

    @Column(name = "quan_huyen")
    private String quanHuyen;

    @Column(name = "phuong_xa")
    private String phuongXa;

    @Column(name = "chi_tiet_dia_chi")
    private String chiTietDiaChi;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime thoiGianTao;

    @Column(name = "id_qr_gioi_thieu")
    private String idQrGioiThieu;

    @Column(name = "thoi_gian_bat_han_otp")
    private LocalDateTime thoiGianBatHanOtp;

    @Column(name = "trang_thai")
    private Boolean trangThai;
}
