package com.example.AsmGD1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "phuong_thuc_thanh_toan")
@Data
public class PhuongThucThanhToan {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "ten_phuong_thuc", nullable = false, length = 100)
    private String tenPhuongThuc;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;
}