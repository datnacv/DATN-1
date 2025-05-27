package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "san_pham")
@Data
public class SanPham {
    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_chat_lieu")
    private ChatLieu chatLieu;

    @ManyToOne
    @JoinColumn(name = "id_xuat_xu")
    private XuatXu xuatXu;

    @ManyToOne
    @JoinColumn(name = "id_phong_cach")
    private PhongCach phongCach;

    @ManyToOne
    @JoinColumn(name = "id_danh_muc")
    private DanhMuc danhMuc;

    @Column(name = "ten_san_pham")
    private String tenSanPham;

    @Column(name = "ma_san_pham")
    private String maSanPham;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "id_nha_phan_phoi")
    private Integer idNhaPhanPhoi;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime thoiGianTao;

    @Column(name = "trang_thai")
    private Boolean trangThai;
}
