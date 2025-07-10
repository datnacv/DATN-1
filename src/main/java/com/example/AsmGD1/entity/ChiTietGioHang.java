package com.example.AsmGD1.entity;

import com.example.AsmGD1.entity.ChiTietSanPham;
import com.example.AsmGD1.entity.GioHang;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gio_hang_chi_tiet")
@Data
public class ChiTietGioHang {
    @Id
//    @GeneratedValue bỏ trường này
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_gio_hang", nullable = false)
    @JsonIgnore
    private GioHang gioHang;

    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_san_pham", nullable = false)
    private ChiTietSanPham chiTietSanPham;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    @Column(name = "gia", nullable = false, precision = 10, scale = 2)
    private BigDecimal gia;

    @Column(name = "tien_giam", precision = 10, scale = 2)
    private BigDecimal tienGiam;

    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    @Column(name = "thoi_gian_them", nullable = false)
    private LocalDateTime thoiGianThem;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai;
}