package com.example.AsmGD1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "phuong_thuc_ban_hang")
@Data
public class PhuongThucBanHang {
    @Id
    @Column(name = "ma")
    private Integer ma;

    @Column(name = "ten")
    private String ten;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "phi_van_chuyen")
    private BigDecimal phiVanChuyen;

    @Column(name = "trang_thai")
    private Boolean trangThai;
}