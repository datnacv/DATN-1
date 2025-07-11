package com.example.AsmGD1.repository.BanHang;

import com.example.AsmGD1.entity.DonHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface DonHangRepository extends JpaRepository<DonHang, UUID> {
    Optional<DonHang> findByMaDonHang(String maDonHang);

}
