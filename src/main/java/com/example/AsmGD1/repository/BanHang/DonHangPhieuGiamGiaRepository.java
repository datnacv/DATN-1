package com.example.AsmGD1.repository.BanHang;

import com.example.AsmGD1.entity.DonHangPhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DonHangPhieuGiamGiaRepository extends JpaRepository<DonHangPhieuGiamGia, UUID> {
}
