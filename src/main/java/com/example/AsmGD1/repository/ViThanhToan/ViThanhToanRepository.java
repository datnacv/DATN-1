package com.example.AsmGD1.repository.ViThanhToan;

import com.example.AsmGD1.entity.ViThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ViThanhToanRepository extends JpaRepository<ViThanhToan, UUID> {
    Optional<ViThanhToan> findByIdNguoiDung(UUID idNguoiDung);
}