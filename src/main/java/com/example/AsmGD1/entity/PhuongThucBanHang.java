package com.example.AsmGD1.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "phuong_thuc_ban_hang")
@Data
public class PhuongThucBanHang {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ma")
    private UUID ma;

    @Column(name = "ten")
    private String ten;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "phi_van_chuyen")
    private BigDecimal phiVanChuyen;

    @Column(name = "trang_thai")
    private Boolean trangThai;
}