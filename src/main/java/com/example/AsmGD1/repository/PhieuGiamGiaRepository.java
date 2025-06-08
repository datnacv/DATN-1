package com.example.AsmGD1.repository;

import com.example.AsmGD1.entity.PhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, UUID>, JpaSpecificationExecutor<PhieuGiamGia> {
    boolean existsByMaIgnoreCase(String ma);
}
