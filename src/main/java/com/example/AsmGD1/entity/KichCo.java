package com.example.AsmGD1.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "kich_co")
@Data
public class KichCo {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "ten")
    private String ten;
}