package com.example.AsmGD1.entity;
import lombok.Data;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chien_dich_giam_gia")
@Data
public class ChienDichGiamGia {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "ten")
    private String ten;

    @Column(name = "hinh_thuc_giam")
    private String hinhThucGiam;

    @Column(name = "so_luong")
    private Integer soLuong;

    @Column(name = "phan_tram_giam")
    private BigDecimal phanTramGiam;

    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDate ngayKetThuc;

    @Column(name = "thoi_gian_tao")
    private LocalDateTime thoiGianTao;
}
