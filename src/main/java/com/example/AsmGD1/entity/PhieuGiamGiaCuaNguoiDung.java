package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "phieu_giam_gia_cua_nguoi_dung")
@Data
public class PhieuGiamGiaCuaNguoiDung {
    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_nguoi_dung")
    private NguoiDung nguoiDung;

    @ManyToOne
    @JoinColumn(name = "id_phieu_giam_gia")
    private PhieuGiamGia phieuGiamGia;
}
