package com.example.AsmGD1.repository;

import com.example.AsmGD1.entity.PhieuGiamGiaCuaNguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PhieuGiamGiaCuaNguoiDungRepository extends JpaRepository<PhieuGiamGiaCuaNguoiDung, UUID> {
    List<PhieuGiamGiaCuaNguoiDung> findByPhieuGiamGia_Id(UUID phieuId);

}