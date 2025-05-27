package com.example.AsmGD1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "xuat_xu")
@Data
public class XuatXu {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "ten_xuat_xu")
    private String tenXuatXu;
}