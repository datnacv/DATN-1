package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "phieu_giam_gia")
@Data
public class PhieuGiamGia {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;

    @Column(name = "loai", nullable = false, length = 50)
    private String loai;

    @Column(name = "gia_tri_giam", nullable = false, precision = 10, scale = 2)
    private BigDecimal giaTriGiam;

    @Column(name = "gia_tri_giam_toi_thieu", precision = 10, scale = 2)
    private BigDecimal giaTriGiamToiThieu;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "gioi_han_su_dung")
    private Integer gioiHanSuDung;

    @Column(name = "cong_khai")
    private Boolean congKhai;

    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDate ngayKetThuc;

    @Column(name = "thoi_gian_tao", nullable = false)
    private LocalDateTime thoiGianTao;

    @Column(name = "kieu_phieu", length = 20)
    private String kieuPhieu;
}