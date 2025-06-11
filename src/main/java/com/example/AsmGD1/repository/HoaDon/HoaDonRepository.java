package com.example.AsmGD1.repository.HoaDon;

import com.example.AsmGD1.entity.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, UUID> {
    Optional<HoaDon> findByDonHangId(UUID donHangId);
}