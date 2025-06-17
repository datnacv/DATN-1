package com.example.AsmGD1.repository.BanHang;

import com.example.AsmGD1.entity.ChiTietDonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, UUID> {
    List<ChiTietDonHang> findByDonHangId(UUID donHangId);
}