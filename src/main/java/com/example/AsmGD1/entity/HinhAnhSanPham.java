package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "hinh_anh_san_pham")
@Data
public class HinhAnhSanPham {
    @Id
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_chi_tiet_san_pham")
    private ChiTietSanPham chiTietSanPham;

    @Column(name = "url_hinh_anh")
    private String urlHinhAnh;
}
