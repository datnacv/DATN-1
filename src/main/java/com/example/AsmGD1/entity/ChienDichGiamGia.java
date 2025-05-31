package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chien_dich_giam_gia")
@Data
public class ChienDichGiamGia {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;

    @Column(name = "hinh_thuc_giam", nullable = false, length = 50)
    private String hinhThucGiam;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "phan_tram_giam", precision = 5, scale = 2)
    private BigDecimal phanTramGiam;

    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDate ngayKetThuc;

    @Column(name = "thoi_gian_tao", nullable = false)
    private LocalDateTime thoiGianTao;
}
