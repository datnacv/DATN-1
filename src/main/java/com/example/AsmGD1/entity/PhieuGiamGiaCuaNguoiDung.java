package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "phieu_giam_gia_cua_nguoi_dung")
@Data
public class PhieuGiamGiaCuaNguoiDung {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_nguoi_dung", nullable = false)
    private NguoiDung nguoiDung;

    @ManyToOne
    @JoinColumn(name = "id_phieu_giam_gia", nullable = false)
    private PhieuGiamGia phieuGiamGia;

    @Column(name = "da_gui_mail")
    private Boolean daGuiMail;
}