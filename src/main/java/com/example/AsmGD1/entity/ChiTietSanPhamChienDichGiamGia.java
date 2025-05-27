package com.example.AsmGD1.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "chi_tiet_san_pham_chien_dich_giam_gia")
@Data
public class ChiTietSanPhamChienDichGiamGia {
    @Id
    @ManyToOne
    @JoinColumn(name = "id_chien_dich")
    private ChienDichGiamGia chienDichGiamGia;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_san_pham")
    private ChiTietSanPham chiTietSanPham;
}