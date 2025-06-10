package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "san_pham")
@Data
public class SanPham {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_danh_muc", nullable = false)
    private DanhMuc danhMuc;

    @Column(name = "ma_san_pham", nullable = false, length = 50, unique = true)
    private String maSanPham;

    @Column(name = "ten_san_pham", nullable = false, length = 100)
    private String tenSanPham;

    @Column(name = "mo_ta", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    @Column(name = "url_hinh_anh", columnDefinition = "NVARCHAR(MAX)")
    private String urlHinhAnh;

    @Column(name = "thoi_gian_tao", nullable = false)
    private LocalDateTime thoiGianTao;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai;
}