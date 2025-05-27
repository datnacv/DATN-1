package com.example.AsmGD1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "danh_muc")
@Data
public class DanhMuc {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "ten_danh_muc")
    private String tenDanhMuc;
}