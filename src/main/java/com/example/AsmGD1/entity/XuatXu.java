package com.example.AsmGD1.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "xuat_xu")
@Data
public class XuatXu {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "ten_xuat_xu")
    private String tenXuatXu;
}