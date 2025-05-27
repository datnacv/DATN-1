package com.example.AsmGD1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "phong_cach")
@Data
public class PhongCach {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "ten_phong_cach")
    private String tenPhongCach;
}